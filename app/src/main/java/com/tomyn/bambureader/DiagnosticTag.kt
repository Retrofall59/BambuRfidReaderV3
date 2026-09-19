package com.tomyn.bambureader

enum class NiveauTag(val libelle: String, val pastille: String) {
    SAIN("Tag sain", "\u2705"),
    LIMITE("Tag limite", "\u26A0\uFE0F"),
    DEFAILLANT("Tag defaillant", "\u274C"),
    NON_EVALUABLE("Diagnostic impossible", "\u2754")
}

data class DiagnosticTag(
    val niveau: NiveauTag,
    val raisons: List<String>,
    val conseil: String?
)

/** Ce que l'appli sait deja de la session en cours (utile pour distinguer "tag HS" et "telephone qui rame"). */
data class ContexteDiagnostic(
    /** Nombre d'AUTRES tags lus sans aucun souci depuis le lancement de l'appli. */
    val autresTagsSainsVus: Int = 0,
    /** Nombre de scans precedents de CE tag (meme UID) juges defaillants. */
    val scansDefaillantsPrecedents: Int = 0
)

/**
 * Verdict par scan, a partir de la qualite de lecture (ResultatLecture) et de la coherence des donnees decodees.
 *
 * ATTENTION : les seuils ci-dessous sont des estimations, pas calibrees sur des mesures reelles.
 * Un seul scan ne prouve rien : c'est pourquoi le conseil tient compte du contexte de session.
 */
object EvaluateurTag {

    /** A partir de ce nombre d'echecs pendant un scan, meme rattrapes par les reessais, le tag est juge defaillant. */
    const val SEUIL_INCIDENTS_DEFAILLANT = 4

    /** Plages volontairement larges : on ne veut signaler que des valeurs impossibles, pas des valeurs inhabituelles. */
    private const val POIDS_MAX_G = 10000
    private const val TEMP_BUSE_MAX_C = 500
    private const val TEMP_PLATEAU_MAX_C = 200
    private const val TEMP_SECHAGE_MAX_C = 150

    fun evaluer(
        lecture: ResultatLecture,
        info: BambuTagDecoder.InfoFilament,
        ctx: ContexteDiagnostic = ContexteDiagnostic()
    ): DiagnosticTag {

        if (lecture.connexionImpossible) {
            return DiagnosticTag(
                NiveauTag.NON_EVALUABLE,
                listOf("Connexion au tag impossible"),
                "Repositionne le telephone. Si ca echoue a chaque fois, il ne supporte sans doute pas le MIFARE Classic."
            )
        }

        val secteur0 = lecture.statutSecteurs.firstOrNull { it.secteur == 0 }
        if (lecture.blocs.isEmpty() && secteur0?.detail == LecteurTagRobuste.DETAIL_AUTH_REFUSEE) {
            return DiagnosticTag(
                NiveauTag.NON_EVALUABLE,
                listOf("Les cles Bambu sont refusees des le secteur 0"),
                "Ce n'est probablement pas un tag Bambu (ou son UID a ete mal lu) : rescanne pour verifier."
            )
        }

        val raisons = mutableListOf<String>()
        var niveau = NiveauTag.SAIN

        fun monterA(n: NiveauTag) { if (n.ordinal > niveau.ordinal) niveau = n }

        if (!lecture.essentielsLus) {
            monterA(NiveauTag.DEFAILLANT)
            raisons += "Infos essentielles illisibles (secteurs 0 et 1) apres ${lecture.nbPasses} passe(s)"
        } else {
            val nonLus = lecture.statutSecteurs.filter { !it.lu }.map { it.secteur }
            if (lecture.nbIncidents >= SEUIL_INCIDENTS_DEFAILLANT) {
                monterA(NiveauTag.DEFAILLANT)
                raisons += "${lecture.nbIncidents} echecs de lecture pendant le scan"
            } else if (lecture.nbIncidents > 0) {
                monterA(NiveauTag.LIMITE)
                raisons += if (nonLus.isEmpty())
                    "${lecture.nbIncidents} echec(s) de lecture rattrape(s) par les reessais"
                else
                    "${lecture.nbIncidents} echec(s) de lecture pendant le scan"
            }
            if (nonLus.isNotEmpty()) {
                monterA(NiveauTag.LIMITE)
                raisons += "Secteurs non lus : ${nonLus.joinToString(", ")}"
            }
        }

        val incoherences = incoherences(info)
        if (incoherences.isNotEmpty()) {
            monterA(NiveauTag.DEFAILLANT)
            raisons += "Donnees incoherentes : ${incoherences.joinToString(", ")}"
        }

        val conseil = when (niveau) {
            NiveauTag.SAIN, NiveauTag.NON_EVALUABLE -> null
            NiveauTag.LIMITE ->
                "Lecture reussie mais instable : rescanne pour confirmer. Si l'AMS le refuse de temps en temps, remplace-le."
            NiveauTag.DEFAILLANT ->
                if (incoherences.isNotEmpty()) {
                    "Les donnees memorisees dans le tag sont invalides : l'AMS le refusera. A remplacer."
                } else {
                    val telephone = if (ctx.autresTagsSainsVus > 0)
                        "Ton telephone lit bien d'autres tags, donc ce tag est probablement en cause."
                    else
                        "Impossible de dire si c'est le tag ou le telephone : scanne un autre tag pour comparer."
                    val confirmation = if (ctx.scansDefaillantsPrecedents > 0)
                        "Resultat confirme sur plusieurs scans."
                    else
                        "Recolle le telephone sans bouger et rescanne pour confirmer."
                    "$telephone $confirmation"
                }
        }
        return DiagnosticTag(niveau, raisons, conseil)
    }

