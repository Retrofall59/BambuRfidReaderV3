package com.tomyn.bambureader

import android.content.Context

/**
 * Lit/ecrit les parametres utilisateur (SharedPreferences), partages entre MainActivity
 * et SettingsActivity.
 */
object GestionnaireParametres {

    private const val FICHIER = "bambureader_parametres"
    private const val CLE_SEUIL_DEFAILLANT = "seuil_scans_avant_defaillant"
    private const val CLE_VIBRATION_FIN_LECTURE = "vibration_fin_lecture"

    /** 1 = comportement d'origine (2 mauvais scans au total avant "defaillant"). */
    fun lireSeuilAvantDefaillant(context: Context): Int =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getInt(CLE_SEUIL_DEFAILLANT, 1)

    fun ecrireSeuilAvantDefaillant(context: Context, seuil: Int) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putInt(CLE_SEUIL_DEFAILLANT, seuil).apply()
    }

    fun lireVibrationFinLecture(context: Context): Boolean =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getBoolean(CLE_VIBRATION_FIN_LECTURE, true)

    fun ecrireVibrationFinLecture(context: Context, active: Boolean) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putBoolean(CLE_VIBRATION_FIN_LECTURE, active).apply()
    }
}
