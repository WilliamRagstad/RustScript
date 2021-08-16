@echo off
REM Build the RustScript CLI
echo Have you updated the VERSION constant in Cli.java?
set /p version=Version:
@echo on
rmdir dist\bin /S /Q
mkdir dist\bin
javac *.java core/*.java core/formatting/*.java
jar -cvmf manifest.txt dist\bin\rsc.jar *.class core/*.class core/util/*.class
cd dist\bin
echo Building native binary...
jpackage --name rsc --input . --main-jar rsc.jar --main-class Cli --type msi --vendor "William Ragstad" --description "The RustScript CLI tool" --app-version %version% --icon ../icon.ico --file-associations ../assoc.properties --win-console
copy ..\README.md .
powershell /c "Compress-Archive * rsc.zip"

@echo off
cd ..\..
echo.
echo Done!
pause
