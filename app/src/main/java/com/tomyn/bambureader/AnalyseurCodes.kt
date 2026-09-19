package com.tomyn.bambureader

data class CodeLu(val nom: String?, val hex: String)

data class ResultatAnalyse(
    val codes: List<CodeLu>,
    /** Lignes ou morceaux de texte ressemblant a un code hex mais illisibles (trop d'erreurs de lecture). */
    val illisibles: List<String>
)

/**
 * Extrait des couleurs (nom facultatif + code hex a 6 caracteres) d'un texte libre :
 * saisie au clavier, texte colle depuis un site ou un tableur, ou texte lu sur une photo.
 *
 * Formats acceptes, un par ligne : "Red #CE3845", "#CE3845", "CE3845", "Red;CE3845", "CE3845 Red".
 * Sans '#', le code doit contenir au moins un chiffre (pour ne pas confondre un mot comme "Facade" avec un code).
 */
object AnalyseurCodes {

    // 6 caracteres apres le '#', avec au plus un espace apres chacun des 5 premiers : la lecture d'une photo
    // coupe parfois un code en deux ("#B1 B3B3"). Le 6e caractere est suivi de rien d'alphanumerique.
    private val REGEX_DIESE = Regex("#\\s*((?:[0-9A-Za-z]\\s?){5}[0-9A-Za-z])(?![0-9A-Za-z])")
    private val REGEX_HEX = Regex("^[0-9A-Fa-f]{6}$")
    private val SEPARATEURS = Regex("[;,:|\\t]+")
    private val ESPACES = Regex("\\s+")

    fun analyser(texte: String, depuisPhoto: Boolean = false): ResultatAnalyse {
        val codes = LinkedHashMap<String, CodeLu>()
        val illisibles = mutableListOf<String>()

        for (brute in texte.lines()) {
            val ligne = brute.trim()
            if (ligne.isEmpty()) continue

            val correspondances = REGEX_DIESE.findAll(ligne).toList()
            if (correspondances.isNotEmpty()) {
                val valides = mutableListOf<Pair<IntRange, String>>()
                for (m in correspondances) {
                    val brut = m.groupValues[1].replace(" ", "")
                    val hex = normaliser(brut, depuisPhoto)
                    if (hex != null) valides.add(m.range to hex) else illisibles.add("#" + m.groupValues[1])
                }
                // Un seul code sur la ligne : le reste du texte est le nom. Plusieurs : impossible d'associer les noms.
                val nom = if (correspondances.size == 1 && valides.size == 1) nettoyerNom(ligne.removeRange(valides[0].first)) else null
                for ((_, hex) in valides) ajouter(codes, CodeLu(nom, hex))
                continue
            }

            val mots = ligne.replace(SEPARATEURS, " ").split(ESPACES).filter { it.isNotEmpty() }
            if (mots.isEmpty()) continue
            val dernier = mots.last()
            val premier = mots.first()
            when {
                estCodeSansDiese(dernier) ->
                    ajouter(codes, CodeLu(nettoyerNom(mots.dropLast(1).joinToString(" ")), dernier.uppercase()))
                mots.size > 1 && estCodeSansDiese(premier) ->
                    ajouter(codes, CodeLu(nettoyerNom(mots.drop(1).joinToString(" ")), premier.uppercase()))
                // sinon : ligne sans code (titre, nom de couleur seul...) -> ignoree ; on ne signale que les '#' mal formes
                '#' in ligne -> illisibles.add(ligne)
            }
        }
        return ResultatAnalyse(codes.values.toList(), illisibles)
    }

    private fun ajouter(codes: LinkedHashMap<String, CodeLu>, c: CodeLu) {
        codes.putIfAbsent(c.hex + "|" + (c.nom ?: ""), c)
    }

    private fun estCodeSansDiese(mot: String): Boolean =
        REGEX_HEX.matches(mot) && mot.any { it.isDigit() }

    private fun nettoyerNom(brut: String): String? {
        val nom = brut.replace(SEPARATEURS, " ").replace(ESPACES, " ").trim().trim('-', '=', '>', '(', ')', '.').trim()
        return if (nom.any { it.isLetter() }) nom else null
    }

    /**
     * Retourne le code en majuscules s'il est valide, sinon null.
     * Depuis une photo, les confusions frequentes de lecture sont corrigees : O/Q -> 0, I/l -> 1, S -> 5, Z -> 2, G -> 6, T -> 7.
     * (8 et B, eux, sont tous les deux valides en hexadecimal : impossible a corriger, d'ou la verification manuelle.)
     */
    fun normaliser(brut: String, depuisPhoto: Boolean): String? {
        val texte = if (!depuisPhoto) brut else brut.map { c ->
            when (c) {
                'O', 'o', 'Q' -> '0'
                'I', 'l', 'i' -> '1'
                'S', 's' -> '5'
                'Z', 'z' -> '2'
                'G' -> '6'
                'T' -> '7'
                else -> c
            }
        }.joinToString("")
        return if (REGEX_HEX.matches(texte)) texte.uppercase() else null
    }
}
