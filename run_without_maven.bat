@echo off
REM Script pour lancer le projet EmoNado sans Maven
setlocal enabledelayedexpansion

echo ========================================
echo Lancement du projet EmoNado (sans Maven)
echo ========================================

cd /d c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail

REM Vérifier si Java est installé
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installé ou pas dans le PATH
    echo Veuillez installer Java 17 ou supérieur
    pause
    exit /b 1
)

echo [INFO] Java détecté. Vérification des JARs...

REM Construire le classpath
set CLASSPATH=target\classes;lib\*

REM Ajouter les ressources au classpath
set CLASSPATH=!CLASSPATH!;src\main\resources

echo [INFO] Classpath: !CLASSPATH!

echo [INFO] Lancement de l'application...

REM Lancer l'application avec JavaFX
java -cp "!CLASSPATH!" org.example.Main

if errorlevel 1 (
    echo [ERREUR] Erreur lors du lancement
    pause
)

pause
