package com.tomyn.bambureader

/**
 * Abstraction minimale d'un tag MIFARE Classic.
 * Elle isole la logique de lecture d'Android pour pouvoir la tester sur un PC
 * avec un faux tag qui simule des pannes (voir AccesMifare pour la vraie implementation).
 */
interface AccesTag {
    val nbSecteurs: Int
    fun premierBloc(secteur: Int): Int
    fun nbBlocs(secteur: Int): Int

    /**
     * (Re)selectionne le tag : ferme puis rouvre la connexion.
     * Necessaire apres toute authentification ratee (le tag repasse en veille)
     * ou apres une erreur de communication. Retourne false si le tag n'est plus la.
     */
    fun reconnecter(): Boolean

    /** true = authentifie, false = refuse. Peut lever une exception si le tag est perdu. */
    fun authentifier(secteur: Int, cle: ByteArray): Boolean

    /** Lit un bloc de 16 octets. Leve une exception en cas d'echec. */
    fun lireBloc(numBloc: Int): ByteArray

    fun fermer()
}

data class StatutSecteur(val secteur: Int, val lu: Boolean, val detail: String)

data class ResultatLecture(
    val blocs: Map<Int, ByteArray>,
    val statutSecteurs: List<StatutSecteur>,
    val nbPasses: Int,
    val connexionImpossible: Boolean,
    /** Nombre total d'echecs (authentification refusee, erreur de lecture, tag perdu) rencontres pendant le scan, meme rattrapes ensuite. */
    val nbIncidents: Int = 0,
    /** Le tag a disparu en cours de lecture : la re-selection a echoue apres un premier contact (telephone deplace, tag trop eloigne...). */
    val tagPerdu: Boolean = false
) {
    /** Les blocs necessaires au decodeur (code matiere, type, couleur, poids, temperatures) sont tous la. */
    val essentielsLus: Boolean
        get() = LecteurTagRobuste.BLOCS_ESSENTIELS.all { it in blocs }

    val nbSecteursLus: Int
        get() = statutSecteurs.count { it.lu }
}

/**
 * Lecture en plusieurs passes :
 *  - les secteurs 0 et 1 (tout ce que decode l'appli) sont lus en priorite ;
 *  - a chaque echec, le tag est re-selectionne avant de continuer (un echec d'authentification
 *    met un MIFARE Classic en veille : reessayer sans reconnexion est inutile) ;
 *  - les blocs deja lus sont conserves d'une passe a l'autre, on ne relit que ce qui manque ;
 *  - si les secteurs essentiels sont illisibles, on n'insiste pas sur les autres (tag non Bambu, ou perdu).
 */
object LecteurTagRobuste {

    const val MAX_PASSES = 3
    const val DETAIL_AUTH_REFUSEE = "authentification refusee"
    const val SECTEURS_ESSENTIELS = 2                // secteurs 0 et 1
    val BLOCS_ESSENTIELS = setOf(1, 2, 4, 5, 6)      // cf. BambuTagDecoder

    fun lire(
        acces: AccesTag,
        cles: List<ByteArray>,
        pause: (Long) -> Unit = { ms -> try { Thread.sleep(ms) } catch (e: InterruptedException) { Thread.currentThread().interrupt() } }
    ): ResultatLecture {
        val blocs = mutableMapOf<Int, ByteArray>()
        val secteursLus = mutableSetOf<Int>()
        val details = mutableMapOf<Int, String>()
        val aLire = (0 until acces.nbSecteurs).filter { cles.getOrNull(it) != null }
        var connexionImpossible = false
        var passesFaites = 0
        var incidents = 0
        var tagPerdu = false

        for (passe in 1..MAX_PASSES) {
            if (passe > 1) pause(120)
            if (!acces.reconnecter()) {
                if (passesFaites == 0) connexionImpossible = true else tagPerdu = true
                break
            }
            passesFaites = passe

            for (secteur in aLire) {
                if (secteur in secteursLus) continue

                val ok = try {
                    lireSecteur(acces, secteur, cles[secteur], blocs, details)
                } catch (e: Exception) {
                    details[secteur] = "tag perdu (${e.message ?: e.javaClass.simpleName})"
                    false
                }

                if (ok) {
                    secteursLus += secteur
                    continue
                }
                incidents++
                // Secteur essentiel en echec : inutile de continuer cette passe, on repart d'une reconnexion propre.
                if (secteur < SECTEURS_ESSENTIELS) break
                // Sinon on re-selectionne le tag et on passe au secteur suivant ; s'il a disparu, on arrete la passe.
                if (!acces.reconnecter()) {
                    tagPerdu = true
                    break
                }
            }

            val essentielsOk = (0 until SECTEURS_ESSENTIELS).all { it in secteursLus }
            if (secteursLus.size == aLire.size) break
            if (essentielsOk && passe >= 2) break
        }

        acces.fermer()

        val statuts = (0 until acces.nbSecteurs).map { s ->
            StatutSecteur(s, s in secteursLus, details[s] ?: "non tente")
        }
        return ResultatLecture(blocs.toMap(), statuts, passesFaites, connexionImpossible, incidents, tagPerdu)
    }

    private fun lireSecteur(
        acces: AccesTag,
        secteur: Int,
        cle: ByteArray,
        blocs: MutableMap<Int, ByteArray>,
        details: MutableMap<Int, String>
    ): Boolean {
        if (!acces.authentifier(secteur, cle)) {
            details[secteur] = DETAIL_AUTH_REFUSEE
            return false
        }
        val premier = acces.premierBloc(secteur)
        for (i in 0 until acces.nbBlocs(secteur)) {
            val numBloc = premier + i
            if (numBloc in blocs) continue
            try {
                blocs[numBloc] = acces.lireBloc(numBloc)
            } catch (e: Exception) {
                details[secteur] = "bloc $numBloc illisible (${e.message ?: e.javaClass.simpleName})"
                return false
            }
        }
        details[secteur] = "OK"
        return true
    }
}
