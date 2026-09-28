
import com.tomyn.bambureader.*
import kotlin.math.abs

var echecs = 0
fun check(nom: String, cond: Boolean, detail: String = "") {
    println((if (cond) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
    if (!cond) echecs++
}

fun main() {
    println("1. Table Bambu extraite de NomCouleur : noms et references")
    val attenduBasic = mapOf("FFFFFF" to ("Jade White" to "10100"), "000000" to ("Black" to "10101"), "A6A9AA" to ("Silver" to "10102"), "8E9089" to ("Gray" to "10103"), "D1D3D5" to ("Light Gray" to "10104"), "545454" to ("Dark Gray" to "10105"), "C12E1F" to ("Red" to "10200"), "F7E6DE" to ("Beige" to "10201"), "EC008C" to ("Magenta" to "10202"), "F55A74" to ("Pink" to "10203"), "F5547C" to ("Hot Pink" to "10204"), "9D2235" to ("Maroon Red" to "10205"), "FF6A13" to ("Orange" to "10300"), "FF9016" to ("Pumpkin Orange" to "10301"), "F4EE2A" to ("Yellow" to "10400"), "E4BD68" to ("Gold" to "10401"), "FEC600" to ("Sunflower Yellow" to "10402"), "00AE42" to ("Bambu Green" to "10501"), "3F8E43" to ("Mistletoe Green" to "10502"), "BECF00" to ("Bright Green" to "10503"), "0A2989" to ("Blue" to "10601"), "5B6579" to ("Blue Grey" to "10602"), "0086D6" to ("Cyan" to "10603"), "0056B8" to ("Cobalt Blue" to "10604"), "00B1B7" to ("Turquoise" to "10605"), "5E43B7" to ("Purple" to "10700"), "482960" to ("Indigo Purple" to "10701"), "9D432C" to ("Brown" to "10800"), "847D48" to ("Bronze" to "10801"), "6F5034" to ("Cocoa Brown" to "10802"))
    val attenduMatte = mapOf("FFFFFF" to ("Ivory White" to "11100"), "000000" to ("Charcoal" to "11101"), "9B9EA0" to ("Ash Gray" to "11102"), "CBC6B8" to ("Bone White" to "11103"), "757575" to ("Nardo Gray" to "11104"), "DE4343" to ("Scarlet Red" to "11200"), "E8AFCF" to ("Sakura Pink" to "11201"), "BB3D43" to ("Dark Red" to "11202"), "B15533" to ("Terracotta" to "11203"), "F99963" to ("Mandarin Orange" to "11300"), "F7D959" to ("Lemon Yellow" to "11400"), "E8DBB7" to ("Desert Tan" to "11401"), "61C680" to ("Grass Green" to "11500"), "68724D" to ("Dark Green" to "11501"), "C2E189" to ("Apple Green" to "11502"), "0078BF" to ("Marine Blue" to "11600"), "A3D8E1" to ("Ice Blue" to "11601"), "042F56" to ("Dark Blue" to "11602"), "56B7E6" to ("Sky Blue" to "11603"), "AE96D4" to ("Lilac Purple" to "11700"), "D3B7A7" to ("Latte Brown" to "11800"), "7D6556" to ("Dark Brown" to "11801"), "4D3324" to ("Dark Chocolate" to "11802"), "AE835B" to ("Caramel" to "11803"))
    fun verifie(nomGamme: String, liste: List<CouleurBambu>, attendu: Map<String, Pair<String, String>>) {
        check("$nomGamme : ${attendu.size} couleurs", liste.size == attendu.size, "obtenu=${liste.size}")
        val ecarts = liste.filter { attendu[it.hex] != (it.nomEn to it.ref) }.map { "${it.hex}=${it.nomEn}/${it.ref} (attendu ${attendu[it.hex]})" }
        check("$nomGamme : noms et references identiques", ecarts.isEmpty(), ecarts.joinToString("; "))
    }
    verifie("PLA Basic", EquivalenceBambu.plaBasic, attenduBasic)
    verifie("PLA Matte", EquivalenceBambu.plaMatte, attenduMatte)

    println("2. CIEDE2000 : memes valeurs que l'implementation de reference (Python)")
    for ((a, b, v) in listOf(Triple("212721","000000",10.914243), Triple("FF6A13","F99963",10.974859), Triple("009639","00AE42",7.556835), Triple("6A6DCD","5E43B7",13.346390), Triple("FFFFFF","000000",100.000004), Triple("123456","123456",0.000000))) {
        val e = EquivalenceBambu.ecart(a, b)
        check("ecart $a / $b = ${"%.4f".format(v)}", abs(e - v) < 1e-4, "obtenu=${"%.6f".format(e)}")
    }

    println("3. Meilleure equivalence pour les 28 couleurs Anycubic : memes choix que la reference")
    val attB = mapOf("212721" to ("000000" to 10.9142), "EFF0F1" to ("FFFFFF" to 3.1406), "B1B3B3" to ("A6A9AA" to 2.9513), "CE3845" to ("C12E1F" to 9.2562), "F3E500" to ("F4EE2A" to 2.6232), "003594" to ("0A2989" to 3.9458), "009639" to ("3F8E43" to 4.4524), "6A6DCD" to ("5E43B7" to 13.3464), "FF7F32" to ("FF6A13" to 4.4468), "FF8DA1" to ("F55A74" to 11.078), "75787B" to ("8E9089" to 10.3573), "D4B996" to ("E4BD68" to 10.6979), "7C4D3A" to ("6F5034" to 7.3913), "927968" to ("8E9089" to 14.52), "8A8D8F" to ("8E9089" to 5.0447), "23A3C7" to ("00B1B7" to 11.7403), "CF4F80" to ("EC008C" to 7.124), "89A84F" to ("00AE42" to 13.0739), "5B618F" to ("5B6579" to 8.3441), "FFC196" to ("E4BD68" to 13.9441), "009CBD" to ("00B1B7" to 11.6364), "F1E9E0" to ("F7E6DE" to 4.2578), "FAD6C6" to ("F7E6DE" to 6.9879), "658946" to ("3F8E43" to 6.5766), "DAD9DB" to ("D1D3D5" to 2.1903), "EF3340" to ("F55A74" to 11.0052), "FFB81C" to ("FEC600" to 5.6045), "768692" to ("8E9089" to 11.2973))
    val attM = mapOf("212721" to ("000000" to 10.9142), "EFF0F1" to ("FFFFFF" to 3.1406), "B1B3B3" to ("9B9EA0" to 6.2632), "CE3845" to ("BB3D43" to 3.5591), "F3E500" to ("F7D959" to 7.1092), "003594" to ("042F56" to 8.9393), "009639" to ("61C680" to 16.1604), "6A6DCD" to ("0078BF" to 15.5911), "FF7F32" to ("F99963" to 6.9511), "FF8DA1" to ("E8AFCF" to 12.9969), "75787B" to ("757575" to 2.3133), "D4B996" to ("D3B7A7" to 7.7189), "7C4D3A" to ("7D6556" to 10.2648), "927968" to ("7D6556" to 7.9882), "8A8D8F" to ("9B9EA0" to 5.5702), "23A3C7" to ("56B7E6" to 7.9016), "CF4F80" to ("BB3D43" to 15.6055), "89A84F" to ("61C680" to 13.8947), "5B618F" to ("0078BF" to 17.4228), "FFC196" to ("F99963" to 10.364), "009CBD" to ("56B7E6" to 10.5771), "F1E9E0" to ("FFFFFF" to 6.5738), "FAD6C6" to ("D3B7A7" to 8.3693), "658946" to ("68724D" to 10.7728), "DAD9DB" to ("FFFFFF" to 8.1051), "EF3340" to ("DE4343" to 2.8561), "FFB81C" to ("F7D959" to 12.0881), "768692" to ("757575" to 9.7398))
    var diffs = 0
    for ((hex, att) in attB) {
        val l = EquivalenceBambu.chercher(null, hex)
        if (l.basic.couleur.hex != att.first || abs(l.basic.ecart - att.second) > 1e-3) { diffs++; println("   basic $hex : ${l.basic.couleur.hex} vs ${att.first}") }
        val m = attM[hex]!!
        if (l.matte.couleur.hex != m.first || abs(l.matte.ecart - m.second) > 1e-3) { diffs++; println("   matte $hex : ${l.matte.couleur.hex} vs ${m.first}") }
    }
    check("56 choix identiques", diffs == 0, "ecarts=$diffs")

    println("4. Niveaux")
    check("ecart 0 -> proche", EquivalenceBambu.niveau(0.0) == NiveauEquivalence.PROCHE)
    check("ecart 7.0 -> proche, 7.1 -> approximatif", EquivalenceBambu.niveau(7.0) == NiveauEquivalence.PROCHE && EquivalenceBambu.niveau(7.1) == NiveauEquivalence.APPROXIMATIF)
    check("ecart 13.0 -> approximatif, 13.1 -> aucun", EquivalenceBambu.niveau(13.0) == NiveauEquivalence.APPROXIMATIF && EquivalenceBambu.niveau(13.1) == NiveauEquivalence.AUCUN)
    val exact = EquivalenceBambu.chercher("Test", "C12E1F")
    check("code Bambu exact : ecart nul + niveau proche + bonne ref", exact.basic.ecart < 1e-9 && exact.basic.couleur.ref == "10200" && exact.basic.niveau == NiveauEquivalence.PROCHE)

    println("5. Export")
    val lignes = listOf(EquivalenceBambu.chercher("Red", "CE3845"), EquivalenceBambu.chercher(null, "009639"))
    val csv = EquivalenceBambu.versCsv(lignes)
    val nbCols = csv.trimEnd().lines().map { it.split(";").size }.toSet()
    check("CSV : meme nombre de colonnes sur toutes les lignes (12)", nbCols == setOf(12), nbCols.toString())
    println(csv)
    println(EquivalenceBambu.versTexte(lignes))

    println("6. Analyseur de codes")
    fun codes(t: String, photo: Boolean = false) = AnalyseurCodes.analyser(t, photo).codes.map { (it.nom ?: "-") + "|" + it.hex }
    check("Nom #HEX", codes("Red #CE3845") == listOf("Red|CE3845"))
    check("#HEX seul et HEX seul (avec chiffre)", codes("#ff6a13\n009639") == listOf("-|FF6A13", "-|009639"))
    check("HEX Nom / Nom;HEX / Nom: #HEX", codes("CE3845 Red\nBlue;003594\nTexture Grey: #75787B") == listOf("Red|CE3845", "Blue|003594", "Texture Grey|75787B"))
    check("mot 6 lettres A-F sans # n'est pas un code", codes("Facade\nDecade") == emptyList<String>())
    check("...mais avec # oui", codes("#FACADE") == listOf("-|FACADE"))
    check("doublons supprimes", codes("Red #CE3845\nRed #CE3845") == listOf("Red|CE3845"))
    check("titres et noms seuls ignores", codes("ANYCUBIC PLA Hex Code\nColor\nBlack\n#212721") == listOf("-|212721"))
    val mal = AnalyseurCodes.analyser("#12345\n#GGGGGG\nOk #ABCDEF")
    check("codes mal formes signales, bons gardes", mal.codes.map { it.hex } == listOf("ABCDEF") && mal.illisibles.size == 2, mal.illisibles.toString())

    println("7. Texte de type 'lecture de photo' (erreurs de reconnaissance typiques)")
    val ocr = "ANYCUBIC PLA Hex Code\nColor Hex Color Value Color Display\nBlack\n#212721\nWhite\n#EFFOF1\nGrey\n#B1B3B3\nYellow\n#F3E5OO\nBlue\n#0O3594\nGreen\n# 009639\nPurple\n#6A6DCD\nOrange\n#FF7F32\nTexture Silver #8A8D8F\nCyan #23A3C7 Magenta #CF4F80\nClear\n/\nInterstellar Violet\n#5B618F\nBlue Grey\n#76B692"
    val a = AnalyseurCodes.analyser(ocr, depuisPhoto = true)
    val attendu = listOf("212721","EFF0F1","B1B3B3","F3E500","003594","009639","6A6DCD","FF7F32","8A8D8F","23A3C7","CF4F80","5B618F")
    println("   lus: " + a.codes.map { it.hex } + "  illisibles: " + a.illisibles)
    check("O->0 corrige (EFFOF1, F3E5OO, 0O3594)", a.codes.map { it.hex }.take(5) == attendu.take(5))
    check("'# 009639' (espace apres #) accepte", "009639" in a.codes.map { it.hex })
    check("deux codes sur une ligne : gardes sans nom", a.codes.filter { it.hex == "23A3C7" || it.hex == "CF4F80" }.all { it.nom == null })
    check("nom sur la meme ligne conserve", a.codes.first { it.hex == "8A8D8F" }.nom == "Texture Silver")
    check("'#76B692' (8 lu B) reste un code valide : a verifier a l'oeil", "76B692" in a.codes.map { it.hex })
    check("mode saisie stricte : O n'est PAS corrige", AnalyseurCodes.analyser("#EFFOF1").codes.isEmpty())

    println("8. Codes coupes en deux par un espace (cas reel releve sur la capture : '#B1 B3B3')")
    check("#B1 B3B3 -> B1B3B3", AnalyseurCodes.analyser("#B1 B3B3", true).codes.map { it.hex } == listOf("B1B3B3"))
    check("#EF F0F1 et #2127 21 aussi", AnalyseurCodes.analyser("#EF F0F1\n#2127 21", true).codes.map { it.hex } == listOf("EFF0F1", "212721"))
    val nomApres = AnalyseurCodes.analyser("#212721 Black", true).codes
    check("espace suivi d'un nom : le nom n'est pas avale", nomApres.map { it.hex + "|" + it.nom } == listOf("212721|Black"))
    val nomApres2 = AnalyseurCodes.analyser("Grey #B1 B3B3 Silver", true).codes
    check("code coupe + nom des deux cotes", nomApres2.size == 1 && nomApres2[0].hex == "B1B3B3", nomApres2.toString())
    check("7 caracteres : pas un code", AnalyseurCodes.analyser("#1234567", true).codes.isEmpty())
    check("saisie manuelle inchangee", AnalyseurCodes.analyser("Red #CE3845").codes.map { it.nom + "|" + it.hex } == listOf("Red|CE3845"))

    println(if (echecs == 0) "\nTOUS LES TESTS PASSENT" else "\n$echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
