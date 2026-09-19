package com.tomyn.bambureader

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Lit le texte d'une photo (tableau de codes couleur d'un fabricant, par exemple) avec ML Kit,
 * directement sur le telephone (modele embarque : ni connexion, ni compte Google necessaires).
 * Les callbacks sont appeles sur le thread principal.
 */
object LecteurPhoto {

    fun lireTexte(context: Context, uri: Uri, onTexte: (String) -> Unit, onErreur: (String) -> Unit) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onErreur(e.message ?: "image illisible")
            return
        }
        val reconnaisseur = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        reconnaisseur.process(image)
            .addOnSuccessListener { resultat ->
                reconnaisseur.close()
                onTexte(resultat.text)
            }
            .addOnFailureListener { e ->
                reconnaisseur.close()
                onErreur(e.message ?: "erreur de reconnaissance")
            }
    }
}
