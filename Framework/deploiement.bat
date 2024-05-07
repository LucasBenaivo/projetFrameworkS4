@echo off

set "lib=D:\Etude\ITU\S4\Mr Naina\ProjetFramework\Framework\winter\lib"
set "SRC=D:\Etude\ITU\S4\Mr Naina\ProjetFramework\Framework\winter\src"
set "JAR=D:\Etude\ITU\S4\Mr Naina\ProjetFramework\Framework\winter\jar"
set "CLASSPATH=D:\Etude\ITU\S4\Mr Naina\ProjetFramework\Test\lib"
set "jarName=winter" 

cd "%SRC%"

if not exist "%JAR%" mkdir "%JAR%"

javac -cp "%lib%\*" -d "%JAR%" *.java

cd "%JAR%"

jar -cvf "%jarName%.jar" .

xcopy /s /e /i /y "%jarName%.jar" "%CLASSPATH%"

pause
