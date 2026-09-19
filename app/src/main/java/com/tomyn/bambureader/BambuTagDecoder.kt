package com.tomyn.bambureader

/**
 * Decodeur base sur les positions exactes de blocs documentees par la communaute
 * (script parse.py de queengooborg, base sur les recherches Bambu-Research-Group).
 * Les numeros de bloc sont GLOBAUX (0 a 63 sur un tag MIFARE Classic 1K), pas relatifs au secteur.
 *
 * Positions verifiees :
 * - Bloc 1, octets 8-15 : code matiere interne (ex "GFA00"), en ASCII
 * - Bloc 2 (16 octets) : type de filament (ex "PLA"), en ASCII
 * - Bloc 4 (16 octets) : type detaille (ex "PLA Basic"), en ASCII
 * - Bloc 5, octets 0-3 : couleur au format RGBA (4 octets hexadecimaux)
 * - Bloc 5, octets 4-5 : poids de la bobine en grammes (entier 16 bits, little-endian)
 * - Bloc 6, octets 0-1 : temperature de sechage
 * - Bloc 6, octets 2-3 : duree de sechage
 * - Bloc 6, octets 6-7 : temperature du plateau
 * - Bloc 6, octets 8-9 : temperature buse max
 * - Bloc 6, octets 10-11 : temperature buse min
 */
object BambuTagDecoder {

    data class InfoFilament(
        val codeMatiere: String?,
        val typeFilament: String?,
        val typeDetaille: String?,
        val couleurHex: String?,
        val poidsGrammes: Int?,
        val tempSechage: Int?,
        val dureeSechage: Int?,
        val tempPlateau: Int?,
        val tempBuseMin: Int?,
        val tempBuseMax: Int?
    )

    private fun versTexteAscii(data: ByteArray): String {
        return try {
            data.toString(Charsets.US_ASCII).replace('\u0000', ' ').trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun versEntierLE(data: ByteArray): Int {
        var resultat = 0
        for (i in data.indices) {
            resultat = resultat or ((data[i].toInt() and 0xFF) shl (8 * i))
        }
        return resultat
    }

    /**
     * Decode les infos filament a partir d'une map {numero de bloc global -> 16 octets}.
     * Les blocs manquants (non lus, echec d'authentification) sont geres sans planter.
     */
    fun decoder(blocs: Map<Int, ByteArray>): InfoFilament {
        val bloc1 = blocs[1]
        val bloc2 = blocs[2]
        val bloc4 = blocs[4]
        val bloc5 = blocs[5]
        val bloc6 = blocs[6]

        val codeMatiere = bloc1?.let {
            if (it.size >= 16) versTexteAscii(it.copyOfRange(8, 16)).ifBlank { null } else null
        }
        val typeFilament = bloc2?.let { versTexteAscii(it).ifBlank { null } }
        val typeDetaille = bloc4?.let { versTexteAscii(it).ifBlank { null } }

        val couleurHex = bloc5?.let {
            if (it.size >= 4) "#" + it.copyOfRange(0, 4).joinToString("") { b -> String.format("%02X", b) } else null
        }
        val poidsGrammes = bloc5?.let {
            if (it.size >= 6) versEntierLE(it.copyOfRange(4, 6)) else null
        }

        val tempSechage = bloc6?.let { if (it.size >= 2) versEntierLE(it.copyOfRange(0, 2)) else null }
        val dureeSechage = bloc6?.let { if (it.size >= 4) versEntierLE(it.copyOfRange(2, 4)) else null }
        val tempPlateau = bloc6?.let { if (it.size >= 8) versEntierLE(it.copyOfRange(6, 8)) else null }
        val tempBuseMax = bloc6?.let { if (it.size >= 10) versEntierLE(it.copyOfRange(8, 10)) else null }
        val tempBuseMin = bloc6?.let { if (it.size >= 12) versEntierLE(it.copyOfRange(10, 12)) else null }

        return InfoFilament(
            codeMatiere = codeMatiere,
            typeFilament = typeFilament,
            typeDetaille = typeDetaille,
            couleurHex = couleurHex,
            poidsGrammes = poidsGrammes,
            tempSechage = tempSechage,
            dureeSechage = dureeSechage,
            tempPlateau = tempPlateau,
            tempBuseMin = tempBuseMin,
            tempBuseMax = tempBuseMax
        )
    }
}
