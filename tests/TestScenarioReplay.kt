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


fun main() {
    val cles = (0 until 16).map { s -> ByteArray(6) { (s * 7 + it).toByte() } }
    val sansPause: (Long) -> Unit = {}
    fun rapport(titre: String, t: FauxTag) {
        val r = LecteurTagRobuste.lire(t, cles, sansPause)
        val d = EvaluateurTag.evaluer(r, BambuTagDecoder.decoder(r.blocs))
        println("$titre -> ${r.nbSecteursLus}/16 secteurs, ${r.nbPasses} passe(s), ${r.nbIncidents} echec(s) | ${d.niveau.libelle} | ${d.raisons}")
    }
    rapport("Scenario #1 (mouvement)      ", FauxTag(cles, authRefus = mutableMapOf(0 to 1), maxReconnexions = 1))
    rapport("Scenario #2 (4 echecs rattrapes)", FauxTag(cles, lectureKo = mutableMapOf(9 to 1, 13 to 1, 17 to 1, 21 to 1)))
    rapport("Scenario #3 (parfait)        ", FauxTag(cles))
}
