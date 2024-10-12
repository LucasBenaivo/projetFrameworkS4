@echo off

set DESTINATION_DIR=D:\Etude\ITU\S5\Mr Naina\Framework\Framework\Test

REM Vérifier si le dossier lib existe dans le dossier de destination
if not exist "%DESTINATION_DIR%\lib" (
    mkdir "%DESTINATION_DIR%\lib"
)

REM Compiler les fichiers Java
javac -cp .\lib\* -d .\classes *.java

REM Créer le fichier JAR
jar -cf .\winter.jar -C .\classes .

REM Copier le fichier JAR vers le dossier lib du dossier de destination
copy .\winter.jar "%DESTINATION_DIR%\lib"

echo Compilation et copie terminées.
