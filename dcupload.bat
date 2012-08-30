@echo off
SET BASEDIR=%~dp0
SET RUBYDIR=%BASEDIR%docloader

SET ARGC=0
FOR %%A in (%*) DO SET /A ARGC+=1
IF "%ARGC%"=="4" GOTO OK
    echo USAGE:
    echo   dcupload.sh ^<directory with documents^> ^<documentcloud user name^> ^<documentcloud password^> ^<name^>
    echo.
    echo After upload completes, do:
    echo   preprocess.sh ^<name^>
    echo   overview.sh ^<name^>
    goto END
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

REM find pdf and txt files in a directory, extract the text, convert to CSV
CALL ruby -I %RUBYDIR% %RUBYDIR%\docloader.rb -l -r %1 -u %2 -p %3 -o %4.csv 

:END