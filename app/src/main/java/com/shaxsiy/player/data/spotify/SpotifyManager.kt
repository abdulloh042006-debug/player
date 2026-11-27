package com.shaxsiy.player.data.spotify

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

data class SpotifyPlaylistItem(
    val id: String,
    val name: String,
    val uri: String,
    val trackCount: Int
)

data class SpotifyTrackItem(
    val id: String,
    val name: String,
    val artist: String,
    val uri: String,
    val durationMs: Long
)

data class SpotifyUiState(
    val isConfigured: Boolean = SpotifyConfig.isConfigured(),
    val isSpotifyAppInstalled: Boolean = false,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val currentTrackTitle: String = "",
    val currentTrackArtist: String = "",
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val playlists: List<SpotifyPlaylistItem> = emptyList(),
    val likedSongs: List<SpotifyTrackItem> = emptyList(),
    val searchResults: List<SpotifyTrackItem> = emptyList(),
    val statusMessage: String? = null
)

class SpotifyManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(
        SpotifyUiState(
            isConfigured = SpotifyConfig.isConfigured(),
            isSpotifyAppInstalled = checkSpotifyInstalled(),
            isConnected = !getAccessToken().isNullOrBlank()
        )
    )
    val state: StateFlow<SpotifyUiState> = _state.asStateFlow()

    private fun checkSpotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.spotify.music", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAccessToken(): String? = prefs.getString("access_token", null)
    private fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun createAuthIntent(): Intent? {
        if (!SpotifyConfig.isConfigured()) {
            _state.update {
                it.copy(
                    statusMessage = "Spotify Client ID kiritilmagan. SpotifyConfig.kt faylini to‘ldiring."
                )
            }
            return null
        }

        val verifier = generateCodeVerifier()
        prefs.edit().putString("pkce_verifier", verifier).apply()
        val challenge = generateCodeChallenge(verifier)

        val uri = Uri.parse(SpotifyConfig.AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", SpotifyConfig.CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SpotifyConfig.REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", SpotifyConfig.SCOPES)
            .build()

        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    suspend fun handleAuthCallback(callbackUri: Uri) {
        val error = callbackUri.getQueryParameter("error")
        if (error != null) {
            _state.update { it.copy(statusMessage = "Spotify avtorizatsiyasi bekor qilindi: $error") }
            return
        }

        val code = callbackUri.getQueryParameter("code") ?: return
        val verifier = prefs.getString("pkce_verifier", null) ?: return

        _state.update { it.copy(isLoading = true, statusMessage = null) }
        val success = exchangeCodeForToken(code, verifier)
        if (success) {
            _state.update { it.copy(isConnected = true, statusMessage = "Spotify muvaffaqiyatli ulandi") }
            refreshAllData()
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Spotify token olishda xatolik. Internet yoki Client ID ni tekshiring."
                )
            }
        }
    }

    fun disconnect() {
        prefs.edit().clear().apply()
        _state.update {
            SpotifyUiState(
                isConfigured = SpotifyConfig.isConfigured(),
                isSpotifyAppInstalled = checkSpotifyInstalled(),
                isConnected = false,
                statusMessage = "Spotify hisobidan chiqildi"
            )
        }
    }

    suspend fun refreshAllData() {
        if (getAccessToken().isNullOrBlank()) return
        _state.update { it.copy(isLoading = true, statusMessage = null) }
        try {
            val playlists = fetchPlaylists()
            val liked = fetchLikedSongs()
            fetchPlaybackState()
            _state.update {
                it.copy(
                    isLoading = false,
                    playlists = playlists,
                    likedSongs = liked
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Internet yoki Spotify bilan aloqa yo‘q. Oflayn pleyer ishlashda davom etmoqda."
                )
            }
        }
    }

    suspend fun searchTracks(query: String) {
        if (query.isBlank() || getAccessToken().isNullOrBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        try {
            val encoded = withContext(Dispatchers.IO) { URLEncoder.encode(query, "UTF-8") }
            val body = apiRequest("GET", "/search?q=$encoded&type=track&limit=20") ?: return
            val json = gson.fromJson(body, JsonObject::class.java)
            val items = json.getAsJsonObject("tracks")?.getAsJsonArray("items") ?: return
            val tracks = items.mapNotNull { el ->
                val obj = el.asJsonObject
                val artists = obj.getAsJsonArray("artists")
                    ?.joinToString(", ") { it.asJsonObject.get("name").asString }
                    ?: "Noma'lum"
                SpotifyTrackItem(
                    id = obj.get("id")?.asString ?: return@mapNotNull null,
                    name = obj.get("name")?.asString ?: "",
                    artist = artists,
                    uri = obj.get("uri")?.asString ?: return@mapNotNull null,
                    durationMs = obj.get("duration_ms")?.asLong ?: 0L
                )
            }
            _state.update { it.copy(searchResults = tracks, statusMessage = null) }
        } catch (_: Exception) {
            _state.update { it.copy(statusMessage = "Spotify qidiruvida xatolik yuz berdi.") }
        }
    }

    suspend fun playUri(uri: String, isContext: Boolean = false) {
        val payload = if (isContext) {
            """{"context_uri":"$uri"}"""
        } else {
            """{"uris":["$uri"]}"""
        }
        val ok = sendPlaybackCommand("PUT", "/me/player/play", payload)
        if (ok) fetchPlaybackState()
    }

    suspend fun togglePlayPause() {
        val currentlyPlaying = _state.value.isPlaying
        val endpoint = if (currentlyPlaying) "/me/player/pause" else "/me/player/play"
        val ok = sendPlaybackCommand("PUT", endpoint, null)
        if (ok) {
            _state.update { it.copy(isPlaying = !currentlyPlaying) }
            fetchPlaybackState()
        }
    }

    suspend fun nextTrack() {
        if (sendPlaybackCommand("POST", "/me/player/next", null)) {
            fetchPlaybackState()
        }
    }

    suspend fun previousTrack() {
        if (sendPlaybackCommand("POST", "/me/player/previous", null)) {
            fetchPlaybackState()
        }
    }

    suspend fun seekBy(deltaMs: Long) {
        val target = (_state.value.progressMs + deltaMs).coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        if (sendPlaybackCommand("PUT", "/me/player/seek?position_ms=$target", null)) {
            _state.update { it.copy(progressMs = target) }
        }
    }

    suspend fun fetchPlaybackState() {
        try {
            val body = apiRequest("GET", "/me/player")
            if (body.isNullOrBlank()) {
                _state.update { it.copy(isPlaying = false) }
                return
            }
            val json = gson.fromJson(body, JsonObject::class.java)
            val isPlaying = json.get("is_playing")?.asBoolean ?: false
            val progressMs = json.get("progress_ms")?.asLong ?: 0L
            val item = json.getAsJsonObject("item")
            val title = item?.get("name")?.asString ?: ""
            val durationMs = item?.get("duration_ms")?.asLong ?: 0L
            val artist = item?.getAsJsonArray("artists")
                ?.joinToString(", ") { it.asJsonObject.get("name").asString } ?: ""

            _state.update {
                it.copy(
                    isPlaying = isPlaying,
                    progressMs = progressMs,
                    durationMs = durationMs,
                    currentTrackTitle = title,
                    currentTrackArtist = artist
                )
            }
        } catch (_: Exception) {
            // Silent fail to keep UI responsive
        }
    }

    private suspend fun fetchPlaylists(): List<SpotifyPlaylistItem> {
        val body = apiRequest("GET", "/me/playlists?limit=30") ?: return emptyList()
        val json = gson.fromJson(body, JsonObject::class.java)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.mapNotNull { el ->
            val obj = el.asJsonObject
            SpotifyPlaylistItem(
                id = obj.get("id")?.asString ?: return@mapNotNull null,
                name = obj.get("name")?.asString ?: "Nomsiz",
                uri = obj.get("uri")?.asString ?: return@mapNotNull null,
                trackCount = obj.getAsJsonObject("tracks")?.get("total")?.asInt ?: 0
            )
        }
    }

    private suspend fun fetchLikedSongs(): List<SpotifyTrackItem> {
        val body = apiRequest("GET", "/me/tracks?limit=30") ?: return emptyList()
        val json = gson.fromJson(body, JsonObject::class.java)
        val items = json.getAsJsonArray("items") ?: return emptyList()
        return items.mapNotNull { el ->
            val trackObj = el.asJsonObject.getAsJsonObject("track") ?: return@mapNotNull null
            val artists = trackObj.getAsJsonArray("artists")
                ?.joinToString(", ") { it.asJsonObject.get("name").asString }
                ?: "Noma'lum"
            SpotifyTrackItem(
                id = trackObj.get("id")?.asString ?: return@mapNotNull null,
                name = trackObj.get("name")?.asString ?: "",
                artist = artists,
                uri = trackObj.get("uri")?.asString ?: return@mapNotNull null,
                durationMs = trackObj.get("duration_ms")?.asLong ?: 0L
            )
        }
    }

    private suspend fun sendPlaybackCommand(method: String, path: String, jsonBody: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken() ?: return@withContext false
                val conn = (URL("${SpotifyConfig.API_BASE}$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 8000
                    readTimeout = 8000
                    if (jsonBody != null || method == "PUT" || method == "POST") {
                        doOutput = true
                        OutputStreamWriter(outputStream).use { it.write(jsonBody ?: "") }
                    }
                }
                val code = conn.responseCode
                if (code == 401 && refreshAccessToken()) {
                    return@withContext sendPlaybackCommand(method, path, jsonBody)
                }
                if (code == 404) {
                    _state.update {
                        it.copy(statusMessage = "Faol Spotify qurilmasi topilmadi. Avval telefoningizda Spotify ilovasini oching.")
                    }
                    return@withContext false
                }
                if (code == 403) {
                    _state.update {
                        it.copy(statusMessage = "Spotify ijrosini masofadan boshqarish uchun Spotify Premium talab qilinadi.")
                    }
                    return@withContext false
                }
                code in 200..299
            } catch (_: Exception) {
                _state.update { it.copy(statusMessage = "Internet aloqasi yo‘q yoki Spotify javob bermadi.") }
                false
            }
        }
    }

    private suspend fun apiRequest(method: String, path: String): String? = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext null
        val conn = (URL("${SpotifyConfig.API_BASE}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 8000
            readTimeout = 8000
        }
        val code = conn.responseCode
        if (code == 401 && refreshAccessToken()) {
            return@withContext apiRequest(method, path)
        }
        if (code == 204) return@withContext ""
        if (code !in 200..299) return@withContext null
        BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val form = "client_id=${URLEncoder.encode(SpotifyConfig.CLIENT_ID, "UTF-8")}" +
                "&grant_type=authorization_code" +
                "&code=${URLEncoder.encode(code, "UTF-8")}" +
                "&redirect_uri=${URLEncoder.encode(SpotifyConfig.REDIRECT_URI, "UTF-8")}" +
                "&code_verifier=${URLEncoder.encode(verifier, "UTF-8")}"

            val conn = (URL(SpotifyConfig.TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(form) }
            if (conn.responseCode !in 200..299) return@withContext false
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = gson.fromJson(body, JsonObject::class.java)
            val accessToken = json.get("access_token")?.asString ?: return@withContext false
            val refreshToken = json.get("refresh_token")?.asString
            prefs.edit()
                .putString("access_token", accessToken)
                .apply { if (refreshToken != null) putString("refresh_token", refreshToken) }
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val refresh = getRefreshToken() ?: return@withContext false
        try {
            val form = "client_id=${URLEncoder.encode(SpotifyConfig.CLIENT_ID, "UTF-8")}" +
                "&grant_type=refresh_token" +
                "&refresh_token=${URLEncoder.encode(refresh, "UTF-8")}"

            val conn = (URL(SpotifyConfig.TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream).use { it.write(form) }
            if (conn.responseCode !in 200..299) return@withContext false
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = gson.fromJson(body, JsonObject::class.java)
            val newAccess = json.get("access_token")?.asString ?: return@withContext false
            val newRefresh = json.get("refresh_token")?.asString
            prefs.edit()
                .putString("access_token", newAccess)
                .apply { if (newRefresh != null) putString("refresh_token", newRefresh) }
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}