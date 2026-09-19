# Changelog

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
