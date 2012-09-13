@echo off
SET BASEDIR=%~dp0
SET RUBYDIR=%BASEDIR%docloader

SET ARGC=0
FOR %%A in (%*) DO SET /A ARGC+=1
IF "%ARGC%"=="2" GOTO OK
  echo USAGE:
  echo   loadpdf.bat ^<directory with documents^> ^<name^>
  echo.
  echo After processing completes, do:
  echo   overview.bat ^<name^>
  goto :END 
:OK

REM install json_pure and rest-client gems, if not already installed
CALL gem list | find "json_pure" > nul
IF NOT ERRORLEVEL 1 GOTO JSONINSTALLED
  CALL gem install json_pure
:JSONINSTALLED

CALL gem list | find "rest-client" > nul
IF NOT ERRORLEVEL 1 GOTO RESTINSTALLED
  CALL gem install rest-client
:RESTINSTALLED

CALL gem list | find "unicode_utils" > nul
IF NOT ERRORLEVEL 1 GOTO UNICODEINSTALLED
  CALL gem install unicode_utils
:UNICODEINSTALLED

REM find pdf files in a directory, extract the text, convert to CSV
CALL ruby -I %RUBYDIR% %RUBYDIR%\docloader.rb %1 -o %2.csv -r

REM preprocess those suckers
CALL %BASEDIR%\preprocess.bat %2

:END
