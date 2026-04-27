@echo off
REM Script pour compiler et lancer le projet EmoNado
setlocal enabledelayedexpansion

echo ========================================
echo Compilation et lancement EmoNado
echo ========================================

cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

REM Vérifier si Java est installé
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installé
    pause
    exit /b 1
)

echo [INFO] Compilation en cours...

REM Créer le dossier de sortie s'il n'existe pas
if not exist target\classes mkdir target\classes

REM Construire le classpath pour la compilation
set CLASSPATH=lib\*

REM Compiler tous les fichiers Java
javac -cp "!CLASSPATH!" -d target\classes -encoding UTF-8 ^
    src\main\java\org\example\*.java ^
    src\main\java\org\example\controller\*.java ^
    src\main\java\org\example\service\*.java ^
    src\main\java\org\example\utils\*.java ^
    src\main\java\entities\*.java 2>compile_errors.txt

if errorlevel 1 (
    echo [ERREUR] Erreur de compilation. Consultez compile_errors.txt
    type compile_errors.txt
    pause
    exit /b 1
)

echo [INFO] Compilation réussie!

REM Copier les ressources
echo [INFO] Copie des ressources...
if not exist target\classes\styles mkdir target\classes\styles
if not exist target\classes\images mkdir target\classes\images
if not exist target\classes\fxml mkdir target\classes\fxml

xcopy src\main\resources\*.fxml target\classes\ /Y /Q
xcopy src\main\resources\styles\* target\classes\styles\ /Y /Q
xcopy src\main\resources\images\* target\classes\images\ /Y /Q

echo [INFO] Lancement de l'application...

REM Lancer l'application
java -cp "target\classes;lib\*" org.example.Main

if errorlevel 1 (
    echo [ERREUR] Erreur lors du lancement
    pause
)

pause