    /** Valeurs impossibles dans les champs decodes (uniquement pour les blocs effectivement lus). */
    fun incoherences(info: BambuTagDecoder.InfoFilament): List<String> {
        val liste = mutableListOf<String>()

        fun texteInvalide(t: String?): Boolean =
            t != null && t.any { it.code < 0x20 || it.code > 0x7E }

        if (texteInvalide(info.codeMatiere) || texteInvalide(info.typeFilament) || texteInvalide(info.typeDetaille)) {
            liste += "textes illisibles"
        }
        if (info.poidsGrammes != null && info.poidsGrammes > POIDS_MAX_G) {
            liste += "poids ${info.poidsGrammes} g"
        }
        val min = info.tempBuseMin
        val max = info.tempBuseMax
        if ((min != null && min > TEMP_BUSE_MAX_C) || (max != null && max > TEMP_BUSE_MAX_C)) {
            liste += "temperature buse hors plage"
        } else if (min != null && max != null && min > max) {
            liste += "buse min > buse max"
        }
        if (info.tempPlateau != null && info.tempPlateau > TEMP_PLATEAU_MAX_C) {
            liste += "temperature plateau ${info.tempPlateau} C"
        }
        if (info.tempSechage != null && info.tempSechage > TEMP_SECHAGE_MAX_C) {
            liste += "temperature sechage ${info.tempSechage} C"
        }
        return liste
    }
}

/** Memorise, le temps d'une session, ce qui aide a interpreter un mauvais scan. Utilisee uniquement depuis le thread UI. */
class SessionDiagnostic {
    private val uidsSains = mutableSetOf<String>()
    private val defaillantsParUid = mutableMapOf<String, Int>()

    fun evaluer(uid: String, lecture: ResultatLecture, info: BambuTagDecoder.InfoFilament): DiagnosticTag {
        val ctx = ContexteDiagnostic(
            autresTagsSainsVus = (uidsSains - uid).size,
            scansDefaillantsPrecedents = defaillantsParUid[uid] ?: 0
        )
        val diagnostic = EvaluateurTag.evaluer(lecture, info, ctx)
        when (diagnostic.niveau) {
            NiveauTag.SAIN -> { uidsSains += uid; defaillantsParUid.remove(uid) }
            NiveauTag.DEFAILLANT -> defaillantsParUid[uid] = (defaillantsParUid[uid] ?: 0) + 1
            else -> {}
        }
        return diagnostic
    }
}
