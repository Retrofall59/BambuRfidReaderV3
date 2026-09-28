import com.tomyn.bambureader.*
import java.io.IOException

/** Faux tag 1K qui reproduit le comportement MIFARE : apres un echec, plus rien ne marche tant qu'on ne reconnecte pas. */
class FauxTag(
    val cleOk: List<ByteArray>,
    // echecs d'auth "transitoires" : secteur -> nb de fois ou l'auth sera refusee avant de marcher
    val authRefus: MutableMap<Int, Int> = mutableMapOf(),
    // echecs de lecture transitoires : bloc -> nb de fois ou la lecture leve une IOException
    val lectureKo: MutableMap<Int, Int> = mutableMapOf(),
    // le tag "disparait" apres N operations reussies (Int.MAX_VALUE = jamais)
    var perduApres: Int = Int.MAX_VALUE,
    var reconnexionPossible: Boolean = true,
    val maxReconnexions: Int = Int.MAX_VALUE,
    val toutAuthRefusee: Boolean = false
) : AccesTag {
    override val nbSecteurs = 16
    override fun premierBloc(secteur: Int) = secteur * 4
    override fun nbBlocs(secteur: Int) = 4
    var veille = false
    var authOk = -1
    var ops = 0
    var nbReconnexions = 0
    val tentativesAuth = mutableListOf<Int>()

    private fun op() {
        ops++
        if (ops > perduApres) throw IOException("Tag was lost")
    }

    override fun reconnecter(): Boolean {
        nbReconnexions++
        if (!reconnexionPossible || ops > perduApres || nbReconnexions > maxReconnexions) return false
        veille = false; authOk = -1
        return true
    }

    override fun authentifier(secteur: Int, cle: ByteArray): Boolean {
        op()
        if (veille) throw IOException("tag en veille")
        tentativesAuth += secteur
        val refus = authRefus[secteur] ?: 0
        if (toutAuthRefusee || refus > 0 || !cle.contentEquals(cleOk[secteur])) {
            if (refus > 0) authRefus[secteur] = refus - 1
            veille = true   // echec d'auth => le tag passe en veille
            return false
        }
        authOk = secteur
        return true
    }

    override fun lireBloc(numBloc: Int): ByteArray {
        op()
        if (veille) throw IOException("tag en veille")
        if (authOk != numBloc / 4) throw IOException("pas authentifie")
        val ko = lectureKo[numBloc] ?: 0
        if (ko > 0) { lectureKo[numBloc] = ko - 1; veille = true; throw IOException("Transceive failed") }
        return donnees(numBloc)
    }

    override fun fermer() {}

    var tempBuseMax = 230
    var tempBuseMin = 190
    fun donnees(n: Int): ByteArray {
        val b = ByteArray(16)
        fun texte(t: String, off: Int) { t.toByteArray(Charsets.US_ASCII).copyInto(b, off) }
        fun le16(v: Int, off: Int) { b[off] = (v and 0xFF).toByte(); b[off + 1] = ((v shr 8) and 0xFF).toByte() }
        when (n) {
            1 -> { texte("A00-K0", 0); texte("GFA00", 8) }
            2 -> texte("PLA", 0)
            4 -> texte("PLA Basic", 0)
            5 -> { b[0] = 0xFF.toByte(); b[3] = 0xFF.toByte(); le16(1000, 4) }
            6 -> { le16(55, 0); le16(8, 2); le16(60, 6); le16(tempBuseMax, 8); le16(tempBuseMin, 10) }
            else -> for (i in 0 until 16) b[i] = (n + i).toByte()
        }
        return b
    }
}

