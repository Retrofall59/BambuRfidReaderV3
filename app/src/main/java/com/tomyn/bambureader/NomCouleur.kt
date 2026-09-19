package com.tomyn.bambureader

import kotlin.math.sqrt

/**
 * Deux niveaux de recherche du nom de couleur :
 * 1. Table officielle Bambu : correspondance EXACTE sur le code hexadecimal. Certains codes
 *    hex sont partages par plusieurs gammes (ex: #000000 = "Black" en PLA Basic/ABS et
 *    "Charcoal" en PLA Matte) - dans ce cas, le nom de matiere deja detecte ailleurs (indice)
 *    sert a choisir la bonne entree plutot que d'afficher toutes les options a la fois.
 * 2. Si pas de correspondance exacte : couleur usuelle la plus proche, via la formule
 *    "redmean" (ponderee par canal, plus proche de la perception humaine qu'une simple
 *    distance euclidienne RVB brute - notamment pour bien distinguer marrons/gris fonces)
 */
object NomCouleur {

    private data class CouleurNommee(val nom: String, val r: Int, val g: Int, val b: Int)

    // Chaque code hex pointe vers une liste de (mot-cle de gamme, nom officiel).
    // Quand une seule gamme utilise ce code, la liste n'a qu'un element.
    private val tableOfficielle: Map<String, List<Pair<String, String>>> = mapOf(
        "FFFFFF" to listOf("Gradient" to "Arctic Whisper / Solar Breeze (degrade)", "PLA Basic" to "Jade White", "PLA Matte" to "Ivory White", "ABS" to "White", "TPU 90A" to "Frozen (bicolore)", "PLA Pure" to "Pure White", "PLA Silk" to "White", "PETG" to "White", "PLA Tough" to "White", "TPU for AMS" to "White", "TPU 95A HF" to "White"),
        "F7E6DE" to listOf("" to "Beige"),
        "D1D3D5" to listOf("" to "Light Gray"),
        "A6A9AA" to listOf("" to "Silver"),
        "8E9089" to listOf("PLA Basic" to "Gray", "PLA Sparkle" to "Slate Gray Sparkle"),
        "EC008C" to listOf("" to "Magenta"),
        "F55A74" to listOf("" to "Pink"),
        "F5547C" to listOf("" to "Hot Pink"),
        "FF6A13" to listOf("PLA Basic" to "Orange", "ABS" to "Orange"),
        "FF9016" to listOf("" to "Pumpkin Orange"),
        "E4BD68" to listOf("" to "Gold"),
        "FEC600" to listOf("" to "Sunflower Yellow"),
        "F4EE2A" to listOf("" to "Yellow"),
        "BECF00" to listOf("" to "Bright Green"),
        "00AE42" to listOf("PLA Basic" to "Bambu Green", "ABS" to "Bambu Green", "PETG" to "Green"),
        "3F8E43" to listOf("" to "Mistletoe Green"),
        "847D48" to listOf("" to "Bronze"),
        "6F5034" to listOf("PLA Basic" to "Cocoa Brown"),
        "9D432C" to listOf("" to "Brown"),
        "9D2235" to listOf("" to "Maroon Red"),
        "C12E1F" to listOf("" to "Red"),
        "00B1B7" to listOf("" to "Turquoise"),
        "0086D6" to listOf("PLA Basic" to "Cyan", "PETG" to "Navy Blue"),
        "0A2989" to listOf("" to "Blue"),
        "0056B8" to listOf("" to "Cobalt Blue"),
        "5E43B7" to listOf("" to "Purple"),
        "482960" to listOf("" to "Indigo Purple"),
        "5B6579" to listOf("" to "Blue Grey"),
        "545454" to listOf("" to "Dark Gray"),
        "000000" to listOf("Multi-Color" to "Phantom Blue / Velvet Eclipse (bicolore)", "PLA Basic" to "Black", "ABS" to "Black", "TPU 90A" to "Black", "PETG" to "Black", "PLA Matte" to "Charcoal", "PLA Pure" to "Absolute Black", "PLA Tough" to "Black", "TPU for AMS" to "Black"),
        // --- Gamme PLA Matte ---
        "CBC6B8" to listOf("PLA Matte" to "Bone White"),
        "E8DBB7" to listOf("PLA Matte" to "Desert Tan"),
        "D3B7A7" to listOf("PLA Matte" to "Latte Brown"),
        "AE835B" to listOf("PLA Matte" to "Caramel"),
        "B15533" to listOf("PLA Matte" to "Terracotta"),
        "7D6556" to listOf("PLA Matte" to "Dark Brown"),
        "4D3324" to listOf("PLA Matte" to "Dark Chocolate"),
        "AE96D4" to listOf("PLA Matte" to "Lilac Purple"),
        "E8AFCF" to listOf("PLA Matte" to "Sakura Pink"),
        "F99963" to listOf("PLA Matte" to "Mandarin Orange"),
        "F7D959" to listOf("PLA Matte" to "Lemon Yellow"),
        "950051" to listOf("PLA Matte" to "Plum"),
        "DE4343" to listOf("PLA Matte" to "Scarlet Red"),
        "BB3D43" to listOf("PLA Matte" to "Dark Red"),
        "68724D" to listOf("PLA Matte" to "Dark Green"),
        "61C680" to listOf("PLA Matte" to "Grass Green"),
        "C2E189" to listOf("PLA Matte" to "Apple Green"),
        "A3D8E1" to listOf("PLA Matte" to "Ice Blue"),
        "56B7E6" to listOf("PLA Matte" to "Sky Blue"),
        "0078BF" to listOf("PLA Matte" to "Marine Blue"),
        "042F56" to listOf("PLA Matte" to "Dark Blue"),
        "9B9EA0" to listOf("PLA Matte" to "Ash Gray"),
        "757575" to listOf("PLA Matte" to "Nardo Gray"),
        // --- Gamme ABS ---
        "789D4A" to listOf("ABS" to "Olive"),
        "489FDF" to listOf("ABS" to "Azure"),
        "0C2340" to listOf("ABS" to "Navy Blue"),
        "0A2CA5" to listOf("ABS" to "Blue"),
        "FFC72C" to listOf("ABS" to "Tangerine Yellow"),
        "D32941" to listOf("ABS" to "Red"),
        "AF1685" to listOf("ABS" to "Purple"),
        "87909A" to listOf("ABS" to "Silver"),
        // --- Gamme TPU 90A ---
        "FFFFEE" to listOf("TPU 90A" to "White"),
        "D6ABFF" to listOf("TPU 90A" to "Grape Jelly", "PETG" to "Translucent Purple"),
        "7EB4E1" to listOf("TPU 90A" to "Crystal Blue"),
        "5C4738" to listOf("TPU 90A" to "Cocoa Brown"),
        "9EA2A2" to listOf("TPU 90A" to "Quicksilver"),
        "F1AAA8" to listOf("TPU 90A" to "Blaze (bicolore)"),
        "D21B3C" to listOf("TPU 90A" to "Blaze (bicolore)"),
        "40B6E4" to listOf("TPU 90A" to "Frozen (bicolore)"),
        // --- Gamme PETG-CF ---
        "9F332A" to listOf("PETG" to "Brick Red"),
        "583061" to listOf("PETG" to "Violet Purple"),
        "324585" to listOf("PETG" to "Indigo Blue"),
        "16B08E" to listOf("PETG" to "Malachite Green"),
        "565656" to listOf("PETG" to "Titan Gray"),
        // --- Gamme PLA Pure ---
        "FFB673" to listOf("PLA Pure" to "Apricot"),
        "F7CED7" to listOf("PLA Pure" to "Milky Pink"),
        "A4DBE8" to listOf("PLA Pure" to "Baby Blue"),
        // --- Gamme PLA Silk+ ---
        "C8C8C8" to listOf("PLA Silk" to "Silver"),
        "F3CFB2" to listOf("PLA Silk" to "Champagne"),
        "F7ADA6" to listOf("PLA Silk" to "Pink"),
        "BA9594" to listOf("PLA Silk" to "Rose Gold"),
        "D02727" to listOf("PLA Silk" to "Candy Red"),
        "F4A925" to listOf("PLA Silk" to "Gold"),
        "96DCB9" to listOf("PLA Silk" to "Mint"),
        "018814" to listOf("PLA Silk" to "Candy Green"),
        "A8C6EE" to listOf("PLA Silk" to "Baby Blue"),
        "008BDA" to listOf("PLA Silk" to "Blue"),
        "8671CB" to listOf("PLA Silk" to "Purple"),
        "5F6367" to listOf("PLA Silk" to "Titan Gray"),
        // --- Gamme PLA Translucent ---
        "009FA1" to listOf("PLA Translucent" to "Teal"),
        "96D8AF" to listOf("PLA Translucent" to "Light Jade"),
        "0047BB" to listOf("Multi-Color" to "Midnight Blaze / Neon City (bicolore)", "PLA Translucent" to "Blue"),
        "F5DBAB" to listOf("PLA Translucent" to "Mellow Yellow"),
        "8344B0" to listOf("PLA Translucent" to "Purple"),
        "F5B6CD" to listOf("PLA Translucent" to "Cherry Pink"),
        "F74E02" to listOf("PLA Translucent" to "Orange"),
        "B8CDE9" to listOf("PLA Translucent" to "Ice Blue"),
        "B50011" to listOf("PLA Translucent" to "Red"),
        "B8ACD6" to listOf("PLA Translucent" to "Lavender"),
        // --- Gamme PETG Basic ---
        "D6001C" to listOf("PETG" to "Red"),
        "FF671F" to listOf("PETG" to "Orange"),
        "FCE300" to listOf("PETG" to "Yellow"),
        "001489" to listOf("PETG" to "Reflex Blue"),
        "688197" to listOf("PETG" to "Misty Blue"),
        "009639" to listOf("PETG" to "Green"),
        "034638" to listOf("PETG" to "Pine Green"),
        "4F2C1D" to listOf("PETG" to "Dark Brown"),
        "DBC8B6" to listOf("PETG" to "Dark Beige"),
        "7F7E83" to listOf("PETG" to "Gray"),
        // --- Gamme PETG Translucent ---
        "F9C1BD" to listOf("PETG" to "Translucent Pink"),
        "FF911A" to listOf("PETG" to "Translucent Orange"),
        "C9A381" to listOf("PETG" to "Translucent Brown"),
        "77EDD7" to listOf("PETG" to "Translucent Teal"),
        "61B0FF" to listOf("PETG" to "Translucent Light Blue"),
        "748C45" to listOf("PETG" to "Translucent Olive"),
        "8E8E8E" to listOf("PETG" to "Translucent Gray"),
        // --- Gamme PETG HF ---
        "F9DFB9" to listOf("PETG" to "Cream"),
        "FFD00B" to listOf("PETG" to "Yellow"),
        "F75403" to listOf("PETG" to "Orange"),
        "EB3A3A" to listOf("PETG" to "Red"),
        "6EE53C" to listOf("PETG" to "Lime Green"),
        "39541A" to listOf("PETG" to "Forest Green"),
        "1F79E5" to listOf("PETG" to "Lake Blue"),
        "002E96" to listOf("PETG" to "Blue"),
        "875718" to listOf("PETG" to "Peanut Brown"),
        "ADB1B2" to listOf("PETG" to "Gray"),
        "515151" to listOf("PETG" to "Dark Gray"),
        // --- Gamme PLA Tough+ ---
        "AFB1AE" to listOf("PLA Tough" to "Gray"),
        "959698" to listOf("PLA Tough" to "Silver"),
        "F4D53F" to listOf("PLA Tough" to "Yellow"),
        "009BD8" to listOf("PLA Tough" to "Cyan"),
        "DC3A27" to listOf("PLA Tough" to "Orange"),
        // --- Gamme PLA Basic Gradient (filaments bicolores, 2 codes hex chacun) ---
        "9CDBD9" to listOf("Gradient" to "Arctic Whisper (degrade)"),
        "54FF9B" to listOf("Gradient" to "Ocean to Meadow (degrade)"),
        "307FE2" to listOf("Gradient" to "Ocean to Meadow (degrade)"),
        "E7C1D5" to listOf("Gradient" to "Cotton Candy Cloud (degrade)"),
        "8EC9E9" to listOf("Gradient" to "Cotton Candy Cloud (degrade)"),
        "6FCAEF" to listOf("Gradient" to "Blueberry Bubblegum (degrade)"),
        "8573DD" to listOf("Gradient" to "Blueberry Bubblegum (degrade)"),
        "4EC939" to listOf("Gradient" to "Mint Lime (degrade)"),
        "B6FF43" to listOf("Gradient" to "Mint Lime (degrade)"),
        "E94B3C" to listOf("Gradient" to "Solar Breeze (degrade)"),
        "F78F77" to listOf("Gradient" to "Pink Citrus (degrade)"),
        "E4505A" to listOf("Gradient" to "Pink Citrus (degrade)"),
        "ED9558" to listOf("Gradient" to "Dusk Glare (degrade)"),
        "CE4406" to listOf("Gradient" to "Dusk Glare (degrade)"),
        // --- Gamme PLA Silk Multi-Color (filaments bicolores/multicolores) ---
        "720062" to listOf("Multi-Color" to "Mystic Magenta (bicolore)"),
        "3A913F" to listOf("Multi-Color" to "Mystic Magenta (bicolore)"),
        "00629B" to listOf("Multi-Color" to "Phantom Blue (bicolore)"),
        "A34342" to listOf("Multi-Color" to "Velvet Eclipse (bicolore)"),
        "7D1B49" to listOf("Multi-Color" to "Midnight Blaze (bicolore)"),
        "FF9425" to listOf("Multi-Color" to "Gilded Rose (bicolore)"),
        "FCA2BF" to listOf("Multi-Color" to "Gilded Rose (bicolore)"),
        "60A4E8" to listOf("Multi-Color" to "Blue Hawaii (bicolore)"),
        "4CE4A0" to listOf("Multi-Color" to "Blue Hawaii (bicolore)"),
        "BB22A3" to listOf("Multi-Color" to "Neon City (bicolore)"),
        "7F3696" to listOf("Multi-Color" to "Aurora Purple (bicolore)"),
        "006EC9" to listOf("Multi-Color" to "Aurora Purple (bicolore)"),
        "F772A4" to listOf("Multi-Color" to "South Beach (bicolore)"),
        "00918B" to listOf("Multi-Color" to "South Beach (bicolore)"),
        "EC984C" to listOf("Multi-Color" to "Dawn Radiance (degrade)"),
        "6CD4BC" to listOf("Multi-Color" to "Dawn Radiance (degrade)"),
        "A66EB9" to listOf("Multi-Color" to "Dawn Radiance (degrade)"),
        "D87694" to listOf("Multi-Color" to "Dawn Radiance (degrade)"),
        // --- Gamme PLA Wood ---
        "D6CCA3" to listOf("PLA Wood" to "White Oak"),
        "C98935" to listOf("PLA Wood" to "Ochre Yellow"),
        "995F11" to listOf("PLA Wood" to "Clay Brown"),
        "918669" to listOf("PLA Wood" to "Classic Birch"),
        "4C241C" to listOf("PLA Wood" to "Rosewood"),
        "4F3F24" to listOf("PLA Wood" to "Black Walnut"),
        // --- Gamme PLA Sparkle ---
        "CEA629" to listOf("PLA Sparkle" to "Classic Gold Sparkle"),
        "792B36" to listOf("PLA Sparkle" to "Crimson Red Sparkle"),
        "483D8B" to listOf("PLA Sparkle" to "Royal Purple Sparkle"),
        "3F5443" to listOf("PLA Sparkle" to "Alpine Green Sparkle"),
        "2D2B28" to listOf("PLA Sparkle" to "Onyx Black Sparkle"),
        // --- Gamme PLA Marble ---
        "F7F3F0" to listOf("PLA Marble" to "White Marble"),
        "AD4E38" to listOf("PLA Marble" to "Red Granite"),
        // --- Gamme PLA Metal ---
        "B39B84" to listOf("PLA Metal" to "Iridium Gold Metallic"),
        "AA6443" to listOf("PLA Metal" to "Copper Brown Metallic"),
        "1D7C6A" to listOf("PLA Metal" to "Oxide Green Metallic"),
        "39699E" to listOf("PLA Metal" to "Cobalt Blue Metallic"),
        "43403D" to listOf("PLA Metal" to "Iron Gray Metallic"),
        // --- Gamme PLA Galaxy ---
        "684A43" to listOf("PLA Galaxy" to "Brown, base marron a reflets dores"),
        "3B665E" to listOf("PLA Galaxy" to "Green, base verte a reflets dores"),
        "424379" to listOf("PLA Galaxy" to "Nebulae, base bleu profond a reflets verts"),
        "594177" to listOf("PLA Galaxy" to "Purple, base violette a reflets bleus"),
        // --- Gamme PLA Glow (couleur de jour, phosphorescent la nuit) ---
        "A1FFAC" to listOf("PLA Glow" to "Glow Green"),
        "F8FF80" to listOf("PLA Glow" to "Glow Yellow"),
        "F17B8F" to listOf("PLA Glow" to "Glow Pink"),
        "7AC0E9" to listOf("PLA Glow" to "Glow Blue"),
        "FF9D5B" to listOf("PLA Glow" to "Glow Orange"),
        // --- Gamme TPU for AMS ---
        "939393" to listOf("TPU for AMS" to "Gray"),
        "F9EF41" to listOf("TPU for AMS" to "Yellow"),
        "90FF1A" to listOf("TPU for AMS" to "Neon Green"),
        "ED0000" to listOf("TPU for AMS" to "Red"),
        "5898DD" to listOf("TPU for AMS" to "Blue"),
        // --- Gamme TPU 95A HF ---
        "898D8D" to listOf("TPU 95A HF" to "Gray"),
        "F3E600" to listOf("TPU 95A HF" to "Yellow"),
        "0072CE" to listOf("TPU 95A HF" to "Blue"),
        "C8102E" to listOf("TPU 95A HF" to "Red"),
        "101820" to listOf("TPU 95A HF" to "Black")
    )

