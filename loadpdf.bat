@echo off
set BASEDIR=%~dp0
set RUBYDIR=%BASEDIR%docloader

set ARGC=0
for %%A in (%*) do set /A ARGC+=1
if "%ARGC%"=="2" GOTO OK
  echo "USAGE:"
  echo "  loadpdf.bat <directory with documents> <name>"
  echo " "
  echo "After processing completes, do:"
  echo "  overview.bat <name>"
  exit 
:OK

REM do bundle install to download required gemfiles, if not already done
if EXIST %RUBYDIR%\Gemfile.lock GOTO SKIP
  pushd $RUBYDIR
  bundle install
  popd
:SKIP

REM find pdf files in a directory, extract the text, convert to CSV
ruby -I %RUBYDIR% %RUBYDIR%\docloader.rb %1 -o %2.csv -r

REM preprocess those suckers
%BASEDIR%\preprocess.sh %2
