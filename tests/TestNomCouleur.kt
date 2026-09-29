import com.tomyn.bambureader.NomCouleur

/**
 * Test de NomCouleur.trouverNom() : desambiguisation par matiere quand plusieurs gammes Bambu
 * partagent le meme code hexadecimal (frequent pour le blanc pur #FFFFFF).
 *
 * Cas reel corrige (v2.10) : une bobine ASA blanc (#FFFFFF) etait absente de la table pour ce
 * hex, donc l'appli ne pouvait pas trancher et listait TOUTES les gammes connues concatenees
 * sur l'etiquette imprimee ("Arctic Whisper / Solar Breeze / Jade White / Ivory White / White /
 * Frozen / Pure White / White / White / White / White / White - Bambu, officiel"), illisible.
 * Signale par Zetif sur le forum.
 *
 *   kotlinc ../app/src/main/java/com/tomyn/bambureader/NomCouleur.kt TestNomCouleur.kt \
 *           -include-runtime -d test_nomcouleur.jar
 *   java -jar test_nomcouleur.jar
 */
var echecs = 0
fun check(nom: String, ok: Boolean, detail: String = "") {
    println((if (ok) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
    if (!ok) echecs++
}

fun main() {
    // Cas reel Zetif : ASA blanc doit donner "White", pas la liste de toutes les gammes
    val asa = NomCouleur.trouverNom("FFFFFF", "Bambu ASA", 255)
    check("ASA blanc : 'White (Blanc)', pas une liste", asa.nom == "White (Blanc) (Bambu ASA, officiel)", asa.nom)
    check("ASA blanc : exact (pas approximatif)", asa.estExact)
    check("ASA blanc : nomCourt pour l'etiquette", asa.nomCourt == "Blanc (Bambu ASA, officiel)", asa.nomCourt)

    // Non-regression : les autres gammes deja connues pour FFFFFF continuent de se desambiguiser
    check("ABS blanc", NomCouleur.trouverNom("FFFFFF", "Bambu ABS", 255).nom == "White (Blanc) (Bambu ABS, officiel)")
    check("PLA Basic blanc", NomCouleur.trouverNom("FFFFFF", "Bambu PLA Basic", 255).nom.startsWith("Jade White"))
    check("PLA Matte blanc", NomCouleur.trouverNom("FFFFFF", "Bambu PLA Matte", 255).nom.startsWith("Ivory White"))

    // Matiere totalement inconnue pour ce hex : le repli (liste complete) doit toujours fonctionner,
    // sans planter, et doit maintenant INCLURE l'ASA dans la liste
    val inconnu = NomCouleur.trouverNom("FFFFFF", "Bambu Matiere Du Futur", 255)
    check("matiere inconnue : repli sur la liste complete (contient ASA)", inconnu.nom.contains("ASA") && inconnu.nom.contains("Bambu, officiel"), inconnu.nom)

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