    // Liste elargie pour l'approximation quand aucune correspondance exacte n'est trouvee
    private val couleursApprox = listOf(
        CouleurNommee("Blanc", 255, 255, 255),
        CouleurNommee("Noir", 0, 0, 0),
        CouleurNommee("Gris", 128, 128, 128),
        CouleurNommee("Gris clair", 200, 200, 200),
        CouleurNommee("Gris fonce", 64, 64, 64),
        CouleurNommee("Gris anthracite", 45, 45, 48),
        CouleurNommee("Gris Nardo", 117, 117, 117),
        CouleurNommee("Gris cendre", 155, 158, 160),
        CouleurNommee("Rouge", 220, 20, 20),
        CouleurNommee("Rouge fonce", 139, 0, 0),
        CouleurNommee("Rose", 255, 105, 180),
        CouleurNommee("Rose pale", 255, 182, 193),
        CouleurNommee("Orange", 255, 140, 0),
        CouleurNommee("Jaune", 255, 220, 0),
        CouleurNommee("Jaune pale", 255, 255, 150),
        CouleurNommee("Vert", 34, 139, 34),
        CouleurNommee("Vert clair", 144, 238, 144),
        CouleurNommee("Vert fonce", 0, 100, 0),
        CouleurNommee("Vert olive", 128, 128, 0),
        CouleurNommee("Cyan / turquoise", 0, 200, 200),
        CouleurNommee("Bleu", 30, 60, 200),
        CouleurNommee("Bleu clair", 135, 206, 235),
        CouleurNommee("Bleu marine", 0, 0, 128),
        CouleurNommee("Violet", 138, 43, 226),
        CouleurNommee("Mauve", 200, 150, 220),
        CouleurNommee("Marron", 139, 69, 19),
        CouleurNommee("Marron fonce", 92, 51, 23),
        CouleurNommee("Marron tres fonce", 61, 38, 20),
        CouleurNommee("Chocolat", 79, 46, 26),
        CouleurNommee("Cafe", 111, 78, 55),
        CouleurNommee("Terre de Sienne", 130, 78, 44),
        CouleurNommee("Noisette", 149, 105, 68),
        CouleurNommee("Chataigne", 100, 60, 40),
        CouleurNommee("Acajou", 128, 63, 45),
        CouleurNommee("Taupe", 105, 90, 80),
        CouleurNommee("Kaki / bronze fonce", 96, 84, 56),
        CouleurNommee("Beige", 222, 184, 135),
        CouleurNommee("Beige fonce", 180, 150, 110),
        CouleurNommee("Or / dore", 212, 175, 55),
        CouleurNommee("Argent / gris metal", 192, 192, 192),
        CouleurNommee("Bronze / cuivre", 184, 115, 51),
        CouleurNommee("Transparent / naturel", 240, 240, 235)
    )

