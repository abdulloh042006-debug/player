package com.shaxsiy.player.data.yandex

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

data class YandexPlaylistItem(
    val kind: Int,
    val title: String,
    val trackCount: Int
)

data class YandexTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long
)

data class YandexMusicUiState(
    val isConnected: Boolean = false,
    val isYandexAppInstalled: Boolean = false,
    val isLoading: Boolean = false,
    val accountLogin: String = "",
    val playlists: List<YandexPlaylistItem> = emptyList(),
    val likedSongs: List<YandexTrackItem> = emptyList(),
    val searchResults: List<YandexTrackItem> = emptyList(),
    val selectedPlaylistTitle: String? = null,
    val selectedPlaylistTracks: List<YandexTrackItem> = emptyList(),
    val statusMessage: String? = null
)

class YandexMusicManager(private val context: Context) {

    companion object {
        private const val API_BASE = "https://api.music.yandex.net"
        private const val SIGN_SALT = "XGRlBW9FXlekgbPrRHuSiA"
        const val OAUTH_TOKEN_URL =
            "https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d"
    }

    private val prefs = context.getSharedPreferences("yandex_music_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(
        YandexMusicUiState(
            isConnected = !getToken().isNullOrBlank(),
            isYandexAppInstalled = checkYandexInstalled(),
            accountLogin = prefs.getString("account_login", "").orEmpty()
        )
    )
    val state: StateFlow<YandexMusicUiState> = _state.asStateFlow()

