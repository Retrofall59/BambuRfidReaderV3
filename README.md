# Bambu RFID Reader - lecture et dump de tags Bambu Lab

## Ce que fait cette appli

- Detecte un tag RFID Bambu Lab (MIFARE Classic 13.56MHz) approche du dos du telephone
- Derive automatiquement les 16 cles de secteur a partir de l'UID du tag, via l'algorithme
  HKDF-SHA256 publie par Bambu-Research-Group (voir BambuKeyDeriver.kt pour le detail)
- Decode et affiche le filament en clair : matiere, couleur (nom officiel Bambu EN/FR + code hexa),
  poids, temperatures buse / plateau / sechage, reference produit
- Lit tous les blocs de tous les secteurs accessibles (dump hexadecimal complet exportable en .txt)
- Lecture robuste : les secteurs utiles au decodage sont lus en priorite, le tag est re-selectionne
  apres chaque echec, et l'appli reessaie automatiquement (3 passes max) en ne relisant que ce qui manque
- Signale clairement une lecture incomplete (secteurs non lus) au lieu d'afficher un resultat bancal
- Diagnostic du tag a chaque scan : sain / limite / defaillant, avec les raisons (voir plus bas)
- Historique des scans (CSV), copier / partager, recherche de couleur par code hexa,
  impression d'etiquettes (jusqu'a 21 par page A4)

## Ce qu'elle NE fait PAS (et ne pourra jamais faire avec un AMS standard)

Ecrire un tag avec des donnees personnalisees que l'AMS accepterait. Chaque tag est signe
avec une cle RSA privee que seul Bambu Lab possede - sans cette cle, impossible de generer
une signature valide. Voir https://github.com/Bambu-Research-Group/RFID-Tag-Guide pour le
detail technique (section "Custom Tags" de leur FAQ).

## AVANT DE COMPILER : verifie que ton telephone supporte le MIFARE Classic

Beaucoup de telephones Android recents (notamment pas mal de Pixel et certains Samsung)
n'implementent PAS le support MIFARE Classic au niveau materiel/OS, pour des raisons de
licence sur le chiffrement Crypto1 utilise par ce type de tag. Ce n'est pas un probleme de
code, c'est une limitation de la puce NFC elle-meme.

Pour verifier rapidement : installe une appli comme "NFC TagInfo" (gratuite sur le Play
Store), scanne n'importe quel tag MIFARE Classic (meme un badge d'acces classique), et
regarde si l'appli arrive a en lire le contenu. Si oui, ton telephone est compatible.

## Telephones confirmes compatibles (retours utilisateurs)

