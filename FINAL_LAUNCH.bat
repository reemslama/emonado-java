@echo off
REM Script final pour lancer EmoNado
setlocal enabledelayedexpansion

echo ========================================
echo Lancement Final du Projet EmoNado
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

REM Essayer de lancer avec Maven si disponible
mvn -version >nul 2>&1
if not errorlevel 1 (
    echo [INFO] Maven détecté. Lancement via Maven...
    mvn clean javafx:run
    goto end
)

echo [INFO] Maven non disponible. Lancement direct...

REM Vérifier les classes compilées
if not exist target\classes\org\example\Main.class (
    echo [ERREUR] Classes non compilées. Veuillez compiler d'abord via IntelliJ
    pause
    exit /b 1
)

echo [INFO] Classes trouvées

REM Copier les ressources
echo [INFO] Copie des ressources...
if not exist target\classes\styles mkdir target\classes\styles
if not exist target\classes\images mkdir target\classes\images
if not exist target\classes\fxml mkdir target\classes\fxml

xcopy src\main\resources\*.fxml target\classes\ /Y /Q 2>nul
xcopy src\main\resources\styles\* target\classes\styles\ /Y /Q 2>nul
xcopy src\main\resources\images\* target\classes\images\ /Y /Q 2>nul
xcopy src\main\resources\fxml\* target\classes\fxml\ /Y /Q 2>nul

echo [INFO] Construction du classpath...

REM Détecter automatiquement les JARs JavaFX dans .m2
set JAVAFX_PATH=C:\Users\LENOVO\.m2\repository\org\openjfx

if exist "!JAVAFX_PATH!" (
    echo [INFO] JavaFX trouvé dans .m2
    set MODULE_PATH=!JAVAFX_PATH!\javafx-controls\17.0.13;!JAVAFX_PATH!\javafx-fxml\17.0.13;!JAVAFX_PATH!\javafx-graphics\17.0.13;!JAVAFX_PATH!\javafx-base\17.0.13
    set MODULES=javafx.controls,javafx.fxml
) else (
    echo [INFO] JavaFX non trouvé dans .m2, utilisation des JARs locaux
    set CLASSPATH=target\classes;lib\*
    goto legacy_launch
)

echo [INFO] Lancement avec modules JavaFX...
echo.

java --module-path "!MODULE_PATH!" --add-modules !MODULES! -cp target\classes org.example.Main

goto end

:legacy_launch
echo [INFO] Lancement en mode legacy...
java -cp "!CLASSPATH!" org.example.Main

:end
if errorlevel 1 (
    echo.
    echo [ERREUR] Échec du lancement
    echo.
    echo Solutions:
    echo 1. Ouvrez IntelliJ et utilisez Run (Shift+F10)
    echo 2. Installez Maven et relancez ce script
    echo 3. Vérifiez que JavaFX est installé
)

pause
