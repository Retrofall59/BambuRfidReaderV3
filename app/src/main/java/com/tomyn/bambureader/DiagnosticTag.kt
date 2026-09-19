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
    val conseil: String?,
    /** La qualite de lecture de CE scan est mauvaise (infos essentielles illisibles, ou beaucoup d'echecs). Sert a confirmer sur plusieurs scans. */
    val lectureMauvaise: Boolean = false
)

/** Ce que l'appli sait deja de la session en cours (utile pour distinguer "tag HS" et "telephone qui rame"). */
data class ContexteDiagnostic(
    /** Nombre d'AUTRES tags lus sans aucun souci depuis le lancement de l'appli. */
    val autresTagsSainsVus: Int = 0,
    /** Nombre de scans precedents de CE tag (meme UID), depuis son dernier scan sain, dont la qualite de lecture etait mauvaise. */
    val scansMauvaisPrecedents: Int = 0
)

/**
 * Verdict par scan, a partir de la qualite de lecture (ResultatLecture) et de la coherence des donnees decodees.
 *
 * Regle d'or (issue des premiers retours de testeurs) : UN SEUL mauvais scan ne suffit pas pour condamner un tag.
 * Un tag sain peut donner une lecture instable simplement parce que le telephone n'est pas bien place, et le scan
 * suivant, au meme endroit, se passe parfaitement. Une mauvaise lecture n'est donc "defaillante" qu'apres
 * DEUX mauvais scans de suite du meme tag ; un scan sain remet le compteur a zero. Seules des donnees impossibles
 * dans le tag condamnent immediatement (elles ne dependent pas de la position du telephone).
 *
 * Les seuils ci-dessous sont des estimations, pas calibrees sur des mesures reelles.
 */
object EvaluateurTag {

    /** A partir de ce nombre d'echecs pendant un scan, meme rattrapes par les reessais, la lecture est jugee mauvaise. */
    const val SEUIL_INCIDENTS_MAUVAISE = 4

    /** Plages volontairement larges : on ne veut signaler que des valeurs impossibles, pas des valeurs inhabituelles. */
    private const val POIDS_MAX_G = 10000
    private const val TEMP_BUSE_MAX_C = 500
    private const val TEMP_PLATEAU_MAX_C = 200
    private const val TEMP_SECHAGE_MAX_C = 150

    private const val CONSEIL_POSITION = "Pose le telephone a plat sur le tag, sans bouger, et rescanne."

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

        // Le tag a disparu en cours de lecture et on n'a rien d'exploitable : c'est la position / le mouvement, pas les cles.
        if (lecture.tagPerdu && !lecture.essentielsLus) {
            return DiagnosticTag(
                NiveauTag.NON_EVALUABLE,
                listOf("Le tag a disparu pendant la lecture (telephone deplace ?)"),
                CONSEIL_POSITION
            )
        }

        val secteur0 = lecture.statutSecteurs.firstOrNull { it.secteur == 0 }
        if (lecture.blocs.isEmpty() && secteur0?.detail == LecteurTagRobuste.DETAIL_AUTH_REFUSEE) {
            return DiagnosticTag(
                NiveauTag.NON_EVALUABLE,
                listOf("Les cles Bambu sont refusees des le secteur 0, a chaque tentative"),
                "Ce n'est probablement pas un tag Bambu (ou son UID a ete mal lu) : rescanne pour verifier."
            )
        }

        val raisons = mutableListOf<String>()
        var niveau = NiveauTag.SAIN

        fun monterA(n: NiveauTag) { if (n.ordinal > niveau.ordinal) niveau = n }

        var lectureMauvaise = false
        if (!lecture.essentielsLus) {
            lectureMauvaise = true
            raisons += "Infos essentielles illisibles (secteurs 0 et 1) apres ${lecture.nbPasses} passe(s)"
        } else {
            val nonLus = lecture.statutSecteurs.filter { !it.lu }.map { it.secteur }
            if (lecture.nbIncidents >= SEUIL_INCIDENTS_MAUVAISE) {
                lectureMauvaise = true
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

        // Mauvaise lecture : limite au premier scan, defaillant seulement si elle se repete.
        if (lectureMauvaise) {
            monterA(if (ctx.scansMauvaisPrecedents >= 1) NiveauTag.DEFAILLANT else NiveauTag.LIMITE)
        }

        val incoherences = incoherences(info)
        if (incoherences.isNotEmpty()) {
            monterA(NiveauTag.DEFAILLANT)
            raisons += "Donnees incoherentes : ${incoherences.joinToString(", ")}"
        }

        val conseil = when (niveau) {
            NiveauTag.SAIN, NiveauTag.NON_EVALUABLE -> null
            NiveauTag.LIMITE ->
                if (lectureMauvaise)
                    "Lecture instable sur ce scan. $CONSEIL_POSITION Un tag n'est juge defaillant qu'apres deux mauvais scans de suite."
                else
                    "Lecture reussie mais instable : rescanne pour confirmer. Si l'AMS le refuse de temps en temps, remplace-le."
            NiveauTag.DEFAILLANT ->
                if (incoherences.isNotEmpty()) {
                    "Les donnees memorisees dans le tag sont invalides : l'AMS le refusera. A remplacer."
                } else {
                    val telephone = if (ctx.autresTagsSainsVus > 0)
                        "Ton telephone lit bien d'autres tags, donc ce tag est probablement en cause."
                    else
                        "Impossible de dire si c'est le tag ou le telephone : scanne un autre tag pour comparer."
                    "$telephone Mauvaise lecture confirmee sur ${ctx.scansMauvaisPrecedents + 1} scans de suite."
                }
        }
        return DiagnosticTag(niveau, raisons, conseil, lectureMauvaise)
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
    private val mauvaisScansParUid = mutableMapOf<String, Int>()

    fun evaluer(uid: String, lecture: ResultatLecture, info: BambuTagDecoder.InfoFilament): DiagnosticTag {
        val ctx = ContexteDiagnostic(
            autresTagsSainsVus = (uidsSains - uid).size,
            scansMauvaisPrecedents = mauvaisScansParUid[uid] ?: 0
        )
        val diagnostic = EvaluateurTag.evaluer(lecture, info, ctx)
        when {
            diagnostic.niveau == NiveauTag.SAIN -> { uidsSains += uid; mauvaisScansParUid.remove(uid) }
            diagnostic.lectureMauvaise -> mauvaisScansParUid[uid] = (mauvaisScansParUid[uid] ?: 0) + 1
        }
        return diagnostic
    }
}