    data class ResultatCouleur(val nom: String, val nomCourt: String, val estExact: Boolean)

    private fun distancePerceptuelle(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val rMoyen = (r1 + r2) / 2.0
        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        val poidsR = 2.0 + rMoyen / 256.0
        val poidsG = 4.0
        val poidsB = 2.0 + (255.0 - rMoyen) / 256.0
        return sqrt(poidsR * dr * dr + poidsG * dg * dg + poidsB * db * db)
    }

    /**
     * @param hexRGB les 6 caracteres hexadecimaux R,G,B (sans le # ni le canal alpha)
     * @param indiceMatiere texte deja detecte ailleurs (ex: nom du filament type "Bambu ABS")
     *        utilise pour choisir la bonne gamme quand un code hex est partage par plusieurs.
     *        Optionnel - si vide ou sans correspondance, toutes les gammes possibles sont listees.
     */
    // Traduction anglais -> francais des noms officiels Bambu (affichee entre parentheses)
    private val traductions = mapOf(
        "Jade White" to "Blanc jade",
        "Beige" to "Beige",
        "Light Gray" to "Gris clair",
        "Silver" to "Argent",
        "Gray" to "Gris",
        "Magenta" to "Magenta",
        "Pink" to "Rose",
        "Hot Pink" to "Rose vif",
        "Orange" to "Orange",
        "Pumpkin Orange" to "Orange citrouille",
        "Gold" to "Or",
        "Sunflower Yellow" to "Jaune tournesol",
        "Yellow" to "Jaune",
        "Bright Green" to "Vert vif",
        "Bambu Green" to "Vert Bambu",
        "Mistletoe Green" to "Vert gui",
        "Bronze" to "Bronze",
        "Cocoa Brown" to "Marron cacao",
        "Brown" to "Marron",
        "Maroon Red" to "Rouge bordeaux",
        "Red" to "Rouge",
        "Turquoise" to "Turquoise",
        "Cyan" to "Cyan",
        "Blue" to "Bleu",
        "Cobalt Blue" to "Bleu cobalt",
        "Purple" to "Violet",
        "Indigo Purple" to "Violet indigo",
        "Blue Grey" to "Gris bleute",
        "Dark Gray" to "Gris fonce",
        "Black" to "Noir",
        "Ivory White" to "Blanc ivoire",
        "White" to "Blanc",
        "Frozen (bicolore)" to "Givre (bicolore)",
        "Charcoal" to "Anthracite",
        "Absolute Black" to "Noir absolu",
        "Pure White" to "Blanc pur",
        "Bone White" to "Blanc os",
        "Desert Tan" to "Beige desert",
        "Latte Brown" to "Marron latte",
        "Caramel" to "Caramel",
        "Terracotta" to "Terre cuite",
        "Dark Brown" to "Marron fonce",
        "Dark Chocolate" to "Chocolat noir",
        "Lilac Purple" to "Violet lilas",
        "Sakura Pink" to "Rose sakura",
        "Mandarin Orange" to "Orange mandarine",
        "Lemon Yellow" to "Jaune citron",
        "Plum" to "Prune",
        "Scarlet Red" to "Rouge ecarlate",
        "Dark Red" to "Rouge fonce",
        "Dark Green" to "Vert fonce",
        "Grass Green" to "Vert herbe",
        "Apple Green" to "Vert pomme",
        "Ice Blue" to "Bleu glace",
        "Sky Blue" to "Bleu ciel",
        "Marine Blue" to "Bleu marine",
        "Dark Blue" to "Bleu fonce",
        "Ash Gray" to "Gris cendre",
        "Nardo Gray" to "Gris Nardo",
        "Olive" to "Olive",
        "Azure" to "Azur",
        "Navy Blue" to "Bleu marine fonce",
        "Tangerine Yellow" to "Jaune mandarine",
        "Grape Jelly" to "Confiture de raisin",
        "Crystal Blue" to "Bleu cristal",
        "Quicksilver" to "Vif-argent",
        "Blaze (bicolore)" to "Flamme (bicolore)",
        "Brick Red" to "Rouge brique",
        "Violet Purple" to "Violet",
        "Indigo Blue" to "Bleu indigo",
        "Malachite Green" to "Vert malachite",
        "Titan Gray" to "Gris titane",
        "Apricot" to "Abricot",
        "Milky Pink" to "Rose laiteux",
        "Baby Blue" to "Bleu layette",
        "Champagne" to "Champagne",
        "Rose Gold" to "Or rose",
        "Candy Red" to "Rouge bonbon",
        "Mint" to "Menthe",
        "Candy Green" to "Vert bonbon",
        "Teal" to "Sarcelle",
        "Light Jade" to "Jade clair",
        "Mellow Yellow" to "Jaune doux",
        "Cherry Pink" to "Rose cerise",
        "Lavender" to "Lavande",
        "Reflex Blue" to "Bleu reflex",
        "Misty Blue" to "Bleu brume",
        "Pine Green" to "Vert pin",
        "Dark Beige" to "Beige fonce",
        "Translucent Purple" to "Violet translucide",
        "Translucent Pink" to "Rose translucide",
        "Translucent Orange" to "Orange translucide",
        "Translucent Brown" to "Marron translucide",
        "Translucent Teal" to "Sarcelle translucide",
        "Translucent Light Blue" to "Bleu clair translucide",
        "Translucent Olive" to "Olive translucide",
        "Translucent Gray" to "Gris translucide",
        "Cream" to "Creme",
        "Lime Green" to "Vert citron vert",
        "Forest Green" to "Vert foret",
        "Lake Blue" to "Bleu lac",
        "Peanut Brown" to "Marron cacahuete",
        "White Oak" to "Chene blanc",
        "Ochre Yellow" to "Jaune ocre",
        "Clay Brown" to "Marron argile",
        "Classic Birch" to "Bouleau classique",
        "Rosewood" to "Bois de rose",
        "Black Walnut" to "Noyer noir",
        "Slate Gray Sparkle" to "Gris ardoise pailletee",
        "Classic Gold Sparkle" to "Or classique paillete",
        "Crimson Red Sparkle" to "Rouge cramoisi paillete",
        "Royal Purple Sparkle" to "Violet royal paillete",
        "Alpine Green Sparkle" to "Vert alpin paillete",
        "Onyx Black Sparkle" to "Noir onyx paillete",
        "White Marble" to "Marbre blanc",
        "Red Granite" to "Granit rouge",
        "Iridium Gold Metallic" to "Or iridium metallise",
        "Copper Brown Metallic" to "Marron cuivre metallise",
        "Oxide Green Metallic" to "Vert oxyde metallise",
        "Cobalt Blue Metallic" to "Bleu cobalt metallise",
        "Iron Gray Metallic" to "Gris fer metallise",
        "Glow Green" to "Vert phosphorescent",
        "Glow Yellow" to "Jaune phosphorescent",
        "Glow Pink" to "Rose phosphorescent",
        "Glow Blue" to "Bleu phosphorescent",
        "Glow Orange" to "Orange phosphorescent",
        "Neon Green" to "Vert neon"
    )

