import com.tomyn.bambureader.BambuKeyDeriver

/**
 * Test de la derivation des cles MIFARE (HKDF) sur une paire REELLE :
 * UID B2A9F8ED et les 16 cles de secteur que l'appli avait validees (authentification OK)
 * sur une vraie bobine (dump fourni par legallou sur le forum, septembre 2026).
 */
fun main() {
    val uid = byteArrayOf(0xB2.toByte(), 0xA9.toByte(), 0xF8.toByte(), 0xED.toByte())
    val attendu = listOf(
        "3E8710EE8F7D", "A89208BB7D5B", "7C9A7A012C2A", "6B01D52350A9",
        "3C2D7B1609EE", "AFFA0C1F0E14", "2F01187DE6B9", "0F02E0F64D03",
        "8F37ED832175", "C78946C3554F", "5A7F5FA9C3AB", "1E48A8ECCE30",
        "4864C0A7A077", "E8CF14FF1CB9", "A62E39772720", "1134832B96A2"
    )
    var echecs = 0
    fun check(nom: String, ok: Boolean, detail: String = "") {
        println((if (ok) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
        if (!ok) echecs++
    }

    val cles = BambuKeyDeriver.deriverClesA(uid)
    check("16 cles derivees", cles.size == 16, "obtenu=${cles.size}")
    check("chaque cle fait 6 octets", cles.all { it.size == 6 })
    for (i in attendu.indices) {
        val hex = cles.getOrNull(i)?.joinToString("") { "%02X".format(it) } ?: "?"
        check("secteur $i", hex == attendu[i], "obtenu=$hex attendu=${attendu[i]}")
    }
    val autre = BambuKeyDeriver.deriverClesA(byteArrayOf(1, 2, 3, 4))
    check("un autre UID donne d'autres cles", autre[0].joinToString() != cles[0].joinToString())
    check("deterministe (meme UID, memes cles)",
        BambuKeyDeriver.deriverClesA(uid).map { it.joinToString() } == cles.map { it.joinToString() })

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
