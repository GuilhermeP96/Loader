@ECHO OFF
java -jar %~dp0\Loader.jar %~dp0\configLoader.txt >> %~dp0\logLoader.txt 2>&1
EXIT