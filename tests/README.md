# Tests

Tests de non-régression de la logique de l'appli (pas de l'interface). À relancer avant de
livrer toute modification du décodage, des couleurs ou du diagnostic.

## Lancer

```
cd tests
ANDROID_JAR=/chemin/vers/android.jar sh lancer_tests.sh
```

Il faut `kotlinc` et `java` dans le PATH, et l'`android.jar` du SDK Android
(`$ANDROID_HOME/platforms/android-34/android.jar`). Le script compile chaque test contre le code
actuel de `app/src/main/java/...` et affiche un bilan.

## Contenu

| Test | Ce qu'il vérifie |
|---|---|
| `TestDerivationCles.kt` | La dérivation HKDF des clés MIFARE, sur une **vraie paire** : UID `B2A9F8ED` et les 16 clés validées par l'appli sur une vraie bobine (dump fourni par legallou sur le forum). |
| `TestNomCouleur.kt` | La désambiguïsation par matière quand plusieurs gammes Bambu partagent le même hex (ASA blanc vs PLA Basic/Matte/ABS/... pour `#FFFFFF`). Cas réel corrigé en v2.10 (Zetif). |
| `TestDecodeurTag.kt` | Le décodeur sur les **vrais blocs** de cette même bobine (PETG Translucent) : code matière, type, poids, températures, séchage... doivent retomber sur ce que l'appli affichait sur le téléphone. |
| `TestLecteurEtDiagnostic.kt` | Le lecteur multi-passes et le diagnostic (sain / limite / défaillant) avec un faux tag qui simule les pannes MIFARE : échecs transitoires, tag perdu, clés refusées, reconnexion... |
| `TestTablesCouleurs.kt` | La table de couleurs Bambu (noms, références produit) et l'équivalence de couleurs (CIEDE2000). |
| `TestLibellesCouleurs.kt` | Les libellés de gamme dans les résultats de recherche de couleur (PLA Basic, Matte, Glow...). |
| `TestScenarioReplay.kt` | Rejoue trois scénarios de scan (mouvement, échecs rattrapés, scan parfait) et affiche le diagnostic obtenu, pour comparer d'une version à l'autre. Informatif : il n'échoue pas tout seul. |

## Ajouter un cas réel

Quand un testeur remonte un tag qui pose problème, exporte son dump depuis l'appli et ajoute
ses blocs dans `TestDecodeurTag.kt` (et son UID + clés dans `TestDerivationCles.kt`) : le cas
ne pourra plus régresser sans qu'on le voie.
