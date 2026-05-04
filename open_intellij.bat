@echo off
REM Script pour lancer le projet via IntelliJ
setlocal enabledelayedexpansion

echo ========================================
echo Lancement du projet EmoNado via IntelliJ
echo ========================================

REM Chercher l'installation IntelliJ IDEA
set INTELLIJ_PATH=

REM Chercher dans les emplacements courants
for %%I in (
    "C:\Program Files\JetBrains\IntelliJ IDEA*"
    "C:\Program Files (x86)\JetBrains\IntelliJ IDEA*"
    "%APPDATA%\JetBrains\Toolbox\apps\IDEA-U\*"
) do (
    if exist %%I (
        set INTELLIJ_PATH=%%I
        goto found_intellij
    )
)

:found_intellij
if "!INTELLIJ_PATH!"=="" (
    echo [INFO] IntelliJ IDEA n'a pas pu être trouvé automatiquement
    echo [INFO] Vous pouvez lancer le projet directement via:
    echo    - IntelliJ IDEA: Menu Run ^> Run (ou Shift+F10)
    echo    - Ou créer une configuration de lancement Maven
    pause
    exit /b 1
)

echo [INFO] IntelliJ trouvé: !INTELLIJ_PATH!

REM Ouvrir le projet dans IntelliJ
start "" "!INTELLIJ_PATH!\bin\idea64.exe" "c:\Users\LENOVO\Downloads\ZooManagement3A38(3)\ZooManagement3A38\nourtravail"

echo [INFO] Ouverture du projet dans IntelliJ IDEA...
pause
