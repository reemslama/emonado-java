@echo off
setlocal enabledelayedexpansion
title Installation et Lancement EmoNado

echo ========================================
echo Solution Complète: EmoNado Launcher
echo ========================================
echo.
echo Choisissez une option:
echo.
echo 1. Lancer depuis IntelliJ IDEA (RECOMMANDE)
echo 2. Télécharger Maven et compiler
echo 3. Utiliser les JARs compilés (Experimental)
echo 4. Quitter
echo.

set /p CHOICE="Votre choix (1-4): "

if "%CHOICE%"=="1" goto open_intellij
if "%CHOICE%"=="2" goto install_maven
if "%CHOICE%"=="3" goto run_compiled
if "%CHOICE%"=="4" goto exit_script

echo Choix invalide
pause
exit /b 1

:open_intellij
echo [INFO] Ouverture du projet dans IntelliJ...
cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

REM Chercher IntelliJ
for /d %%D in ("C:\Program Files\JetBrains\IntelliJ IDEA*") do (
    start "" "%%D\bin\idea64.exe" "c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail"
    goto end_script
)

echo [ERREUR] IntelliJ IDEA n'a pas pu être trouvé
echo Veuillez installer IntelliJ IDEA depuis: https://www.jetbrains.com/idea/
pause
exit /b 1

:install_maven
echo [INFO] Téléchargement de Maven...
echo.
echo Pour installer Maven:
echo 1. Visitez: https://maven.apache.org/download.cgi
echo 2. Téléchargez apache-maven-3.9.x-bin.zip (Windows)
echo 3. Extrayez-le (par exemple: C:\apache-maven-3.9.x)
echo 4. Ajoutez au PATH la variable d'environnement
echo    (C:\apache-maven-3.9.x\bin)
echo 5. Redémarrez l'invite de commande
echo 6. Lancez: mvn clean javafx:run
echo.
echo Une fois Maven installé, relancez ce script et choisissez l'option 3
pause
exit /b 0

:run_compiled
echo [INFO] Lancement de l'application compilée...
cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installé
    pause
    exit /b 1
)

REM Vérifier les classes compilées
if not exist target\classes\org\example\Main.class (
    echo [ERREUR] Classes Java non compilées
    echo Veuillez compiler d'abord via IntelliJ ou Maven
    pause
    exit /b 1
)

echo [INFO] Copie des ressources...
if not exist target\classes\styles mkdir target\classes\styles
if not exist target\classes\images mkdir target\classes\images
if not exist target\classes\fxml mkdir target\classes\fxml

xcopy src\main\resources\*.fxml target\classes\ /Y /Q 2>nul
xcopy src\main\resources\styles\* target\classes\styles\ /Y /Q 2>nul
xcopy src\main\resources\images\* target\classes\images\ /Y /Q 2>nul
xcopy src\main\resources\fxml\* target\classes\fxml\ /Y /Q 2>nul

echo [INFO] Lancement...
echo.

java -cp "target\classes;lib\*" org.example.Main

if errorlevel 1 (
    echo.
    echo [ERREUR] Erreur au lancement
    echo Causes possibles:
    echo - JavaFX n'est pas disponible
    echo - Classes Java mal compilées
    echo - Ressources manquantes
)

pause
exit /b 0

:end_script
echo [INFO] Projet ouvert dans IntelliJ. Utilisez Run (Shift+F10) pour lancer.
pause
exit /b 0

:exit_script
echo Au revoir!
exit /b 0
