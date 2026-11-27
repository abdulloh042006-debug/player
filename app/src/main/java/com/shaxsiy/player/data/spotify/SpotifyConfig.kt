package com.shaxsiy.player.data.spotify

object SpotifyConfig {
    // Spotify Developer Dashboard'dan olingan Client ID ni shu yerga yozing
    const val CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID_HERE"

    // AndroidManifest.xml ichidagi intent-filter bilan aynan bir xil bo‘lishi kerak
    const val REDIRECT_URI = "shaxsiyplayer://callback"

    const val AUTH_ENDPOINT = "https://accounts.spotify.com/authorize"
    const val TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
    const val API_BASE = "https://api.spotify.com/v1"

    const val SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing playlist-read-private user-library-read"

    fun isConfigured(): Boolean {
        return CLIENT_ID.isNotBlank() && CLIENT_ID != "YOUR_SPOTIFY_CLIENT_ID_HERE"
    }
}