@echo on

set BASEDIR=C:\Users\ssydo\GCTI

set LIBDIR=%BASEDIR%\lib

if [%1]==[] (
set RUN_DIR=%CD%
) else (
set RUN_DIR=%1
)

cd %RUN_DIR%


set LOGDIR=%RUN_DIR%\.tmp

set VARDIR=%BASEDIR%\var

set ETCDIR=%BASEDIR%\etc\logbrowser

rem set JAVA_HOME='C:\Program Files\Amazon Corretto\jdk21.0.5_11'
rem set JAVA_HOME='C:\Program Files\RedHat\java-11-openjdk-11.0.8-2'

set PATH=%JAVA_HOME%\bin;%PATH%

rem default name of the database

set LOGBRDB=logbr

set LOGBR_TMP=.tmp

set LOGBR_TMP_OPT=-Dlogbr.dir=%LOGBR_TMP%

rem DBG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
set LOG_OPTS=-Dlog4j.configurationFile=%ETCDIR%\logbr.log4j2.inquirer.xml -DlogPath=%LOGDIR%
set MISC_OPTIONS=-Dsun.java2d.d3d=false -Dall=1 -Xms4000m -Xmx4000m -Djacob.dll=C:\Users\A793320\GCTI\bin\logbrowser\jacob-1.20-x64.dll

set SQLITE_PRAGMAS=-Dsqlite.pragma=true

set NO_TLIB_REQUESTS=-Dtlib.norequest=false

set TIMEDIFF=-Dtimediff.parse=false

rem ------------------ no config after this line -------------------------

set JAVA_OPTS=%DBG% %LOGBR_TMP_OPT% %LOG_OPTS% %MISC_OPTIONS% %SQLITE_PRAGMAS% %NO_TLIB_REQUESTS% %TIMEDIFF%

set DB=%RUN_DIR%\logbr

set CFG=%ETCDIR%\backend.xml

rem uncomment below if database is too large
rem set REFERENCE=--no-ref-sync

%BASEDIR%\bin\inquirer.bat %REFERENCE% --dbname=%DB% --config=%ETCDIR%\inquirer.cfg.json --basedir=%RUN_DIR% --outputspec=%ETCDIR%\outputspec3.xml