Cette liste s'allonge au fil des retours sur le forum. Si tu testes sur un telephone qui
n'y est pas encore, n'hesite pas a partager ton retour pour qu'on l'ajoute : le bouton
"Copier le rapport de compatibilite" de l'appli genere un texte pret a coller sur le forum
(modele, Android, verdict du dernier scan ; il ne contient pas l'UID du tag).

| Telephone | Statut | Source |
|---|---|---|
| Samsung Galaxy S20 FE | Compatible (teste sur PLA et ABS) | Retour forum - Zetif |
| Xiaomi 15C | Compatible | Teste par l'auteur (Tomyn) |
| Redmi Note 12 | Compatible (teste sur TPU for AMS) | Retour forum - pascal_lb |
| Redmi Note 11S (Android 13) | Compatible (teste sur PLA Basic, lecture complete, tag sain) | Retour forum - Tchoum |

## Comment obtenir le .apk (le plus simple : sans rien installer)

Ce projet est configure pour se compiler automatiquement sur les serveurs de GitHub
(gratuit), sans avoir besoin d'installer Android Studio ni quoi que ce soit sur ton PC.

1. Cree un compte GitHub gratuit si t'en as pas deja un (github.com)
2. Cree un nouveau depot (repository), par exemple "BambuRfidReader"
3. Sur la page du depot, clique sur "uploading an existing file" et glisse-depose TOUT
   le contenu de ce dossier (garde bien la structure des sous-dossiers)

   ATTENTION : le dossier ".github" (avec le point devant) est cache par defaut dans
   l'explorateur de fichiers Windows/Mac. Si tu le glisses pas, la compilation
   automatique ne se declenchera pas. Active "Afficher les elements caches" dans ton
   explorateur de fichiers avant de faire le glisser-deposer, pour etre sur de bien
   inclure ce dossier.
4. Valide l'envoi (bouton vert "Commit changes")
5. Va dans l'onglet "Actions" du depot en haut de la page
6. Une compilation se lance automatiquement (ca prend 2-3 minutes)
7. Une fois termine (coche verte), clique dessus, puis clique sur "BambuRfidReader-v..." (le nom porte la version)
   tout en bas de la page pour telecharger le fichier .apk

8. Transfere ce .apk sur ton telephone (mail, cle USB, Google Drive...) et installe-le
   (il faudra peut-etre autoriser "sources inconnues" dans les parametres Android)

   Mises a jour : a partir de la version 1.9, la signature est fixe, donc une nouvelle version
   s'installe par-dessus l'ancienne sans rien desinstaller. (Passer de la 1.8 ou avant a la 1.9
   demande une derniere desinstallation ; pense a copier `historique_scans.csv` et `dumps_bambu`
   depuis Android/data/com.tomyn.bambureader/files/, ils partent avec l'appli.)

## Alternative : compiler toi-meme avec Android Studio

1. Installe Android Studio (gratuit, https://developer.android.com/studio)
2. Ouvre ce dossier entier comme projet ("Open an existing project")
3. Laisse Android Studio telecharger les dependances Gradle (peut prendre quelques minutes
   la premiere fois)
4. Branche ton telephone en USB avec le mode developpeur + debogage USB actives
5. Clique sur le bouton "Run" (triangle vert) pour installer et lancer l'appli directement
   sur ton telephone

## Utilisation

1. Lance l'appli, elle affiche "Approche une bobine Bambu..."
2. Pose le dos du telephone sur le tag RFID d'une bobine Bambu (generalement colle sur le
   carton central) et **ne bouge plus** jusqu'a la vibration de confirmation
3. L'appli affiche le filament en clair (matiere, couleur, poids, temperatures)
4. Si elle indique "Lecture incomplete", recolle le telephone sans bouger : la position de
   l'antenne NFC varie beaucoup d'un modele a l'autre, il faut parfois tatonner
5. Bouton "Exporter le dernier dump" pour sauvegarder le detail technique complet dans un .txt :
   Android te demande ou l'enregistrer (Telechargements, Drive...). Meme chose pour l'historique
   des scans (bouton "Exporter" dans la fenetre Historique).

## Diagnostic du tag (sain / limite / defaillant)

Utile pour trier des tags recuperes sur des bobines vides avant de les recoller.

- **Sain** : tout est lu du premier coup, donnees coherentes.
- **Limite** : lecture reussie mais avec des echecs rattrapes par les reessais, ou des secteurs non lus.
  A rescanner ; a surveiller si l'AMS le refuse de temps en temps.
- **Defaillant** : infos essentielles illisibles, au moins 4 echecs pendant un scan, ou donnees
  impossibles (ex. temperature de buse absurde).
- **Diagnostic impossible** : connexion impossible, ou cles Bambu refusees (probablement pas un tag Bambu).

Ce verdict est une heuristique, pas une mesure : un seul scan ne prouve rien, et un mauvais scan peut
venir du telephone ou de sa position autant que du tag. L'appli en tient compte : si elle a deja lu
d'autres tags sans souci pendant la session, elle indique que le tag est probablement en cause ;
sinon elle te dit qu'elle ne peut pas trancher et te suggere de comparer avec un autre tag.
Les seuils (4 echecs, plages de valeurs) sont des estimations non calibrees.

## Limites connues

- Ecriture de tags impossible (signature RSA de Bambu, voir plus haut)
- iOS impossible : Core NFC n'expose pas l'algorithme Crypto1 necessaire au MIFARE Classic
- Certains telephones ont un support MIFARE Classic partiel : lectures intermittentes possibles
  (limite materielle, pas un bug de l'appli)

## Documentation technique

Positions des blocs et algorithme de derivation des cles : 
https://github.com/Bambu-Research-Group/RFID-Tag-Guide/blob/main/BambuLabRfid.md
