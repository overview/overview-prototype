@echo off
set BASEDIR=%~dp0
set RUBYDIR=%BASEDIR%docloader

set ARGC=0
for %%A in (%*) do set /A ARGC+=1
if "%ARGC%"=="4" GOTO OK
    echo "USAGE:"
    echo "  dcupload.sh <directory with documents> <documentcloud user name> <documentcloud password> <name>"
    echo " "
    echo "After upload completes, do:"
    echo "  preprocess.sh <name>"
    echo "  overview.sh <name>"
    exit 
:OK

REM do bundle install to download required gemfiles, if not already done
if EXIST %RUBYDIR%\Gemfile.lock GOTO SKIP
  pushd $RUBYDIR
  bundle install
  popd
:SKIP

REM find pdf and txt files in a directory, extract the text, convert to CSV
ruby -I %RUBYDIR% %RUBYDIR%\docloader.rb -l -r $1 -u $2 -p $3 -o $4.csv 
