import com.tomyn.bambureader.BambuTagDecoder

/**
 * Test du decodeur sur les blocs REELS d'une bobine Bambu PETG Translucent (dump fourni par
 * legallou sur le forum, septembre 2026). Les valeurs attendues sont celles que l'appli avait
 * affichees sur le telephone a partir de ce meme tag (poids 1000 g, buse 230-260, plateau 0,
 * sechage 65 C / 8 h, code GFG01) : le test verifie qu'on retombe dessus a l'identique.
 */
fun hex(s: String) = s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun main() {
    val blocs = mapOf(
        0 to hex("B2 A9 F8 ED 0E 08 04 00 05 AD F5 31 EE 34 50 90"),
        1 to hex("47 30 31 2D 43 30 00 00 47 46 47 30 31 00 00 00"),
        2 to hex("50 45 54 47 00 00 00 00 00 00 00 00 00 00 00 00"),
        4 to hex("50 45 54 47 20 54 72 61 6E 73 6C 75 63 65 6E 74"),
        5 to hex("00 00 00 00 E8 03 00 00 00 00 E0 3F 00 00 00 00"),
        6 to hex("41 00 08 00 00 00 00 00 04 01 E6 00 00 00 00 00"),
        8 to hex("A4 38 80 3E BC 02 20 03 00 00 80 3F CD CC 4C 3E"),
        9 to hex("72 A3 64 32 FF 4B 4D C3 99 5A FE 3E 79 C1 8C DF"),
        10 to hex("00 00 00 00 C9 00 00 00 00 00 00 00 00 00 00 00"),
        12 to hex("32 30 32 36 5F 30 31 5F 32 35 5F 31 34 5F 35 36"),
        13 to hex("32 30 32 36 30 31 32 35 00 00 00 00 00 00 00 00"),
        14 to hex("00 00 00 00 4A 01 00 00 00 00 00 00 00 00 00 00"),
        16 to hex("02 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00")
    )
    var echecs = 0
    fun check(nom: String, ok: Boolean, detail: String = "") {
        println((if (ok) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
        if (!ok) echecs++
    }

    val info = BambuTagDecoder.decoder(blocs)
    println("Decode : $info\n")
    check("code matiere GFG01", info.codeMatiere == "GFG01", "obtenu=${info.codeMatiere}")
    check("type PETG", info.typeFilament == "PETG", "obtenu=${info.typeFilament}")
    check("type detaille PETG Translucent", info.typeDetaille == "PETG Translucent", "obtenu=${info.typeDetaille}")
    check("poids 1000 g", info.poidsGrammes == 1000, "obtenu=${info.poidsGrammes}")
    check("sechage 65 C", info.tempSechage == 65, "obtenu=${info.tempSechage}")
    check("duree sechage 8 h", info.dureeSechage == 8, "obtenu=${info.dureeSechage}")
    check("plateau 0 C", info.tempPlateau == 0, "obtenu=${info.tempPlateau}")
    check("buse min 230", info.tempBuseMin == 230, "obtenu=${info.tempBuseMin}")
    check("buse max 260", info.tempBuseMax == 260, "obtenu=${info.tempBuseMax}")
    check("couleur presente (alpha 0 = transparent)", info.couleurHex != null, "obtenu=${info.couleurHex}")

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
