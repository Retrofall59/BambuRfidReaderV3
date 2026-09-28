#!/bin/sh
# Lance tous les tests du projet contre le code actuel.
# Prerequis : kotlinc et java dans le PATH, et la variable ANDROID_JAR qui pointe vers
# l'android.jar du SDK (ex : $ANDROID_HOME/platforms/android-34/android.jar).
# Usage : cd tests && ANDROID_JAR=/chemin/android.jar sh lancer_tests.sh

: "${ANDROID_JAR:?definis ANDROID_JAR (chemin de android.jar du SDK Android)}"
SRC=../app/src/main/java/com/tomyn/bambureader
TMP=$(mktemp -d)
ECHECS=0

# Fichiers de logique pure (pas d'interface) dont dependent les tests
LOGIQUE="$SRC/AccesMifare.kt $SRC/AnalyseurCodes.kt $SRC/BambuKeyDeriver.kt $SRC/BambuTagDecoder.kt \
$SRC/DiagnosticTag.kt $SRC/EquivalenceBambu.kt $SRC/LecteurTagRobuste.kt $SRC/MaterialIdLookup.kt $SRC/NomCouleur.kt"

for t in TestDerivationCles TestDecodeurTag TestLecteurEtDiagnostic TestTablesCouleurs TestLibellesCouleurs TestScenarioReplay; do
  echo "=============== $t"
  if kotlinc -cp "$ANDROID_JAR" $t.kt $LOGIQUE -include-runtime -d "$TMP/$t.jar" 2>/dev/null; then
    java -jar "$TMP/$t.jar" > "$TMP/$t.txt" 2>&1
    CODE=$?
    if grep -q "ECHEC" "$TMP/$t.txt" || [ $CODE -ne 0 ]; then
      echo "  ECHEC"; grep "ECHEC" "$TMP/$t.txt" | head -10; ECHECS=$((ECHECS+1))
    else
      echo "  OK ($(grep -c '  OK  ' "$TMP/$t.txt") verifications)"
    fi
  else
    echo "  ECHEC de compilation"; ECHECS=$((ECHECS+1))
  fi
done

rm -rf "$TMP"
echo
if [ $ECHECS -eq 0 ]; then echo "TOUS LES TESTS PASSENT"; else echo "$ECHECS TEST(S) EN ECHEC"; exit 1; fi
