@echo off
setlocal

if "%JAVA_HOME%"=="" (
    echo JAVA_HOME n'est pas defini.
    echo Definis JAVA_HOME vers un JDK 17 puis relance le script.
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven est introuvable dans le PATH.
    echo Verifie l'installation de Maven puis relance le script.
    exit /b 1
)

echo JAVA_HOME=%JAVA_HOME%
echo Lancement de l'application avec Maven...
mvn -q javafx:run

endlocal