    private fun checkYandexInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("ru.yandex.music", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getToken(): String? = prefs.getString("oauth_token", null)
    private fun getUid(): Long = prefs.getLong("account_uid", -1L)

    fun createGetTokenIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(OAUTH_TOKEN_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    suspend fun connectWithToken(rawInput: String) {
        val cleaned = extractToken(rawInput)
        if (cleaned.isBlank()) {
            _state.update { it.copy(statusMessage = "Yandex OAuth token noto‘g‘ri yoki bo‘sh.") }
            return
        }

        _state.update { it.copy(isLoading = true, statusMessage = null) }
        prefs.edit().putString("oauth_token", cleaned).apply()

        val accountOk = fetchAccountStatus()
        if (accountOk) {
            _state.update { it.copy(isConnected = true, statusMessage = "Yandex Music ulandi") }
            refreshAllData()
        } else {
            prefs.edit().remove("oauth_token").apply()
            _state.update {
                it.copy(
                    isConnected = false,
                    isLoading = false,
                    statusMessage = "Token yaroqsiz yoki internet aloqasi yo‘q."
                )
            }
        }
    }

    fun disconnect() {
        prefs.edit().clear().apply()
        _state.update {
            YandexMusicUiState(
                isConnected = false,
                isYandexAppInstalled = checkYandexInstalled(),
                statusMessage = "Yandex Music hisobidan chiqildi"
            )
        }
    }

    fun closePlaylistDetail() {
        _state.update { it.copy(selectedPlaylistTitle = null, selectedPlaylistTracks = emptyList()) }
    }

    suspend fun refreshAllData() {
        if (getToken().isNullOrBlank()) return
        _state.update { it.copy(isLoading = true, statusMessage = null) }
        try {
            if (getUid() <= 0L) {
                if (!fetchAccountStatus()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Yandex hisobini tekshirib bo‘lmadi. Internetni tekshiring."
                        )
                    }
                    return
                }
            }
            val playlists = fetchPlaylists()
            val liked = fetchLikedTracks()
            _state.update {
                it.copy(
                    isLoading = false,
                    playlists = playlists,
                    likedSongs = liked
                )
            }
        } catch (_: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Yandex Music bilan aloqa yo‘q. Oflayn pleyer ishlashda davom etmoqda."
                )
            }
        }
    }

    suspend fun openPlaylist(playlist: YandexPlaylistItem) {
        val uid = getUid()
        if (uid <= 0L) return
        _state.update { it.copy(isLoading = true, statusMessage = null) }
        try {
            val body = apiRequest("GET", "/users/$uid/playlists/${playlist.kind}") ?: run {
                _state.update { it.copy(isLoading = false, statusMessage = "Playlistni yuklab bo‘lmadi.") }
                return
            }
            val json = gson.fromJson(body, JsonObject::class.java)
            val tracksArray = json.getAsJsonObject("result")?.getAsJsonArray("tracks")
            val tracks = tracksArray?.mapNotNull { el ->
                val trackObj = el.asJsonObject.getAsJsonObject("track") ?: el.asJsonObject
                parseTrackObject(trackObj)
            }.orEmpty()

            _state.update {
                it.copy(
                    isLoading = false,
                    selectedPlaylistTitle = playlist.title,
                    selectedPlaylistTracks = tracks
                )
            }
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, statusMessage = "Playlistni ochishda xatolik.") }
        }
    }

    suspend fun searchTracks(query: String) {
        if (query.isBlank() || getToken().isNullOrBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        _state.update { it.copy(isLoading = true, statusMessage = null) }
        try {
            val encoded = withContext(Dispatchers.IO) { URLEncoder.encode(query, "UTF-8") }
            val body = apiRequest("GET", "/search?text=$encoded&type=track&page=0")
            if (body.isNullOrBlank()) {
                _state.update { it.copy(isLoading = false, searchResults = emptyList()) }
                return
            }
            val json = gson.fromJson(body, JsonObject::class.java)
            val results = json.getAsJsonObject("result")
                ?.getAsJsonObject("tracks")
                ?.getAsJsonArray("results")
                ?.mapNotNull { parseTrackObject(it.asJsonObject) }
                .orEmpty()

            _state.update { it.copy(isLoading = false, searchResults = results) }
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, statusMessage = "Qidiruvda xatolik yuz berdi.") }
        }
    }

    /**
     * Yandex Music trek ID bo‘yicha to‘g‘ridan-to‘g‘ri MP3 stream URL oladi.
     */
    suspend fun resolveStreamUrl(trackId: String): String? = withContext(Dispatchers.IO) {
        try {
            val infoBody = apiRequest("GET", "/tracks/$trackId/download-info") ?: return@withContext null
            val infoJson = gson.fromJson(infoBody, JsonObject::class.java)
            val formats = infoJson.getAsJsonArray("result") ?: return@withContext null

            var bestUrl: String? = null
            var bestBitrate = -1
            for (el in formats) {
                val obj = el.asJsonObject
                val codec = obj.get("codec")?.asString ?: ""
                val bitrate = obj.get("bitrateInKbps")?.asInt ?: 0
                val url = obj.get("downloadInfoUrl")?.asString ?: continue
                if (codec == "mp3" && bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                } else if (bestUrl == null) {
                    bestUrl = url
                }
            }
            val chosenUrl = bestUrl ?: return@withContext null
            val separator = if (chosenUrl.contains("?")) "&" else "?"
            val directJsonText = rawGet("$chosenUrl${separator}format=json") ?: return@withContext null
            val directObj = gson.fromJson(directJsonText, JsonObject::class.java)

            val host = directObj.get("host")?.asString ?: return@withContext null
            val path = directObj.get("path")?.asString ?: return@withContext null
            val ts = directObj.get("ts")?.asString ?: return@withContext null
            val s = directObj.get("s")?.asString ?: return@withContext null

            val sign = md5(SIGN_SALT + path.removePrefix("/") + s)
            "https://$host/get-mp3/$sign/$ts$path"
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchAccountStatus(): Boolean {
        val body = apiRequest("GET", "/account/status") ?: return false
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val account = json.getAsJsonObject("result")?.getAsJsonObject("account") ?: return false
            val uid = account.get("uid")?.asLong ?: return false
            val login = account.get("login")?.asString ?: "Yandex foydalanuvchi"
            prefs.edit()
                .putLong("account_uid", uid)
                .putString("account_login", login)
                .apply()
            _state.update { it.copy(accountLogin = login) }
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun fetchPlaylists(): List<YandexPlaylistItem> {
        val uid = getUid()
        if (uid <= 0L) return emptyList()
        val body = apiRequest("GET", "/users/$uid/playlists/list") ?: return emptyList()
        val json = gson.fromJson(body, JsonObject::class.java)
        val arr = json.getAsJsonArray("result") ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.asJsonObject
            YandexPlaylistItem(
                kind = obj.get("kind")?.asInt ?: return@mapNotNull null,
                title = obj.get("title")?.asString ?: "Nomsiz playlist",
                trackCount = obj.get("trackCount")?.asInt ?: 0
            )
        }
    }

    private suspend fun fetchLikedTracks(): List<YandexTrackItem> {
        val uid = getUid()
        if (uid <= 0L) return emptyList()
        val body = apiRequest("GET", "/users/$uid/likes/tracks") ?: return emptyList()
        val json = gson.fromJson(body, JsonObject::class.java)
        val ids = json.getAsJsonObject("result")
            ?.getAsJsonObject("library")
            ?.getAsJsonArray("tracks")
            ?.take(40)
            ?.mapNotNull { it.asJsonObject.get("id")?.asString }
            .orEmpty()

        if (ids.isEmpty()) return emptyList()
        val formBody = "track-ids=${URLEncoder.encode(ids.joinToString(","), "UTF-8")}"
        val detailsBody = apiRequest("POST", "/tracks", formBody) ?: return emptyList()
        val detailsJson = gson.fromJson(detailsBody, JsonObject::class.java)
        val arr = detailsJson.getAsJsonArray("result") ?: return emptyList()
        return arr.mapNotNull { parseTrackObject(it.asJsonObject) }
    }

    private fun parseTrackObject(obj: JsonObject): YandexTrackItem? {
        val id = obj.get("id")?.asString ?: return null
        val title = obj.get("title")?.asString ?: "Nomsiz"
        val artists = obj.getAsJsonArray("artists")
            ?.mapNotNull { it.asJsonObject.get("name")?.asString }
            ?.joinToString(", ")
            ?.ifBlank { "Noma'lum ijrochi" }
            ?: "Noma'lum ijrochi"
        val durationMs = obj.get("durationMs")?.asLong ?: 0L
        return YandexTrackItem(id = id, title = title, artist = artists, durationMs = durationMs)
    }

    private suspend fun apiRequest(method: String, path: String, formBody: String? = null): String? =
        withContext(Dispatchers.IO) {
            val token = getToken() ?: return@withContext null
            try {
                val conn = (URL("$API_BASE$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    setRequestProperty("Authorization", "OAuth $token")
                    setRequestProperty("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                    connectTimeout = 8000
                    readTimeout = 8000
                    if (formBody != null) {
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        doOutput = true
                        OutputStreamWriter(outputStream).use { it.write(formBody) }
                    }
                }
                if (conn.responseCode !in 200..299) return@withContext null
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }

    private fun rawGet(fullUrl: String): String? {
        return try {
            val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractToken(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("access_token=([^&\\s]+)").find(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}