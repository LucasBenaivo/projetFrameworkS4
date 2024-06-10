@echo off

set DESTINATION_DIR=C:\Users\Mahery\Desktop\working_dir\Framework\Test

REM Vérifier si le dossier lib existe dans le dossier de destination
if not exist "%DESTINATION_DIR%\lib" (
    mkdir "%DESTINATION_DIR%\lib"
)

REM Compiler les fichiers Java
javac -d .\classes *.java

REM Créer le fichier JAR
jar -cf .\framework.jar -C .\classes .

REM Copier le fichier JAR vers le dossier lib du dossier de destination
copy .\framework.jar "%DESTINATION_DIR%\lib"

echo Compilation et copie terminées.