    private fun avecTraduction(nomAnglais: String): String {
        val traduction = traductions[nomAnglais]
        return if (traduction != null) "$nomAnglais ($traduction)" else nomAnglais
    }

    // Pour l'etiquette imprimable : juste le francais (ou l'anglais seul si pas de traduction connue)
    private fun traductionSeule(nomAnglais: String): String {
        return traductions[nomAnglais] ?: nomAnglais
    }

    // Reference produit/catalogue Bambu (le "10101" affiche entre parentheses sur leur site).
    // ATTENTION : ce numero n'est PAS encode sur le tag RFID, c'est une reference commerciale
    // deduite ici a partir du code hex. Verifie et complet uniquement pour la gamme PLA Basic
    // pour l'instant - a completer au fur et a mesure pour les autres gammes si besoin.
    private val referencesProduitPlaBasic = mapOf(
        "FFFFFF" to "10100",
        "000000" to "10101",
        "A6A9AA" to "10102",
        "8E9089" to "10103",
        "D1D3D5" to "10104",
        "545454" to "10105",
        "C12E1F" to "10200",
        "F7E6DE" to "10201",
        "EC008C" to "10202",
        "F55A74" to "10203",
        "F5547C" to "10204",
        "9D2235" to "10205",
        "FF6A13" to "10300",
        "FF9016" to "10301",
        "F4EE2A" to "10400",
        "E4BD68" to "10401",
        "FEC600" to "10402",
        "00AE42" to "10501",
        "3F8E43" to "10502",
        "BECF00" to "10503",
        "0A2989" to "10601",
        "5B6579" to "10602",
        "0086D6" to "10603",
        "0056B8" to "10604",
        "00B1B7" to "10605",
        "5E43B7" to "10700",
        "482960" to "10701",
        "9D432C" to "10800",
        "847D48" to "10801",
        "6F5034" to "10802"
    )