var echecs = 0
fun check(nom: String, cond: Boolean, detail: String = "") {
    println((if (cond) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
    if (!cond) echecs++
}

fun main() {
    val cles = (0 until 16).map { s -> ByteArray(6) { (s * 7 + it).toByte() } }
    val sansPause: (Long) -> Unit = {}

    println("1. Tag parfait")
    var t = FauxTag(cles)
    var r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("16 secteurs lus", r.nbSecteursLus == 16)
    check("essentiels lus", r.essentielsLus)
    check("1 seule passe", r.nbPasses == 1, "passes=${r.nbPasses}")
    check("64 blocs", r.blocs.size == 64)

    println("2. Auth du secteur 1 refusee une fois (glitch)")
    t = FauxTag(cles, authRefus = mutableMapOf(1 to 1))
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("recupere tout", r.nbSecteursLus == 16 && r.essentielsLus, "passes=${r.nbPasses} lus=${r.nbSecteursLus}")

    println("3. Lecture du bloc 5 echoue 2 fois (IOException) puis marche")
    t = FauxTag(cles, lectureKo = mutableMapOf(5 to 2))
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("essentiels recuperes", r.essentielsLus, "passes=${r.nbPasses}")
    check("tout lu", r.nbSecteursLus == 16)

    println("4. Erreur de lecture au secteur 7 (non essentiel), une fois")
    t = FauxTag(cles, lectureKo = mutableMapOf(29 to 1))
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("essentiels OK des la passe 1", r.essentielsLus)
    check("secteur 7 rattrape", r.nbSecteursLus == 16, "passes=${r.nbPasses}")

    println("5. Le tag disparait apres le secteur 1 (8 ops : 2 auth + 6 lectures... ) ")
    t = FauxTag(cles, perduApres = 10, reconnexionPossible = true)
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("essentiels lus malgre tout", r.essentielsLus, "blocs=${r.blocs.keys.sorted()}")
    check("lecture partielle detectee", r.nbSecteursLus in 2..15, "lus=${r.nbSecteursLus}")
    check("pas de boucle infinie", t.ops < 200, "ops=${t.ops}")

    println("6. Cles fausses (tag non Bambu) : on n'insiste pas")
    val tag6 = FauxTag(cles.map { ByteArray(6) }, toutAuthRefusee = true)
    r = LecteurTagRobuste.lire(tag6, cles, sansPause)
    check("rien lu", r.nbSecteursLus == 0 && !r.essentielsLus)
    check("borne : au plus 3 auth (secteur 0 uniquement)", tag6.tentativesAuth.size <= 3 && tag6.tentativesAuth.all { it == 0 }, "auth=${tag6.tentativesAuth}")

    println("7. Reconnexion impossible des le depart")
    t = FauxTag(cles, reconnexionPossible = false)
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("connexionImpossible", r.connexionImpossible && r.blocs.isEmpty())

    println("8. Le secteur 1 refuse 3 fois de suite (au-dela du budget de 3 passes)")
    t = FauxTag(cles, authRefus = mutableMapOf(1 to 3))
    r = LecteurTagRobuste.lire(t, cles, sansPause)
    check("essentiels NON lus (bloc 4-6 absents) -> l'appli doit signaler 'lecture partielle'", !r.essentielsLus, "passes=${r.nbPasses}")
    check("mais bloc 1 et 2 (secteur 0) sont conserves", 1 in r.blocs && 2 in r.blocs)

    // ======================= DIAGNOSTIC =======================
    fun diag(t: FauxTag, ctx: ContexteDiagnostic = ContexteDiagnostic()): Triple<DiagnosticTag, ResultatLecture, BambuTagDecoder.InfoFilament> {
        val res = LecteurTagRobuste.lire(t, cles, sansPause)
        val info = BambuTagDecoder.decoder(res.blocs)
        return Triple(EvaluateurTag.evaluer(res, info, ctx), res, info)
    }
    val essentielsKO = { FauxTag(cles, authRefus = mutableMapOf(1 to 3)) }          // secteur 1 refuse 3 fois, tag toujours present
    val quatreIncidents = { FauxTag(cles, lectureKo = mutableMapOf(9 to 1, 13 to 1, 17 to 1, 21 to 1)) }   // secteurs 2..5 en echec puis rattrapes

    println("D1. Tag parfait -> SAIN")
    val d1 = diag(FauxTag(cles))
    check("SAIN, 0 incident, aucune raison, pas de mauvaise lecture", d1.first.niveau == NiveauTag.SAIN && d1.second.nbIncidents == 0 && d1.first.raisons.isEmpty() && d1.first.conseil == null && !d1.first.lectureMauvaise)

    println("D2. Un incident rattrape -> LIMITE")
    val d2 = diag(FauxTag(cles, authRefus = mutableMapOf(1 to 1)))
    check("LIMITE (1 incident), pas une 'mauvaise lecture'", d2.first.niveau == NiveauTag.LIMITE && d2.second.nbIncidents == 1 && !d2.first.lectureMauvaise, "niveau=${d2.first.niveau}")

    println("D3. Seuil : 3 incidents -> LIMITE ; 4 incidents -> mauvaise lecture (LIMITE au 1er scan, DEFAILLANT si repete)")
    val d3 = diag(FauxTag(cles, lectureKo = mutableMapOf(9 to 1, 13 to 1, 17 to 1)))
    check("3 incidents -> LIMITE, pas mauvaise", d3.second.nbIncidents == 3 && d3.first.niveau == NiveauTag.LIMITE && !d3.first.lectureMauvaise)
    val d3b = diag(quatreIncidents())
    check("4 incidents, tout lu, 1er scan -> LIMITE (plus DEFAILLANT) et marque 'mauvaise lecture'", d3b.second.nbIncidents == 4 && d3b.second.nbSecteursLus == 16 && d3b.first.niveau == NiveauTag.LIMITE && d3b.first.lectureMauvaise, "niveau=${d3b.first.niveau}")
    // Depuis la 2.4 le seuil est reglable : le message annonce "apres N mauvais scans de suite" (N=2 par defaut)
    check("...conseil : poser le telephone sans bouger + 'apres 2 mauvais scans de suite'", d3b.first.conseil!!.contains("sans bouger") && d3b.first.conseil!!.contains("apres 2 mauvais scans de suite"), d3b.first.conseil!!)
    val d3c = diag(quatreIncidents(), ContexteDiagnostic(scansMauvaisPrecedents = 1))
    check("meme scan mais precede d'un mauvais scan -> DEFAILLANT", d3c.first.niveau == NiveauTag.DEFAILLANT, "niveau=${d3c.first.niveau}")

    println("D4. Infos essentielles illisibles (tag present, 3 passes) : LIMITE au 1er scan, DEFAILLANT si repete")
    val d4 = diag(essentielsKO())
    check("1er scan -> LIMITE (et non defaillant)", d4.first.niveau == NiveauTag.LIMITE && d4.first.lectureMauvaise && !d4.second.tagPerdu, "niveau=${d4.first.niveau} passes=${d4.second.nbPasses}")
    val d4b = diag(essentielsKO(), ContexteDiagnostic(scansMauvaisPrecedents = 1))
    check("2e mauvais scan de suite -> DEFAILLANT", d4b.first.niveau == NiveauTag.DEFAILLANT)
    check("...sans autre tag sain vu : dit qu'on ne peut pas savoir", d4b.first.conseil!!.contains("Impossible de dire"), d4b.first.conseil!!)
    val d4c = diag(essentielsKO(), ContexteDiagnostic(autresTagsSainsVus = 2, scansMauvaisPrecedents = 2))
    check("...avec tags sains vus : tag probablement en cause, confirme sur 3 scans", d4c.first.conseil!!.contains("probablement en cause") && d4c.first.conseil!!.contains("3 scans"), d4c.first.conseil!!)

    println("D5. Donnees incoherentes (buse 900 C) -> DEFAILLANT immediat, meme au 1er scan et sans incident")
    val t5 = FauxTag(cles); t5.tempBuseMax = 900
    val d5 = diag(t5)
    check("DEFAILLANT + raison + 0 incident", d5.first.niveau == NiveauTag.DEFAILLANT && d5.second.nbIncidents == 0 && d5.first.raisons.any { it.contains("incoherentes") }, d5.first.raisons.toString())
    check("conseil : a remplacer", d5.first.conseil!!.contains("A remplacer"))
    val t5b = FauxTag(cles); t5b.tempBuseMin = 250; t5b.tempBuseMax = 200
    check("buse min > max detecte", diag(t5b).first.raisons.any { it.contains("min > buse max") })

    println("D6. Donnees normales : aucune alerte")
    check("temperatures 190-230 / 60 / 55 : aucune incoherence", EvaluateurTag.incoherences(BambuTagDecoder.decoder(LecteurTagRobuste.lire(FauxTag(cles), cles, sansPause).blocs)).isEmpty())

    println("D7. Connexion impossible / cles refusees a chaque tentative -> NON_EVALUABLE")
    check("connexion impossible", diag(FauxTag(cles, reconnexionPossible = false)).first.niveau == NiveauTag.NON_EVALUABLE)
    val d7 = diag(FauxTag(cles, toutAuthRefusee = true))
    check("3 passes, tag present, cles refusees -> 'cles Bambu' (probablement pas un tag Bambu)", d7.first.niveau == NiveauTag.NON_EVALUABLE && d7.first.raisons[0].contains("cles Bambu") && d7.second.nbPasses == 3 && !d7.second.tagPerdu, d7.first.raisons.toString())

    println("D8. Tag perdu apres le secteur 1 : essentiels OK, secteurs non lus -> LIMITE")
    val d8 = diag(FauxTag(cles, perduApres = 10))
    check("LIMITE avec secteurs non lus listes", d8.first.niveau == NiveauTag.LIMITE && d8.first.raisons.any { it.startsWith("Secteurs non lus") }, "niveau=${d8.first.niveau} ${d8.first.raisons}")

    println("D9. RAPPORT #1 du testeur POCO : telephone en mouvement, tag detecte mais 1 seule authentification refusee puis tag perdu")
    val d9 = diag(FauxTag(cles, authRefus = mutableMapOf(0 to 1), maxReconnexions = 1))
    check("1 passe, 1 echec, tag perdu (comme dans le rapport)", d9.second.nbPasses == 1 && d9.second.nbIncidents == 1 && d9.second.tagPerdu && d9.second.nbSecteursLus == 0, "passes=${d9.second.nbPasses} inc=${d9.second.nbIncidents} perdu=${d9.second.tagPerdu}")
    check("verdict : NON_EVALUABLE 'le tag a disparu' (et surtout PAS 'cles Bambu refusees')", d9.first.niveau == NiveauTag.NON_EVALUABLE && d9.first.raisons[0].contains("disparu") && d9.first.raisons.none { it.contains("cles Bambu") }, d9.first.raisons.toString())
    check("...conseil : poser sans bouger", d9.first.conseil!!.contains("sans bouger"))
    check("...et ne compte pas comme mauvais scan", !d9.first.lectureMauvaise)

    println("D10. SessionDiagnostic rejoue les 3 rapports du testeur POCO (meme tag)")
    val session = SessionDiagnostic()
    fun scanSession(uid: String, t: FauxTag): DiagnosticTag {
        val rr = LecteurTagRobuste.lire(t, cles, sansPause)
        return session.evaluer(uid, rr, BambuTagDecoder.decoder(rr.blocs))
    }
    val r1 = scanSession("TAG-PETG", FauxTag(cles, authRefus = mutableMapOf(0 to 1), maxReconnexions = 1))
    check("#1 en mouvement -> Diagnostic impossible", r1.niveau == NiveauTag.NON_EVALUABLE)
    val r2 = scanSession("TAG-PETG", quatreIncidents())
    check("#2 partiellement en erreur (4 echecs, 16/16 lus) -> LIMITE, plus 'defaillant'", r2.niveau == NiveauTag.LIMITE && r2.raisons.any { it.contains("4 echecs") }, "niveau=${r2.niveau}")
    val r3 = scanSession("TAG-PETG", FauxTag(cles))
    check("#3 lecture complete -> SAIN", r3.niveau == NiveauTag.SAIN)
    println("   (avant correction, le rapport #2 affichait 'Tag defaillant' pour un tag qui s'est ensuite revele sain)")

    println("D11. Deux mauvais scans de suite -> DEFAILLANT ; un scan sain entre les deux remet le compteur a zero")
    val s2 = SessionDiagnostic()
    fun scan2(uid: String, t: FauxTag): DiagnosticTag { val rr = LecteurTagRobuste.lire(t, cles, sansPause); return s2.evaluer(uid, rr, BambuTagDecoder.decoder(rr.blocs)) }
    check("1er mauvais scan -> LIMITE", scan2("A", quatreIncidents()).niveau == NiveauTag.LIMITE)
    check("2e mauvais scan de suite -> DEFAILLANT", scan2("A", quatreIncidents()).niveau == NiveauTag.DEFAILLANT)
    val s3 = SessionDiagnostic()
    fun scan3(uid: String, t: FauxTag): DiagnosticTag { val rr = LecteurTagRobuste.lire(t, cles, sansPause); return s3.evaluer(uid, rr, BambuTagDecoder.decoder(rr.blocs)) }
    scan3("B", quatreIncidents()); scan3("B", FauxTag(cles))
    check("mauvais / sain / mauvais -> LIMITE (compteur remis a zero)", scan3("B", quatreIncidents()).niveau == NiveauTag.LIMITE)
    val s4 = SessionDiagnostic()
    fun scan4(uid: String, t: FauxTag): DiagnosticTag { val rr = LecteurTagRobuste.lire(t, cles, sansPause); return s4.evaluer(uid, rr, BambuTagDecoder.decoder(rr.blocs)) }
    check("deux tags differents mauvais chacun une fois -> LIMITE chacun", scan4("X", quatreIncidents()).niveau == NiveauTag.LIMITE && scan4("Y", quatreIncidents()).niveau == NiveauTag.LIMITE)

    println("P. Progression de lecture (indicateur 'Lecture 9/16')")
    fun suivre(t: FauxTag): Pair<List<Pair<Int, Int>>, ResultatLecture> {
        val evts = mutableListOf<Pair<Int, Int>>()
        val res = LecteurTagRobuste.lire(t, cles, sansPause, progression = { lus, total -> evts += lus to total })
        return evts to res
    }
    val (evP, resP) = suivre(FauxTag(cles))
    check("tag parfait : 0/16 puis 1/16 ... 16/16", evP == (0..16).map { it to 16 }, evP.take(3).toString() + "..." + evP.last())
    val (evQ, resQ) = suivre(quatreIncidents())
    check("avec echecs et 2e passe : ne recule jamais, finit a 16/16", evQ.zipWithNext().all { (a, b) -> b.first >= a.first } && evQ.last() == (16 to 16) && resQ.nbPasses == 2, evQ.map { it.first }.toString())
    val (evR, resR) = suivre(FauxTag(cles, perduApres = 10))
    check("tag perdu : la progression s'arrete au dernier secteur lu", evR.last().first == resR.nbSecteursLus && resR.nbSecteursLus in 2..15, "dernier=${evR.last()} lus=${resR.nbSecteursLus}")
    val (evS, _) = suivre(FauxTag(cles, reconnexionPossible = false))
    check("connexion impossible : un seul evenement 0/16", evS == listOf(0 to 16), evS.toString())
    check("sans callback (usage historique) : rien ne casse", LecteurTagRobuste.lire(FauxTag(cles), cles, sansPause).nbSecteursLus == 16)

    println(if (echecs == 0) "\nTOUS LES TESTS PASSENT" else "\n$echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
