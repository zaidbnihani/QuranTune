package com.example.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * أداة تشفير بسيطة لحماية رسائل المزامنة عبر MQTT.
 * تستخدم خوارزمية AES-GCM مع مفتاح مشترك بين الجهازين المرتبطين.
 */
object CryptoHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    /**
     * يولد مفتاح تشفير عشوائي جديد بصيغة نصية (Base64) لمشاركته عبر QR.
     */
    fun generateSharedSecret(): String {
        val keyBytes = ByteArray(32) // 256-bit key
        SecureRandom().nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    private fun getKeyFromSecret(secret: String): SecretKeySpec {
        val keyBytes = Base64.decode(secret, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * يشفر نصًا عاديًا (plaintext) ويعيد نصًا مشفرًا بصيغة Base64 (يحتوي IV + النص المشفر).
     * يرجع null إذا فشل التشفير.
     */
    fun encrypt(plainText: String, sharedSecret: String): String? {
        return try {
            val secretKey = getKeyFromSecret(sharedSecret)
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * يفك تشفير نص مشفر بصيغة Base64 ويعيد النص الأصلي.
     * يرجع null إذا فشل فك التشفير (مثلاً مفتاح خاطئ أو بيانات تالفة).
     */
    fun decrypt(encryptedText: String, sharedSecret: String): String? {
        return try {
            val secretKey = getKeyFromSecret(sharedSecret)
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val encryptedBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * يولد مفتاح تشفير مستقر وثابت بناءً على معرفي الجهازين المرتبطين يدوياً.
     * هذا يضمن تطابق مفاتيح التشفير وفك التشفير تلقائياً دون الحاجة لمسح رمز QR.
     */
    fun generateDeterministicSecret(id1: String, id2: String): String {
        val sorted = listOf(id1, id2).sorted().joinToString(":")
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(sorted.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * يولد زوج مفاتيح EC (Elliptic Curve) لبروتوكول تبادل المفاتيح ECDH.
     */
    fun generateEcKeyPair(): java.security.KeyPair? {
        return try {
            val kpg = java.security.KeyPairGenerator.getInstance("EC")
            kpg.initialize(256) // Secp256r1
            kpg.generateKeyPair()
        } catch (e: java.lang.Exception) {
            android.util.Log.e("CryptoHelper", "Error generating EC KeyPair", e)
            null
        }
    }

    /**
     * يستخرج المفتاح العام كـ Base64 من زوج المفاتيح.
     */
    fun getPublicKeyString(keyPair: java.security.KeyPair): String {
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * يستخرج المفتاح الخاص كـ Base64 من زوج المفاتيح.
     */
    fun getPrivateKeyString(keyPair: java.security.KeyPair): String {
        return Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
    }

    /**
     * يحسب المفتاح المشترك (Shared Secret) باستخدام المفتاح الخاص المحلي والمفتاح العام عن بعد عبر ECDH.
     * ثم يمرره عبر SHA-256 لتوليد مفتاح AES موحد بطول 256 بت.
     */
    fun computeECDHSharedSecret(localPrivateKeyStr: String, remotePublicKeyStr: String): String? {
        return try {
            val privateKeyBytes = Base64.decode(localPrivateKeyStr, Base64.NO_WRAP)
            val publicKeyBytes = Base64.decode(remotePublicKeyStr, Base64.NO_WRAP)

            val kf = java.security.KeyFactory.getInstance("EC")
            val privateKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
            val publicKey = kf.generatePublic(java.security.spec.X509EncodedKeySpec(publicKeyBytes))

            val ka = javax.crypto.KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(publicKey, true)
            val rawSecret = ka.generateSecret()

            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawSecret)
            Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        } catch (e: java.lang.Exception) {
            android.util.Log.e("CryptoHelper", "Error computing ECDH shared secret", e)
            null
        }
    }
}