    // Reference produit PLA Matte - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaMatte = mapOf(
        "FFFFFF" to "11100",
        "000000" to "11101",
        "9B9EA0" to "11102",
        "CBC6B8" to "11103",
        "757575" to "11104",
        "DE4343" to "11200",
        "E8AFCF" to "11201",
        "BB3D43" to "11202",
        "B15533" to "11203",
        "F99963" to "11300",
        "F7D959" to "11400",
        "E8DBB7" to "11401",
        "61C680" to "11500",
        "68724D" to "11501",
        "C2E189" to "11502",
        "0078BF" to "11600",
        "A3D8E1" to "11601",
        "042F56" to "11602",
        "56B7E6" to "11603",
        "AE96D4" to "11700",
        "D3B7A7" to "11800",
        "7D6556" to "11801",
        "4D3324" to "11802",
        "AE835B" to "11803"
    )

    // Reference produit ABS (plage 40xxx, distincte de l'ABS-GF en 41xxx) - convergence verifiee
    // sur plusieurs revendeurs independants. Incomplet (Red et Purple non confirmes pour l'instant).
    private val referencesProduitAbs = mapOf(
        "FFFFFF" to "40100",
        "000000" to "40101",
        "87909A" to "40102",
        "FF6A13" to "40300",
        "FFC72C" to "40402",
        "00AE42" to "40500",
        "789D4A" to "40502",
        "0A2CA5" to "40600",
        "489FDF" to "40601",
        "0C2340" to "40602"
    )

    // Reference produit PETG Basic - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPetgBasic = mapOf(
        "000000" to "30105",
        "FFFFFF" to "30106",
        "7F7E83" to "30107",
        "688197" to "30108",
        "D6001C" to "30201",
        "FF671F" to "30301",
        "FCE300" to "30402",
        "DBC8B6" to "30403",
        "009639" to "30502",
        "034638" to "30503",
        "001489" to "30603",
        "0086D6" to "30604",
        "4F2C1D" to "30800"
    )

    // Reference produit TPU 90A - convergence verifiee, SAUF le noir (deux codes conflictuels
    // trouves - 51107 et 51103 - probablement TPU 85A vs 90A meles sur la meme fiche produit,
    // volontairement omis par prudence)
    private val referencesProduitTpu90a = mapOf(
        "FFFFEE" to "51105",
        "9EA2A2" to "51106",
        "D6ABFF" to "51700",
        "7EB4E1" to "51601",
        "5C4738" to "51800",
        "FFFFFF" to "51900",
        "F1AAA8" to "51901"
    )

    // Reference produit PETG HF - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPetgHf = mapOf(
        "FFFFFF" to "33100",
        "ADB1B2" to "33101",
        "000000" to "33102",
        "515151" to "33103",
        "EB3A3A" to "33200",
        "F75403" to "33300",
        "FFD00B" to "33400",
        "F9DFB9" to "33401",
        "00AE42" to "33500",
        "6EE53C" to "33501",
        "39541A" to "33502",
        "002E96" to "33600",
        "1F79E5" to "33601",
        "875718" to "33801"
    )

    // Reference produit PETG-CF - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPetgCf = mapOf(
        "000000" to "31100",
        "565656" to "31101",
        "9F332A" to "31200",
        "16B08E" to "31500",
        "324585" to "31600",
        "583061" to "31700"
    )

    // Reference produit PLA Pure - convergence verifiee (3D Universe, plusieurs fiches)
    private val referencesProduitPlaPure = mapOf(
        "FFFFFF" to "17100",
        "000000" to "17101",
        "F7CED7" to "17200",
        "FFB673" to "17300",
        "A4DBE8" to "17600"
    )

    // Reference produit PLA Silk+ - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaSilk = mapOf(
        "5F6367" to "13108",
        "C8C8C8" to "13109",
        "FFFFFF" to "13110",
        "D02727" to "13205",
        "BA9594" to "13206",
        "F7ADA6" to "13207",
        "F3CFB2" to "13404",
        "F4A925" to "13405",
        "018814" to "13506",
        "96DCB9" to "13507",
        "A8C6EE" to "13603",
        "008BDA" to "13604",
        "8671CB" to "13702"
    )

    // Reference produit PLA Translucent - convergence verifiee sur plusieurs revendeurs
    private val referencesProduitPlaTranslucent = mapOf(
        "B50011" to "13210",
        "F5B6CD" to "13211",
        "F74E02" to "13301",
        "F5DBAB" to "13410",
        "96D8AF" to "13510",
        "B8CDE9" to "13610",
        "0047BB" to "13611",
        "009FA1" to "13612",
        "8344B0" to "13710",
        "B8ACD6" to "13711"
    )

    // Reference produit PETG Translucent - convergence verifiee sur plusieurs revendeurs
    private val referencesProduitPetgTranslucent = mapOf(
        "8E8E8E" to "32100",
        "F9C1BD" to "32200",
        "FF911A" to "32300",
        "748C45" to "32500",
        "77EDD7" to "32501",
        "61B0FF" to "32600",
        "D6ABFF" to "32700",
        "C9A381" to "32800"
    )

    // Reference produit PLA Wood - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaWood = mapOf(
        "D6CCA3" to "13106",
        "4F3F24" to "13107",
        "4C241C" to "13204",
        "C98935" to "13403",
        "918669" to "13505",
        "995F11" to "13801"
    )

    // Reference produit PLA Sparkle - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaSparkle = mapOf(
        "2D2B28" to "13101",
        "8E9089" to "13102",
        "792B36" to "13200",
        "CEA629" to "13402",
        "3F5443" to "13501",
        "483D8B" to "13700"
    )

    // Reference produit PLA Marble
    private val referencesProduitPlaMarble = mapOf(
        "F7F3F0" to "13103",
        "AD4E38" to "13201"
    )

    // Reference produit PLA Metal - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaMetal = mapOf(
        "43403D" to "13100",
        "B39B84" to "13400",
        "1D7C6A" to "13500",
        "39699E" to "13600",
        "AA6443" to "13800"
    )

    // Reference produit PLA Galaxy - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaGalaxy = mapOf(
        "684A43" to "13203",
        "3B665E" to "13503",
        "424379" to "13504",
        "594177" to "13602"
    )

    // Reference produit PLA Glow - convergence verifiee sur plusieurs revendeurs independants
    private val referencesProduitPlaGlow = mapOf(
        "F17B8F" to "15200",
        "FF9D5B" to "15300",
        "F8FF80" to "15400",
        "A1FFAC" to "15500",
        "7AC0E9" to "15600"
    )

    // Reference produit PLA Basic Gradient - convergence verifiee. Le blanc pur (#FFFFFF) est
    // partage par Arctic Whisper ET Solar Breeze donc volontairement omis ici (ambigu).
    private val referencesProduitPlaGradient = mapOf(
        "9CDBD9" to "10900",
        "E94B3C" to "10901",
        "54FF9B" to "10902",
        "307FE2" to "10902",
        "F78F77" to "10903",
        "E4505A" to "10903",
        "4EC939" to "10904",
        "B6FF43" to "10904",
        "6FCAEF" to "10905",
        "8573DD" to "10905",
        "ED9558" to "10906",
        "CE4406" to "10906",
        "E7C1D5" to "10907",
        "8EC9E9" to "10907"
    )

    // Reference produit PLA Silk Multi-Color - convergence verifiee. 0047BB (partage entre
    // Midnight Blaze et Neon City) et 000000 (partage entre Velvet Eclipse, Phantom Blue et
    // d'autres gammes) volontairement omis ici (ambigus).
    private val referencesProduitPlaSilkMulti = mapOf(
        "FF9425" to "13901",
        "FCA2BF" to "13901",
        "7D1B49" to "13902",
        "BB22A3" to "13903",
        "60A4E8" to "13904",
        "4CE4A0" to "13904",
        "A34342" to "13905",
        "F772A4" to "13906",
        "00918B" to "13906",
        "7F3696" to "13909",
        "006EC9" to "13909",
        "EC984C" to "13912",
        "6CD4BC" to "13912",
        "A66EB9" to "13912",
        "D87694" to "13912",
        "720062" to "13913",
        "3A913F" to "13913",
        "00629B" to "13916"
    )

    // Reference produit TPU 95A HF - convergence verifiee sur de nombreux revendeurs
    private val referencesProduitTpu95aHf = mapOf(
        "101820" to "51100",
        "898D8D" to "51101",
        "FFFFFF" to "51102",
        "C8102E" to "51200",
        "F3E600" to "51400",
        "0072CE" to "51600"
    )

    /**
     * Reference produit Bambu (20 gammes verifiees pour l'instant). Retourne null si inconnue
     * ou si la matiere detectee ne correspond a aucune de ces gammes verifiees.
     */
    fun trouverReferenceProduit(hexRGB: String, indiceMatiere: String): String? {
        val hex = hexRGB.uppercase()
        val indice = indiceMatiere.uppercase()
        if (indice.contains("PLA MATTE")) return referencesProduitPlaMatte[hex]
        if (indice.contains("ABS")) return referencesProduitAbs[hex]
        if (indice.contains("PLA PURE")) return referencesProduitPlaPure[hex]
        if (indice.contains("PLA SPARKLE")) return referencesProduitPlaSparkle[hex]
        if (indice.contains("PLA MARBLE")) return referencesProduitPlaMarble[hex]
        if (indice.contains("PLA METAL")) return referencesProduitPlaMetal[hex]
        if (indice.contains("PLA GALAXY")) return referencesProduitPlaGalaxy[hex]
        if (indice.contains("PLA GLOW")) return referencesProduitPlaGlow[hex]
        if (indice.contains("GRADIENT")) return referencesProduitPlaGradient[hex]
        if (indice.contains("MULTI-COLOR") || indice.contains("MULTI COLOR")) return referencesProduitPlaSilkMulti[hex]
        if (indice.contains("PLA SILK")) return referencesProduitPlaSilk[hex]
        if (indice.contains("PLA TRANSLUCENT")) return referencesProduitPlaTranslucent[hex]
        if (indice.contains("PLA WOOD")) return referencesProduitPlaWood[hex]
        if (indice.contains("PLA BASIC")) return referencesProduitPlaBasic[hex]
        if (indice.contains("PETG BASIC")) return referencesProduitPetgBasic[hex]
        if (indice.contains("TPU 90A")) return referencesProduitTpu90a[hex]
        if (indice.contains("TPU 95A HF")) return referencesProduitTpu95aHf[hex]
        if (indice.contains("PETG HF")) return referencesProduitPetgHf[hex]
        if (indice.contains("PETG-CF") || indice.contains("PETG CF")) return referencesProduitPetgCf[hex]
        if (indice.contains("PETG TRANSLUCENT")) return referencesProduitPetgTranslucent[hex]
        return null
    }

    fun trouverNom(hexRGB: String, indiceMatiere: String = ""): ResultatCouleur {
        val hexNormalise = hexRGB.uppercase()
        val entrees = tableOfficielle[hexNormalise]
        if (entrees != null) {
            if (entrees.size == 1) {
                return ResultatCouleur(
                    "${avecTraduction(entrees[0].second)} (Bambu, officiel)",
                    "${traductionSeule(entrees[0].second)} (Bambu, officiel)",
                    true
                )
            }
            val indiceNormalise = indiceMatiere.uppercase()
            val correspondance = entrees.firstOrNull { (ligne, _) ->
                ligne.isNotEmpty() && indiceNormalise.contains(ligne.uppercase())
            }
            if (correspondance != null) {
                return ResultatCouleur(
                    "${avecTraduction(correspondance.second)} (Bambu ${correspondance.first}, officiel)",
                    "${traductionSeule(correspondance.second)} (Bambu ${correspondance.first}, officiel)",
                    true
                )
            }
            // Aucun indice de matiere ne permet de trancher : on liste toutes les options connues
            val toutesLesOptions = entrees.joinToString(" / ") { (ligne, nom) ->
                val nomTraduit = avecTraduction(nom)
                if (ligne.isEmpty()) nomTraduit else "$nomTraduit ($ligne)"
            }
            val toutesLesOptionsCourtes = entrees.joinToString(" / ") { (ligne, nom) ->
                val nomTraduit = traductionSeule(nom)
                if (ligne.isEmpty()) nomTraduit else "$nomTraduit ($ligne)"
            }
            return ResultatCouleur("$toutesLesOptions - Bambu, officiel", "$toutesLesOptionsCourtes - Bambu, officiel", true)
        }

        val r = hexNormalise.substring(0, 2).toInt(16)
        val g = hexNormalise.substring(2, 4).toInt(16)
        val b = hexNormalise.substring(4, 6).toInt(16)

        var meilleurNom = "Inconnu"
        var meilleureDistance = Double.MAX_VALUE
        for (c in couleursApprox) {
            val distance = distancePerceptuelle(r, g, b, c.r, c.g, c.b)
            if (distance < meilleureDistance) {
                meilleureDistance = distance
                meilleurNom = c.nom
            }
        }
        return ResultatCouleur(meilleurNom, meilleurNom, false)
    }

    data class CorrespondanceProche(
        val nom: String,
        val ligne: String,
        val hexOfficiel: String,
        val estExact: Boolean
    )

    /**
     * Recherche inverse : a partir d'un code hexa saisi manuellement (filament tiers par
     * exemple), trouve les couleurs officielles Bambu les plus proches, toutes gammes
     * confondues. Utile pour faire correspondre un tag Bambu salvage a la couleur reelle
     * d'un filament d'une autre marque.
     *
     * @param hexRGB les 6 caracteres hexadecimaux R,G,B (sans le #)
     * @param nombreResultats combien de correspondances proches retourner (les plus proches
     *        en premier)
     */
    fun trouverCorrespondancesProches(hexRGB: String, nombreResultats: Int = 5): List<CorrespondanceProche> {
        val hexNormalise = hexRGB.uppercase()
        val r = hexNormalise.substring(0, 2).toInt(16)
        val g = hexNormalise.substring(2, 4).toInt(16)
        val b = hexNormalise.substring(4, 6).toInt(16)

        val toutesLesEntrees = mutableListOf<Triple<String, String, String>>() // (nom, ligne, hexOfficiel)
        for ((hex, entrees) in tableOfficielle) {
            for ((ligne, nom) in entrees) {
                val ligneAffichee = if (ligne.isEmpty()) "Bambu" else ligne
                toutesLesEntrees.add(Triple(avecTraduction(nom), ligneAffichee, hex))
            }
        }

        // Correspondance exacte : on la met en premier si elle existe, peu importe la gamme
        val exactes = toutesLesEntrees.filter { it.third == hexNormalise }
        val resultatsExacts = exactes.map { (nom, ligne, hex) -> CorrespondanceProche(nom, ligne, hex, true) }

        // Le reste, trie par distance perceptuelle croissante
        val approximatifs = toutesLesEntrees
            .filter { it.third != hexNormalise }
            .map { (nom, ligne, hex) ->
                val rOff = hex.substring(0, 2).toInt(16)
                val gOff = hex.substring(2, 4).toInt(16)
                val bOff = hex.substring(4, 6).toInt(16)
                val distance = distancePerceptuelle(r, g, b, rOff, gOff, bOff)
                Pair(distance, CorrespondanceProche(nom, ligne, hex, false))
            }
            .sortedBy { it.first }
            .map { it.second }

        return (resultatsExacts + approximatifs).take(nombreResultats)
    }
}
