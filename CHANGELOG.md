# Changelog

## 2.0
- Corrige : les exports passent par le selecteur Android "Enregistrer sous" (Telechargements, Drive...). Avant, les fichiers restaient dans Android/data, inaccessible depuis Android 11.
- Nouveau : bouton "Exporter" dans l'historique des scans (fichier CSV).
- Change : l'APK telecharge depuis GitHub porte sa version dans son nom (BambuRfidReader-v2.0.apk).

## 1.9
- Nouveau : signature fixe de l'APK. A partir de cette version, les mises a jour s'installent PAR-DESSUS l'ancienne, sans desinstaller.
- Attention : pour passer de la 1.8 (ou avant) a la 1.9, il faut desinstaller une derniere fois (la signature change). Copie d'abord `historique_scans.csv` et le dossier `dumps_bambu` (dans Android/data/com.tomyn.bambureader/files/) : ils sont supprimes avec l'appli.

## 1.8
- Nouveau : bouton "Copier le rapport de compatibilite" (modele, Android, support MIFARE declare par le systeme, verdict et etat des secteurs du dernier scan, sans l'UID du tag), a coller sur le forum.
- Nouveau : si le NFC est desactive, l'appli le dit et propose d'ouvrir les reglages (au lieu de rester sur "Approche une bobine...").
- Corrige : l'icone NFC ne se deforme plus apres plusieurs scans rates (l'animation verticale n'etait jamais arretee).

## 1.7
- Corrige : quand l'appli est fermee et qu'on scanne un tag, elle s'ouvre maintenant ET lit le tag directement (avant, elle s'ouvrait sans rien lire et il fallait rescanner).
- Corrige : pas de relecture parasite du meme tag juste apres l'ouverture, ni lors d'une rotation d'ecran.

## 1.6
- Nouveau : diagnostic du tag apres chaque scan : sain / limite / defaillant / diagnostic impossible, avec les raisons.
- Nouveau : l'appli retient les tags sains lus pendant la session, pour dire si un mauvais scan vient plutot du tag ou du telephone.
- Change : le message "Lecture incomplete (x/16)" est remplace par ce diagnostic. Le verdict figure aussi dans le resume copie ou partage.
- Change : README mis a jour (diagnostic, limites connues).

## 1.5
- Corrige : la lecture NFC ne bloque plus l'interface, elle tourne en arriere-plan.
- Corrige : le tag est re-selectionne apres chaque echec (avant, les retries se faisaient sans reconnexion et ne servaient a rien).
- Ameliore : lecture en 3 passes maximum. Les secteurs 0 et 1 (ceux qui servent au decodage) passent en premier, et seuls les blocs manquants sont relus.
- Ameliore : un tag non Bambu ne coute plus que 3 essais, au lieu de tenter les 16 secteurs.
- Change : passage en mode lecteur NFC (enableReaderMode, NFC-A, sans verification NDEF) a la place du foreground dispatch.
- Change : une lecture incomplete n'alimente plus l'historique ni la file d'etiquettes.
- Nouveau : le rapport d'export detaille l'etat de chaque secteur et les blocs non lus.

## 1.4 et avant
- Versions anterieures a la refonte de la lecture (couleurs officielles, etiquettes, historique, retries, etc.).
