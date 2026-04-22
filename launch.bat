@echo off
REM Script complet pour lancer EmoNado avec dépendances JavaFX
setlocal enabledelayedexpansion

echo ========================================
echo Lancement du projet EmoNado
echo ========================================

cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

REM Vérifier Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installé
    pause
    exit /b 1
)

echo [INFO] Java détecté

REM Vérifier si les classes sont compilées
if not exist target\classes\org\example\Main.class (
    echo [INFO] Classes non compilées. Compilation en cours...
    echo [ERREUR] Veuillez compiler d'abord avec Maven ou IntelliJ
    pause
    exit /b 1
)

echo [INFO] Classes trouvées

REM Copier les ressources FXML et images si nécessaire
if not exist target\classes\main.fxml (
    echo [INFO] Copie des ressources...
    if not exist target\classes\styles mkdir target\classes\styles
    if not exist target\classes\images mkdir target\classes\images
    if not exist target\classes\fxml mkdir target\classes\fxml
    
    copy src\main\resources\*.fxml target\classes\ >nul
    copy src\main\resources\styles\* target\classes\styles\ >nul
    copy src\main\resources\images\* target\classes\images\ >nul
    copy src\main\resources\fxml\* target\classes\fxml\ >nul 2>&1
)

echo [INFO] Construction du classpath...

REM Chercher tous les JARs dans lib
set CLASSPATH=target\classes;lib\*

echo [INFO] Lancement de l'application EmoNado...
echo.

REM Lancer avec JavaFX en ligne de commande (sans modules)
java -cp "!CLASSPATH!" org.example.Main

if errorlevel 1 (
    echo.
    echo [ERREUR] L'application n'a pas pu démarrer
    echo.
    echo Si vous voyez une erreur "NoClassDefFoundError: javafx", cela signifie que
    echo JavaFX n'est pas disponible. Veuillez:
    echo 1. Installer Maven: https://maven.apache.org/download.cgi
    echo 2. Ou installer JavaFX manuellement
)

pause
