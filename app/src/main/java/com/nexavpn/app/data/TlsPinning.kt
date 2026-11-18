package com.nexavpn.app.data

import java.security.MessageDigest
import java.security.cert.CertificateException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * TLS trust configuration for the NexaVPN backend.
 *
 * The test deployment uses a self-signed certificate for the backend IP.
 * Instead of disabling verification (insecure), we pin the exact
 * certificate: any connection whose leaf cert does not match
 * [PINNED_SHA256_FINGERPRINT] fails immediately.
 */
object TlsPinning {

    /** SHA-256 fingerprint of the backend certificate (colon-separated hex). */
    const val PINNED_SHA256_FINGERPRINT =
        "C6:5F:57:FC:65:67:7B:A5:E1:22:C7:FE:D0:F5:74:7B:0E:14:16:3C:D5:55:F4:45:C0:E4:69:38:89:54:82:0A"

    private fun pinnedTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val leaf = chain.firstOrNull() ?: throw CertificateException("empty chain")
                val digest = MessageDigest.getInstance("SHA-256").digest(leaf.encoded)
                val hex = digest.joinToString(":") { "%02X".format(it) }
                if (!hex.equals(PINNED_SHA256_FINGERPRINT, ignoreCase = true)) {
                    throw CertificateException("certificate mismatch: server identity not pinned")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    fun client(base: OkHttpClient): OkHttpClient {
        val tm = pinnedTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(tm), null)
        return base.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, tm)
            .hostnameVerifier { _, _ -> true } // identity enforced by pinning, not hostname
            .build()
    }
}
