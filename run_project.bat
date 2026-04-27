@echo off
REM Script pour lancer le projet EmoNado
echo ========================================
echo Lancement du projet EmoNado...
echo ========================================
cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

REM Vérifier si Maven est installé
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Maven n'est pas installé ou pas dans le PATH
    echo Veuillez installer Maven depuis: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Vérifier si Java est installé
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installé ou pas dans le PATH
    echo Veuillez installer Java 17 ou supérieur
    pause
    exit /b 1
)

echo [INFO] Téléchargement des dépendances...
mvn clean compile 2>&1 | tee build.log

if errorlevel 1 (
    echo [ERREUR] Erreur lors de la compilation. Voir build.log
    pause
    exit /b 1
)

echo [INFO] Lancement de l'application...
mvn javafx:run

if errorlevel 1 (
    echo [ERREUR] Erreur lors du lancement de l'application
    echo Consultez le fichier build.log pour plus de détails
    pause
)

pause
