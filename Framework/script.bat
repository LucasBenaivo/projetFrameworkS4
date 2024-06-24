@echo off

set DESTINATION_DIR=D:\Etude\ITU\S4\Mr Naina\winterFramework\Test

REM Vérifier si le dossier lib existe dans le dossier de destination
if not exist "%DESTINATION_DIR%\lib" (
    mkdir "%DESTINATION_DIR%\lib"
)

REM Compiler les fichiers Java
javac -d .\classes *.java

REM Créer le fichier JAR
jar -cf .\winter.jar -C .\classes .

REM Copier le fichier JAR vers le dossier lib du dossier de destination
copy .\winter.jar "%DESTINATION_DIR%\lib"

pause
echo Compilation et copie terminées.
