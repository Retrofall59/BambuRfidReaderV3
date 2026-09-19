package com.tomyn.bambureader

import android.nfc.tech.MifareClassic

/** Vraie implementation d'AccesTag, au-dessus de l'API MifareClassic d'Android. */
class AccesMifare(private val mifare: MifareClassic) : AccesTag {

    companion object {
        /** Delai max d'une commande vers le tag, en ms (le defaut Android est plus court). A ajuster si besoin. */
        private const val TIMEOUT_MS = 1000
        private const val TENTATIVES_CONNEXION = 3
    }

    override val nbSecteurs: Int get() = mifare.sectorCount

    override fun premierBloc(secteur: Int): Int = mifare.sectorToBlock(secteur)

    override fun nbBlocs(secteur: Int): Int = mifare.getBlockCountInSector(secteur)

    private fun dormir(ms: Long) {
        try { Thread.sleep(ms) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
    }

    override fun reconnecter(): Boolean {
        for (tentative in 1..TENTATIVES_CONNEXION) {
            try {
                if (mifare.isConnected) mifare.close()
            } catch (ignored: Exception) {
            }
            try {
                dormir(40)
                mifare.connect()
                // close() remet les timeouts a zero cote systeme : on les repose apres chaque connexion.
                try { mifare.timeout = TIMEOUT_MS } catch (ignored: Exception) {}
                return true
            } catch (e: Exception) {
                dormir(120)
            }
        }
        return false
    }

    override fun authentifier(secteur: Int, cle: ByteArray): Boolean =
        mifare.authenticateSectorWithKeyA(secteur, cle)

    override fun lireBloc(numBloc: Int): ByteArray = mifare.readBlock(numBloc)

    override fun fermer() {
        try { mifare.close() } catch (ignored: Exception) {}
    }
}
