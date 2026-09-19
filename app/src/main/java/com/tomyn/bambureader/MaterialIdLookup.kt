package com.tomyn.bambureader

/**
 * Table de correspondance des codes matiere internes Bambu Lab (ex: "GFA00") vers un nom
 * lisible. Ces codes sont documentes publiquement par la communaute (issue #42 du depot
 * Bambu-Research-Group/RFID-Tag-Guide, et le composant Home Assistant ha-bambulab).
 * Liste non exhaustive : Bambu ajoute regulierement de nouvelles references.
 */
object MaterialIdLookup {

    private val table = mapOf(
        "GFA00" to "Bambu PLA Basic",
        "GFA01" to "Bambu PLA Matte",
        "GFA02" to "Bambu PLA Metal",
        "GFA05" to "Bambu PLA Silk",
        "GFA07" to "Bambu PLA Marble",
        "GFA08" to "Bambu PLA Sparkle",
        "GFA09" to "Bambu PLA Wood",
        "GFA11" to "Bambu PLA Galaxy",
        "GFA12" to "Bambu PLA Glow",
        "GFA13" to "Bambu PLA Aero",
        "GFA50" to "Bambu PLA-CF",
        "GFB00" to "Bambu ABS",
        "GFB01" to "Bambu ASA",
        "GFB50" to "Bambu ABS-GF",
        "GFC00" to "Bambu PC",
        "GFG00" to "Bambu PETG Basic",
        "GFG01" to "Bambu PETG Translucent",
        "GFG02" to "Bambu PETG HF",
        "GFG50" to "Bambu PETG-CF",
        "GFN01" to "Bambu PA6-CF",
        "GFN03" to "Bambu PA-CF",
        "GFN04" to "Bambu PAHT-CF",
        "GFN08" to "Bambu PA6-GF",
        "GFS00" to "Bambu Support W",
        "GFS01" to "Bambu Support G",
        "GFT01" to "Bambu TPU 95A",
        "GFT02" to "Bambu TPU 90A",
        "GFU00" to "Bambu TPU for AMS",
        "GFU01" to "Bambu TPU 95A HF",
        "GFL00" to "PolyLite PLA",
        "GFL01" to "PolyTerra PLA",
        "GFL98" to "PLA-CF generique",
        "GFL99" to "PLA generique",
        "GFG99" to "PETG generique",
        "GFC99" to "PC generique",
        "GFN98" to "PA-CF generique",
        "GFN99" to "PA generique",
        "GFB98" to "ASA generique",
        "GFB99" to "ABS generique",
        "GFS99" to "PVA generique",
        "GFU99" to "TPU generique"
    )

    /**
     * Cherche un code matiere (GFxxx) dans un texte donne et retourne son nom lisible SI ce
     * code est connu dans la table ci-dessus. Si le code est trouve mais pas dans la table,
     * retourne null (plutot qu'un texte "inconnu") pour que l'appelant puisse se rabattre sur
     * le nom en toutes lettres deja present ailleurs sur le tag (bloc "type detaille").
     */
    fun trouverEtTraduire(texte: String): Pair<String, String>? {
        val regex = Regex("GF[A-Z0-9]{3}")
        val trouve = regex.find(texte) ?: return null
        val code = trouve.value
        val nom = table[code] ?: return null
        return Pair(code, nom)
    }
}
