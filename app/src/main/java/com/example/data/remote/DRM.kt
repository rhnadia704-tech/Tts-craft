package com.example.data.remote

import android.util.Log
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Handles Edge TTS DRM (Sec-MS-GEC calculation, clock skew correction, MUID generation).
 * Matches the reference mechanism in yynag/edge-tts-android and rany2/edge-tts.
 */
object DRM {
    private const val TAG = "EdgeTTS_DRM"

    const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    const val CHROMIUM_FULL_VERSION = "131.0.2903.51"
    const val CHROMIUM_MAJOR_VERSION = "131"
    const val SEC_MS_GEC_VERSION = "1-131.0.2903.51"
    const val CHROMIUM_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"
    const val CHROMIUM_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

    private const val WIN_EPOCH = 11644473600.0
    private const val S_TO_NS = 1e9

    private var clockSkewSeconds: Double = 0.0
    private val secureRandom = SecureRandom()

    /**
     * Generates the short-lived Sec-MS-GEC token based on Windows file time rounded to 5 minutes,
     * adjusted for any server clock skew.
     */
    fun generateSecMsGec(): String {
        var ticks = getUnixTimestamp()
        ticks += WIN_EPOCH
        ticks -= (ticks % 300)
        ticks *= S_TO_NS / 100
        val strToHash = "${ticks.toLong()}$TRUSTED_CLIENT_TOKEN"
        val digest = MessageDigest.getInstance("SHA-256").digest(strToHash.toByteArray(Charsets.US_ASCII))
        return BigInteger(1, digest).toString(16).uppercase().padStart(64, '0')
    }

    /**
     * Generates a 32-character hexadecimal MUID for the Cookie header.
     */
    fun generateMuid(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Updates the clock skew when the server returns a Date header (e.g. during 403 or health-check).
     */
    fun adjustClockSkew(rfc2616DateStr: String) {
        val serverTime = parseRfc2616Date(rfc2616DateStr)
        if (serverTime != null) {
            val clientTime = System.currentTimeMillis().toDouble() / 1000.0
            clockSkewSeconds = serverTime - clientTime
            Log.d(TAG, "Adjusted clock skew: $clockSkewSeconds seconds from server date: $rfc2616DateStr")
        }
    }

    private fun getUnixTimestamp(): Double {
        return (System.currentTimeMillis().toDouble() / 1000.0) + clockSkewSeconds
    }

    private fun parseRfc2616Date(dateStr: String): Double? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss 'GMT'",
            "EEE MMM dd yyyy HH:mm:ss",
            "EEE, dd-MMM-yyyy HH:mm:ss zzz"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return date.time.toDouble() / 1000.0
                }
            } catch (_: ParseException) {}
        }
        return null
    }
}
