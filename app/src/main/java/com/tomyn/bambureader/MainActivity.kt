package com.tomyn.bambureader

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var txtResultat: TextView
    private lateinit var txtStatut: TextView
    private lateinit var vuCouleur: View
    private lateinit var imgNfc: ImageView
    private lateinit var layoutLignesInfo: LinearLayout
    private var dernierDumpTexte: String = ""
    private var dernierResume: String = ""
    private var dernierNomFilament: String? = null

    data class EtiquetteEnAttente(
        val nomFilament: String,
        val nomCouleur: String?,
        val couleurArgb: Int?,
        val codeHexa: String?,
        val poidsGrammes: Int?,
        val tempBuseTexte: String?,
        val tempPlateau: Int?
    )
    private lateinit var btnImprimerEtiquette: Button
    private val filesAttenteEtiquettes = mutableListOf<EtiquetteEnAttente>()
    private var dernierNomCouleur: String? = null
    private var dernierNomCouleurEtiquette: String? = null
    private var dernierCodeHexa: String? = null
    private var dernierCouleurArgb: Int? = null
    private var dernierPoidsGrammes: Int? = null
    private var dernierTempBuseTexte: String? = null
    private var dernierTempPlateau: Int? = null
    private var animationPulse: ObjectAnimator? = null
    private val lectureEnCours = AtomicBoolean(false)
    private val sessionDiagnostic = SessionDiagnostic()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtResultat = findViewById(R.id.txtResultat)
        txtStatut = findViewById(R.id.txtStatut)
        vuCouleur = findViewById(R.id.vuCouleur)
        imgNfc = findViewById(R.id.imgNfc)
        layoutLignesInfo = findViewById(R.id.layoutLignesInfo)

        val btnExporter = findViewById<Button>(R.id.btnExporter)
        btnExporter.setOnClickListener { exporterDump() }

        btnImprimerEtiquette = findViewById(R.id.btnImprimerEtiquette)
        btnImprimerEtiquette.setOnClickListener { imprimerEtiquette() }
        mettreAJourBoutonImpression()

        val btnCopier = findViewById<Button>(R.id.btnCopier)
        btnCopier.setOnClickListener { copierResume() }

        val btnPartager = findViewById<Button>(R.id.btnPartager)
        btnPartager.setOnClickListener { partagerResume() }

        val btnHistorique = findViewById<Button>(R.id.btnHistorique)
        btnHistorique.setOnClickListener { afficherHistorique() }

        val btnChercherCouleur = findViewById<Button>(R.id.btnChercherCouleur)
        btnChercherCouleur.setOnClickListener { afficherRechercheCouleur() }

        demarrerPulseNfc()

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            txtStatut.text = "Ce telephone n'a pas de puce NFC."
            return
        }
        nfcAdapter = adapter
    }

    private fun demarrerPulseNfc() {
        val animateur = ObjectAnimator.ofFloat(imgNfc, "scaleX", 1f, 1.15f, 1f)
        animateur.duration = 1200
        animateur.repeatCount = ValueAnimator.INFINITE
        val animateurY = ObjectAnimator.ofFloat(imgNfc, "scaleY", 1f, 1.15f, 1f)
        animateurY.duration = 1200
        animateurY.repeatCount = ValueAnimator.INFINITE
        animateur.start()
        animateurY.start()
        animationPulse = animateur
    }

    private fun arreterPulseNfc() {
        animationPulse?.cancel()
        imgNfc.scaleX = 1f
        imgNfc.scaleY = 1f
    }

    override fun onResume() {
        super.onResume()
        if (!::nfcAdapter.isInitialized) return

        // Mode lecteur : plus fiable que le foreground dispatch pour du MIFARE Classic (pas de
        // verification NDEF parasite avant notre lecture), et le callback tourne deja hors thread UI.
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500)
        nfcAdapter.enableReaderMode(
            this,
            NfcAdapter.ReaderCallback { tag -> demarrerLecture(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableReaderMode(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            demarrerLecture(tag)
        }
    }

    private fun ajouterLigneInfo(icone: Int, texte: String) {
        val ligne = LinearLayout(this)
        ligne.orientation = LinearLayout.HORIZONTAL
        ligne.gravity = Gravity.CENTER_VERTICAL
        val paddingPx = (6 * resources.displayMetrics.density).toInt()
        ligne.setPadding(0, paddingPx, 0, paddingPx)

        val img = ImageView(this)
        img.setImageResource(icone)
        val tailleIcone = (20 * resources.displayMetrics.density).toInt()
        val paramsImg = LinearLayout.LayoutParams(tailleIcone, tailleIcone)
        paramsImg.marginEnd = (10 * resources.displayMetrics.density).toInt()
        img.layoutParams = paramsImg

        val txt = TextView(this)
        txt.text = texte
        txt.setTextColor(resources.getColor(R.color.texte_principal, theme))
        txt.textSize = 14f

        ligne.addView(img)
        ligne.addView(txt)
        layoutLignesInfo.addView(ligne)

        val animation = AnimationUtils.loadAnimation(this, R.anim.apparition_ligne)
        animation.startOffset = (layoutLignesInfo.childCount - 1) * 90L
        ligne.startAnimation(animation)
    }

    /**
     * Lance la lecture HORS du thread UI (les echanges NFC sont bloquants et peuvent durer
     * plusieurs secondes : sur le thread principal, ils gelaient l'interface).
     */
    private fun demarrerLecture(tag: Tag) {
        if (!lectureEnCours.compareAndSet(false, true)) return
        Thread {
            try {
                lireTag(tag)
            } catch (e: Exception) {
                runOnUiThread {
                    txtStatut.text = "Erreur inattendue : ${e.message}"
                    demarrerPulseNfc()
                }
            } finally {
                lectureEnCours.set(false)
            }
        }.start()
    }

    /** Tourne sur un thread d'arriere-plan : ne touche a l'interface que via runOnUiThread. */
    private fun lireTag(tag: Tag) {
        runOnUiThread {
            arreterPulseNfc()
            layoutLignesInfo.removeAllViews()
            txtResultat.visibility = View.GONE
            txtStatut.text = "Lecture en cours... ne bouge pas le telephone"
        }

        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            runOnUiThread {
                txtStatut.text = "Ce tag n'est pas un MIFARE Classic (ou ton telephone ne le supporte pas)."
                demarrerPulseNfc()
            }
            return
        }

        val uid = tag.id
        val uidHex = uid.joinToString("") { String.format("%02X", it) }
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date())
        val cles = BambuKeyDeriver.deriverClesA(uid)

        val lecture = LecteurTagRobuste.lire(AccesMifare(mifare), cles)
        val rapport = construireRapport(mifare, uidHex, date, cles, lecture)

        runOnUiThread { afficherResultat(uidHex, rapport, lecture) }
    }

    private fun construireRapport(
        mifare: MifareClassic,
        uidHex: String,
        date: String,
        cles: List<ByteArray>,
        lecture: ResultatLecture
    ): String {
        val rapport = StringBuilder()
        rapport.append("=== Dump tag Bambu Lab ===\n")
        rapport.append("Date : $date\n")
        rapport.append("UID : $uidHex\n")
        rapport.append("Nb secteurs : ${mifare.sectorCount}\n")
        rapport.append("Lecture : ${lecture.nbSecteursLus}/${lecture.statutSecteurs.size} secteurs, ${lecture.nbPasses} passe(s)\n\n")
        if (lecture.connexionImpossible) {
            rapport.append("Connexion au tag impossible (tag deplace, ou telephone sans support MIFARE Classic complet).\n")
        }
        for (statut in lecture.statutSecteurs) {
            val cleHex = cles.getOrNull(statut.secteur)?.joinToString("") { String.format("%02X", it) } ?: "?"
            val etat = if (statut.lu) "OK" else "ECHEC : ${statut.detail}"
            rapport.append("[Secteur ${statut.secteur}] $etat (cle : $cleHex)\n")
            val premier = mifare.sectorToBlock(statut.secteur)
            for (i in 0 until mifare.getBlockCountInSector(statut.secteur)) {
                val numBloc = premier + i
                val donnees = lecture.blocs[numBloc]
                if (donnees != null) {
                    rapport.append("  Bloc $numBloc : ${donnees.joinToString(" ") { String.format("%02X", it) }}\n")
                } else if (!statut.lu && statut.detail != "non tente") {
                    rapport.append("  Bloc $numBloc : non lu\n")
                }
            }
            rapport.append("\n")
        }
        return rapport.toString()
    }

    /** Doit etre appelee sur le thread UI. */
    private fun afficherResultat(uidHex: String, rapport: String, lecture: ResultatLecture) {
        val tousLesBlocsLisibles = lecture.blocs.toSortedMap().values
            .joinToString("") { String(it, Charsets.ISO_8859_1) }

        val infoFilament = BambuTagDecoder.decoder(lecture.blocs)
        val resultatMatiere = infoFilament.codeMatiere?.let { MaterialIdLookup.trouverEtTraduire(it) }
            ?: MaterialIdLookup.trouverEtTraduire(tousLesBlocsLisibles)

        val resumeTexte = StringBuilder()
        resumeTexte.append("UID du tag : $uidHex\n")

        val nomFilament: String? = resultatMatiere?.second ?: infoFilament.typeDetaille ?: infoFilament.typeFilament
        val codeAffiche: String? = resultatMatiere?.first ?: infoFilament.codeMatiere

        if (nomFilament != null) {
            txtStatut.text = if (lecture.essentielsLus) "Filament detecte" else "Filament detecte (lecture partielle)"
            ajouterLigneInfo(R.drawable.ic_materiau, nomFilament + if (codeAffiche != null) " ($codeAffiche)" else "")
            resumeTexte.append("Filament : $nomFilament${if (codeAffiche != null) " ($codeAffiche)" else ""}\n")
            dernierNomFilament = nomFilament
        } else {
            txtStatut.text = if (lecture.connexionImpossible) "Connexion au tag impossible - recolle le telephone" else "Filament non identifie"
            dernierNomFilament = null
        }

        if (infoFilament.couleurHex != null) {
            try {
                val hexPur = infoFilament.couleurHex.removePrefix("#")
                if (hexPur.length == 8) {
                    val hexRGB = hexPur.substring(0, 6)
                    val r = hexPur.substring(0, 2).toInt(16)
                    val g = hexPur.substring(2, 4).toInt(16)
                    val b = hexPur.substring(4, 6).toInt(16)
                    val a = hexPur.substring(6, 8).toInt(16)
                    val resultatCouleur = NomCouleur.trouverNom(hexRGB, nomFilament ?: "")
                    val suffixe = if (resultatCouleur.estExact) "" else " (approximatif)"
                    ajouterLigneInfo(R.drawable.ic_couleur, "${resultatCouleur.nom}$suffixe")
                    ajouterLigneInfo(R.drawable.ic_couleur, "Code hexa : #$hexRGB")
                    resumeTexte.append("Couleur : ${resultatCouleur.nom}$suffixe (${infoFilament.couleurHex})\n")

                    val referenceProduit = NomCouleur.trouverReferenceProduit(hexRGB, nomFilament ?: "")
                    if (referenceProduit != null) {
                        ajouterLigneInfo(R.drawable.ic_materiau, "Reference Bambu : $referenceProduit")
                        resumeTexte.append("Reference Bambu : $referenceProduit\n")
                    }

                    vuCouleur.backgroundTintList = ColorStateList.valueOf(Color.argb(a, r, g, b))
                    vuCouleur.visibility = View.VISIBLE
                    imgNfc.visibility = View.GONE
                    dernierNomCouleur = "${resultatCouleur.nom}$suffixe"
                    dernierNomCouleurEtiquette = "${resultatCouleur.nomCourt}$suffixe"
                    dernierCodeHexa = "#$hexRGB"
                    dernierCouleurArgb = Color.argb(a, r, g, b)
                }
            } catch (e: Exception) {
                vuCouleur.visibility = View.GONE
                imgNfc.visibility = View.VISIBLE
                dernierNomCouleur = null
                dernierNomCouleurEtiquette = null
                dernierCodeHexa = null
                dernierCouleurArgb = null
            }
        } else {
            vuCouleur.visibility = View.GONE
            imgNfc.visibility = View.VISIBLE
            dernierNomCouleur = null
            dernierNomCouleurEtiquette = null
            dernierCodeHexa = null
            dernierCouleurArgb = null
        }

        if (infoFilament.poidsGrammes != null && infoFilament.poidsGrammes in 1..10000) {
            ajouterLigneInfo(R.drawable.ic_materiau, "Poids bobine : ${infoFilament.poidsGrammes}g")
            resumeTexte.append("Poids bobine : ${infoFilament.poidsGrammes}g\n")
            dernierPoidsGrammes = infoFilament.poidsGrammes
        } else {
            dernierPoidsGrammes = null
        }
        if (infoFilament.tempBuseMin != null && infoFilament.tempBuseMax != null &&
            infoFilament.tempBuseMin in 0..500 && infoFilament.tempBuseMax in 0..500) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Buse : ${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C")
            resumeTexte.append("Temperature buse : ${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C\n")
            dernierTempBuseTexte = "${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C"
        } else {
            dernierTempBuseTexte = null
        }
        if (infoFilament.tempPlateau != null && infoFilament.tempPlateau in 0..200) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Plateau : ${infoFilament.tempPlateau}C")
            resumeTexte.append("Temperature plateau : ${infoFilament.tempPlateau}C\n")
            dernierTempPlateau = infoFilament.tempPlateau
        } else {
            dernierTempPlateau = null
        }
        if (infoFilament.tempSechage != null && infoFilament.tempSechage in 0..150) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Sechage : ${infoFilament.tempSechage}C / ${infoFilament.dureeSechage ?: "?"}h")
            resumeTexte.append("Sechage recommande : ${infoFilament.tempSechage}C pendant ${infoFilament.dureeSechage ?: "?"}h\n")
        }

        // Diagnostic du tag (sain / limite / defaillant) + contexte de session
        val diagnostic = sessionDiagnostic.evaluer(uidHex, lecture, infoFilament)
        ajouterLigneInfo(R.drawable.ic_nfc, "${diagnostic.niveau.pastille} ${diagnostic.niveau.libelle}")
        resumeTexte.append("Diagnostic : ${diagnostic.niveau.libelle}\n")
        for (raison in diagnostic.raisons) {
            ajouterLigneInfo(R.drawable.ic_nfc, raison)
            resumeTexte.append("  - $raison\n")
        }
        diagnostic.conseil?.let { ajouterLigneInfo(R.drawable.ic_nfc, it) }

        // Lecture incomplete : ni historique, ni etiquette bancale
        val detectionReussie = nomFilament != null && lecture.essentielsLus

        dernierDumpTexte = resumeTexte.toString() + "\n\n--- DETAIL TECHNIQUE COMPLET (pour export) ---\n\n" + rapport
        dernierResume = resumeTexte.toString()
        txtResultat.text = dernierDumpTexte

        if (detectionReussie) {
            vibrerConfirmation()
            val couleurPourHistorique = infoFilament.couleurHex ?: ""
            enregistrerDansHistorique(uidHex, nomFilament ?: "Inconnu", couleurPourHistorique)

            filesAttenteEtiquettes.add(
                EtiquetteEnAttente(
                    nomFilament = nomFilament ?: "Filament inconnu",
                    nomCouleur = dernierNomCouleurEtiquette,
                    couleurArgb = dernierCouleurArgb,
                    codeHexa = dernierCodeHexa,
                    poidsGrammes = dernierPoidsGrammes,
                    tempBuseTexte = dernierTempBuseTexte,
                    tempPlateau = dernierTempPlateau
                )
            )
            mettreAJourBoutonImpression()
        } else {
            demarrerPulseNfc()
        }
    }

    private fun mettreAJourBoutonImpression() {
        val n = filesAttenteEtiquettes.size
        btnImprimerEtiquette.text = if (n <= 1) {
            "Imprimer l'etiquette"
        } else {
            "Imprimer les etiquettes ($n)"
        }
    }

    private fun vibrerConfirmation() {
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (e: Exception) {
            // Pas grave si la vibration echoue, ce n'est qu'un confort
        }
    }

    private fun enregistrerDansHistorique(uid: String, nom: String, couleurHex: String) {
        try {
            val fichier = File(getExternalFilesDir(null), "historique_scans.csv")
            val ligne = "${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())};$uid;$nom;$couleurHex\n"
            fichier.appendText(ligne)
        } catch (e: Exception) {
            // Pas grave si l'ecriture de l'historique echoue
        }
    }

    private fun afficherRechercheCouleur() {
        val champSaisie = EditText(this)
        champSaisie.hint = "Ex : FF6A13 ou #FF6A13"
        champSaisie.inputType = InputType.TYPE_CLASS_TEXT
        champSaisie.filters = arrayOf(InputFilter.LengthFilter(7))

        val conteneur = LinearLayout(this)
        conteneur.orientation = LinearLayout.VERTICAL
        val paddingPx = (20 * resources.displayMetrics.density).toInt()
        conteneur.setPadding(paddingPx, paddingPx, paddingPx, 0)
        conteneur.addView(champSaisie)

        AlertDialog.Builder(this)
            .setTitle("Chercher une couleur")
            .setMessage("Colle le code hexadecimal d'une couleur (fournisseur tiers par exemple) pour trouver les teintes Bambu officielles les plus proches.")
            .setView(conteneur)
            .setPositiveButton("Chercher") { _, _ ->
                val saisie = champSaisie.text.toString().trim().removePrefix("#").uppercase()
                if (saisie.length != 6 || !saisie.matches(Regex("[0-9A-F]{6}"))) {
                    Toast.makeText(this, "Code hexadecimal invalide (attendu : 6 caracteres, ex FF6A13)", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                afficherResultatsRecherche(saisie)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun afficherResultatsRecherche(hexSaisi: String) {
        val correspondances = NomCouleur.trouverCorrespondancesProches(hexSaisi, 5)
        if (correspondances.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Aucun resultat")
                .setMessage("Aucune correspondance trouvee.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val texte = StringBuilder()
        texte.append("Code saisi : #$hexSaisi\n\n")
        correspondances.forEachIndexed { index, c ->
            val etiquette = if (c.estExact) "EXACT" else "Proche"
            texte.append("${index + 1}. ${c.nom}\n")
            texte.append("   ${c.ligne} - #${c.hexOfficiel} ($etiquette)\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Resultats pour #$hexSaisi")
            .setMessage(texte.toString())
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun afficherHistorique() {
        try {
            val fichier = File(getExternalFilesDir(null), "historique_scans.csv")
            if (!fichier.exists() || fichier.readText().isBlank()) {
                AlertDialog.Builder(this)
                    .setTitle("Historique des scans")
                    .setMessage("Aucun scan enregistre pour l'instant.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }
            val lignes = fichier.readLines().reversed()
            val texteAffiche = lignes.joinToString("\n\n") { ligne ->
                val parts = ligne.split(";")
                if (parts.size >= 3) {
                    "${parts[0]}\n${parts[2]}${if (parts.size >= 4 && parts[3].isNotBlank()) " (${parts[3]})" else ""}"
                } else ligne
            }
            AlertDialog.Builder(this)
                .setTitle("Historique des scans (${lignes.size})")
                .setMessage(texteAffiche)
                .setPositiveButton("Fermer", null)
                .setNegativeButton("Vider l'historique") { _, _ ->
                    fichier.delete()
                    Toast.makeText(this, "Historique efface.", Toast.LENGTH_SHORT).show()
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur lecture historique : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copierResume() {
        if (dernierResume.isEmpty()) {
            Toast.makeText(this, "Rien a copier pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Resultat Bambu RFID", dernierResume))
        Toast.makeText(this, "Copie dans le presse-papier.", Toast.LENGTH_SHORT).show()
    }

    private fun partagerResume() {
        if (dernierResume.isEmpty()) {
            Toast.makeText(this, "Rien a partager pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, dernierResume)
        startActivity(Intent.createChooser(intent, "Partager le resultat"))
    }

    private fun imprimerEtiquette() {
        if (filesAttenteEtiquettes.isEmpty()) {
            Toast.makeText(this, "Aucune etiquette en attente, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }

        // On fige la liste au moment de l'impression : si un nouveau scan arrive pendant
        // que la boite de dialogue systeme est ouverte, il ne sera pas perdu, juste pas
        // inclus dans CE job d'impression (il restera dans la file pour la prochaine fois).
        val etiquettesAImprimer = filesAttenteEtiquettes.toList()

        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            var document: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                document = PdfDocument()
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val nbPages = Math.ceil(etiquettesAImprimer.size.toDouble() / ETIQUETTES_PAR_PAGE).toInt().coerceAtLeast(1)
                val info = PrintDocumentInfo.Builder("etiquettes_filament.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(nbPages)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                var index = 0
                while (index < etiquettesAImprimer.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index / ETIQUETTES_PAR_PAGE + 1).create()
                    val page = document!!.startPage(pageInfo)
                    val lotDeCettePage = etiquettesAImprimer.subList(
                        index,
                        minOf(index + ETIQUETTES_PAR_PAGE, etiquettesAImprimer.size)
                    )
                    lotDeCettePage.forEachIndexed { positionDansPage, etiquette ->
                        val colonne = positionDansPage % COLONNES_GRILLE
                        val ligne = positionDansPage / COLONNES_GRILLE
                        val x = MARGE_GRILLE + colonne * LARGEUR_CELLULE
                        val y = MARGE_GRILLE + ligne * HAUTEUR_CELLULE
                        dessinerEtiquette(page.canvas, etiquette, x, y)
                    }
                    document!!.finishPage(page)
                    index += ETIQUETTES_PAR_PAGE
                }

                try {
                    document!!.writeTo(FileOutputStream(destination.fileDescriptor))
                } catch (e: IOException) {
                    callback.onWriteFailed(e.message)
                    return
                } finally {
                    document!!.close()
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }

        printManager.print("Etiquettes filament Bambu", adapter, PrintAttributes.Builder().build())

        // On retire de la file uniquement ce qu'on vient d'envoyer a l'impression (pas ce qui
        // aurait ete ajoute entre-temps).
        filesAttenteEtiquettes.removeAll(etiquettesAImprimer)
        mettreAJourBoutonImpression()
    }

    // Grille d'etiquettes sur une page A4 (595x842 points) : 3 colonnes x 7 lignes = 21
    // etiquettes par page, a decouper aux ciseaux une fois imprimees.
    companion object {
        const val COLONNES_GRILLE = 3
        const val LIGNES_GRILLE = 7
        const val ETIQUETTES_PAR_PAGE = COLONNES_GRILLE * LIGNES_GRILLE
        const val MARGE_GRILLE = 25f
        const val LARGEUR_CELLULE = (595f - 2 * MARGE_GRILLE) / COLONNES_GRILLE
        const val HAUTEUR_CELLULE = (842f - 2 * MARGE_GRILLE) / LIGNES_GRILLE
    }

    private fun dessinerEtiquette(canvas: Canvas, etiquette: EtiquetteEnAttente, margeGauche: Float, y: Float) {
        // Taille reelle d'une petite etiquette (environ 6cm x 3.7cm), avec un peu de marge
        // interne par rapport a la cellule de grille pour laisser un espace de decoupe.
        val largeurEtiquette = LARGEUR_CELLULE - 8f
        val hauteurEtiquette = HAUTEUR_CELLULE - 8f

        val paintTitre = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 7f
        }
        val paintMatiere = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val paintTexte = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 8f
        }
        val paintBordure = Paint().apply {
            color = android.graphics.Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val paintSwatch = Paint().apply {
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(margeGauche, y, margeGauche + largeurEtiquette, y + hauteurEtiquette, 5f, 5f, paintBordure)

        val margeInterne = margeGauche + 8f
        var yInterne = y + 13f
        canvas.drawText("Bambu RFID Reader", margeInterne, yInterne, paintTitre)

        yInterne += 14f
        canvas.drawText(etiquette.nomFilament, margeInterne, yInterne, paintMatiere)

        yInterne += 16f
        if (etiquette.couleurArgb != null) {
            paintSwatch.color = etiquette.couleurArgb
            canvas.drawCircle(margeInterne + 5f, yInterne - 3f, 5.5f, paintSwatch)
            val paintCercleBordure = Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawCircle(margeInterne + 5f, yInterne - 3f, 5.5f, paintCercleBordure)
            canvas.drawText(etiquette.nomCouleur ?: "", margeInterne + 16f, yInterne, paintTexte)
            yInterne += 11f
            if (etiquette.codeHexa != null) {
                canvas.drawText(etiquette.codeHexa, margeInterne + 16f, yInterne, paintTexte)
                yInterne += 12f
            }
        }

        if (etiquette.poidsGrammes != null) {
            canvas.drawText("Poids : ${etiquette.poidsGrammes}g", margeInterne, yInterne, paintTexte)
            yInterne += 11f
        }

        if (etiquette.tempBuseTexte != null) {
            canvas.drawText("Buse : ${etiquette.tempBuseTexte}", margeInterne, yInterne, paintTexte)
            yInterne += 11f
        }

        if (etiquette.tempPlateau != null) {
            canvas.drawText("Plateau : ${etiquette.tempPlateau}C", margeInterne, yInterne, paintTexte)
        }

        val paintDate = Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 5.5f
        }
        canvas.drawText(
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date()),
            margeInterne,
            y + hauteurEtiquette - 6f,
            paintDate
        )
    }

    private fun exporterDump() {
        if (dernierDumpTexte.isEmpty()) {
            Toast.makeText(this, "Aucun dump a exporter pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dossier = File(getExternalFilesDir(null), "dumps_bambu")
            if (!dossier.exists()) dossier.mkdirs()
            val nomFichier = "dump_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".txt"
            val fichier = File(dossier, nomFichier)
            fichier.writeText(dernierDumpTexte)
            Toast.makeText(this, "Dump enregistre : ${fichier.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur export : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
