import com.tomyn.bambureader.*
fun main() {
    var echecs = 0
    fun check(n: String, c: Boolean, d: String = "") { println((if (c) "  OK   " else "  ECHEC") + " $n" + if (d.isNotEmpty()) "  [$d]" else ""); if (!c) echecs++ }
    val r = NomCouleur.trouverCorrespondancesProches("F57575", 5)
    r.forEach { println("   ${it.nom} | ${it.ligne} | #${it.hexOfficiel}") }
    check("meme classement que la capture (Glow Pink, Pink Citrus, Pink, Hot Pink, Dawn Radiance)",
        r.map { it.nom.substringBefore(" (") } == listOf("Glow Pink", "Pink Citrus", "Pink", "Hot Pink", "Dawn Radiance"))
    check("Pink et Hot Pink affichent PLA Basic (plus 'Bambu')", r[2].ligne == "PLA Basic" && r[3].ligne == "PLA Basic")
    check("les autres gammes gardent leur libelle", r[0].ligne == "PLA Glow" && r[1].ligne == "Gradient" && r[4].ligne == "Multi-Color")
    // aucune couleur des gammes Basic / Matte ne doit encore s'appeler simplement "Bambu"
    val toutes = (EquivalenceBambu.plaBasic + EquivalenceBambu.plaMatte)
    var restants = 0
    for (c in toutes) for (m in NomCouleur.trouverCorrespondancesProches(c.hex, 200)) if (m.hexOfficiel == c.hex && m.ligne == "Bambu") restants++
    check("aucune entree Basic/Matte etiquetee 'Bambu'", restants == 0, "restantes=$restants")
    val mat = NomCouleur.trouverCorrespondancesProches("DE4343", 3)
    check("code exact Matte (Scarlet Red) : etiquete PLA Matte", mat[0].estExact && mat[0].ligne == "PLA Matte", "${mat[0].nom} ${mat[0].ligne}")
    val bas = NomCouleur.trouverCorrespondancesProches("C12E1F", 3)
    check("code exact Basic (Red) : etiquete PLA Basic", bas[0].estExact && bas[0].ligne == "PLA Basic", "${bas[0].nom} ${bas[0].ligne}")
    println(if (echecs == 0) "\nTOUS LES TESTS PASSENT" else "\n$echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
