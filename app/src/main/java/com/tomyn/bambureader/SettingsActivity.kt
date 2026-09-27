package com.tomyn.bambureader

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.btnRetour).setOnClickListener { finish() }

        val groupe = findViewById<RadioGroup>(R.id.groupeSeuilDefaillant)
        val option2 = findViewById<RadioButton>(R.id.optionSeuil2)
        val option3 = findViewById<RadioButton>(R.id.optionSeuil3)
        val option4 = findViewById<RadioButton>(R.id.optionSeuil4)

        // Le seuil stocke est "scansMauvaisPrecedents avant DEFAILLANT" (1 = 2 scans au total)
        when (GestionnaireParametres.lireSeuilAvantDefaillant(this)) {
            2 -> option3.isChecked = true
            3 -> option4.isChecked = true
            else -> option2.isChecked = true
        }
        groupe.setOnCheckedChangeListener { _, checkedId ->
            val seuil = when (checkedId) {
                R.id.optionSeuil3 -> 2
                R.id.optionSeuil4 -> 3
                else -> 1
            }
            GestionnaireParametres.ecrireSeuilAvantDefaillant(this, seuil)
        }

        val interrupteurVibration = findViewById<Switch>(R.id.interrupteurVibration)
        interrupteurVibration.isChecked = GestionnaireParametres.lireVibrationFinLecture(this)
        interrupteurVibration.setOnCheckedChangeListener { _, active ->
            GestionnaireParametres.ecrireVibrationFinLecture(this, active)
        }

        findViewById<TextView>(R.id.texteVersion).text = try {
            val infos = packageManager.getPackageInfo(packageName, 0)
            "Bambu RFID Reader — version ${infos.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            "Bambu RFID Reader"
        }
    }
}
