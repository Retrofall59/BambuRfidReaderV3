package com.tomyn.bambureader

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Implementation Kotlin de l'algorithme de derivation de cles publie par
 * Bambu-Research-Group (deriveKeys.py) :
 *
 *   def kdf(uid):
 *       master = bytes([0x9a,0x75,0x9c,0xf2,0xc4,0xf7,0xca,0xff,
 *                        0x22,0x2c,0xb9,0x76,0x9b,0x41,0xbc,0x96])
 *       return HKDF(uid, 6, master, SHA256, 16, context=b"RFID-A\0")
 *
 * HKDF (RFC 5869) = Extract (HMAC-SHA256 du UID avec le "master" comme sel)
 *                 + Expand (derive N cles de 6 octets chacune, avec le contexte "RFID-A\0")
 *
 * Ceci reproduit fidelement l'algorithme documente publiquement (lecture seule des tags,
 * pas de signature RSA impliquee ici).
 */
object BambuKeyDeriver {

    private val MASTER_KEY = byteArrayOf(
        0x9a.toByte(), 0x75, 0x9c.toByte(), 0xf2.toByte(),
        0xc4.toByte(), 0xf7.toByte(), 0xca.toByte(), 0xff.toByte(),
        0x22, 0x2c, 0xb9.toByte(), 0x76,
        0x9b.toByte(), 0x41, 0xbc.toByte(), 0x96.toByte()
    )
    private val CONTEXT = "RFID-A".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00)
    private const val CLE_LONGUEUR = 6   // MIFARE Classic : chaque cle A/B fait 6 octets
    private const val NB_CLES = 16       // 16 secteurs sur un MIFARE Classic 1K

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /**
     * HKDF-Extract (RFC 5869) : PRK = HMAC-SHA256(salt, IKM)
     * Ici : salt = MASTER_KEY, IKM = uid
     */
    private fun hkdfExtract(uid: ByteArray): ByteArray {
        return hmacSha256(MASTER_KEY, uid)
    }

    /**
     * HKDF-Expand (RFC 5869) : derive la longueur totale necessaire (16 x 6 = 96 octets)
     * a partir du PRK, en iterant T(1)=HMAC(PRK, T(0)|info|0x01), T(2)=HMAC(PRK, T(1)|info|0x02), etc.
     */
    private fun hkdfExpand(prk: ByteArray, longueurTotale: Int): ByteArray {
        val resultat = ByteArray(longueurTotale)
        var t = ByteArray(0)
        var position = 0
        var compteur = 1
        while (position < longueurTotale) {
            val entree = t + CONTEXT + byteArrayOf(compteur.toByte())
            t = hmacSha256(prk, entree)
            val aCopier = minOf(t.size, longueurTotale - position)
            System.arraycopy(t, 0, resultat, position, aCopier)
            position += aCopier
            compteur++
        }
        return resultat
    }

    /**
     * Derive les 16 cles A (6 octets chacune) a partir de l'UID du tag.
     * @param uid l'UID brut du tag (generalement 4 ou 7 octets selon le type de MIFARE)
     * @return une liste de 16 tableaux de 6 octets, une cle par secteur
     */
    fun deriverClesA(uid: ByteArray): List<ByteArray> {
        val prk = hkdfExtract(uid)
        val flux = hkdfExpand(prk, NB_CLES * CLE_LONGUEUR)
        return (0 until NB_CLES).map { i ->
            flux.copyOfRange(i * CLE_LONGUEUR, (i + 1) * CLE_LONGUEUR)
        }
    }
}
