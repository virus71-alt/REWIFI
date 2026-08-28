package com.rewifi.app.data

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.ByteArrayOutputStream

/**
 * Builds a Wi-Fi Simple Configuration (WSC) NDEF message and writes it to an NFC
 * tag. Phones natively recognise `application/vnd.wfa.wsc` tags and offer to join
 * the network on tap — so a tag written here becomes a "tap to connect" sticker.
 */
object NfcWriter {

    private const val MIME_WSC = "application/vnd.wfa.wsc"

    // WSC attribute IDs (TLV, 2-byte type + 2-byte length).
    private const val CREDENTIAL = 0x100E
    private const val NETWORK_INDEX = 0x1026
    private const val SSID = 0x1045
    private const val AUTH_TYPE = 0x1003
    private const val ENCRYPT_TYPE = 0x100F
    private const val NETWORK_KEY = 0x1027
    private const val MAC_ADDRESS = 0x1020

    // Auth/encryption values.
    private const val AUTH_OPEN = 0x0001
    private const val AUTH_WPA2PSK = 0x0020
    private const val ENC_NONE = 0x0001
    private const val ENC_AES = 0x0008

    fun wifiNdef(ssid: String, password: String, open: Boolean): NdefMessage {
        val record = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            MIME_WSC.toByteArray(Charsets.US_ASCII),
            ByteArray(0),
            wscPayload(ssid, password, open)
        )
        return NdefMessage(arrayOf(record))
    }

    /** Write [message] to [tag]. Returns true on success. */
    fun write(tag: Tag, message: NdefMessage): Boolean {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable || ndef.maxSize < message.toByteArray().size) false
                else { ndef.writeNdefMessage(message); true }
            } catch (e: Exception) {
                false
            } finally {
                runCatching { ndef.close() }
            }
        }
        val formatable = NdefFormatable.get(tag) ?: return false
        return try {
            formatable.connect()
            formatable.format(message)
            true
        } catch (e: FormatException) {
            false
        } catch (e: Exception) {
            false
        } finally {
            runCatching { formatable.close() }
        }
    }

    private fun wscPayload(ssid: String, password: String, open: Boolean): ByteArray {
        val credential = ByteArrayOutputStream().apply {
            writeTlv(NETWORK_INDEX, byteArrayOf(1))
            writeTlv(SSID, ssid.toByteArray(Charsets.UTF_8))
            writeTlv(AUTH_TYPE, twoBytes(if (open) AUTH_OPEN else AUTH_WPA2PSK))
            writeTlv(ENCRYPT_TYPE, twoBytes(if (open) ENC_NONE else ENC_AES))
            writeTlv(NETWORK_KEY, if (open) ByteArray(0) else password.toByteArray(Charsets.UTF_8))
            writeTlv(MAC_ADDRESS, ByteArray(6))
        }.toByteArray()

        return ByteArrayOutputStream().apply { writeTlv(CREDENTIAL, credential) }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeTlv(type: Int, value: ByteArray) {
        write(twoBytes(type))
        write(twoBytes(value.size))
        write(value)
    }

    private fun twoBytes(v: Int) = byteArrayOf((v shr 8 and 0xFF).toByte(), (v and 0xFF).toByte())
}
