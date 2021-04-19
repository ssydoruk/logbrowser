set BASEDIR=C: \Users\N748772\gcti

set LIBDIR=%BASEDIR%\1lib

set LOGDIR=$(pwd)\tmp

set VARDIR=%BASEDIR%\var

set ETCDIR=4BASEDIR%\etc

set JAVA_HOME=C: \Users\N748772\Software\graalvm-ce-java8-21.0.0.2

set PATH=4JAVA_HOME%\bin; %PATHA%

rem default name of the database

set LOGBRDB=logbr

set LOGBR_TMP=.tmp

set LOGBR_TMP_OPT=-Dlogbr.dir=%LOGBR_TMP%

rem DBG="-Xdebug -Xrunjdwp:transport=dt_socket, address=8000, server=y, suspend=y"™
set LOG_OPTS=-Dlog4j.configurationFile=4ETCDIR%\logbr. log4j2.indexer.xml -Dlog4j.logPath=%LOGDIR%
set MISC_OPTIONS=-Dsun. java2d.d3d=false -Dall=1 -Xms32000m -Xmx32000m

set SQLITE_PRAGMAS=-Dsqlite.pragma=true

set NO_TLIB_REQUESTS=-Dtlib.norequest=false

set TIMEDIFF=-Dtimediff.parse=false

rem ------------------ no config after this line -------------------------

set JAVA_OPTS=%DBG% %LOGBR_TMP_OPT% %LOG_OPTS% “MISC OPTIONSA *%SQLITE_PRAGMAS% %NO_TLIB_REQUESTS% %TIMEDIFF%
if [%1]==[] set RUN DIR=%CD%

else set RUN_DIR=41

cd %RUN_DIR%

set DB=%RUN_DIR%\ALOGBRDB%

set CFG=%ETCDIR%\backend. xml

ALIBDIR%\bin\indexer.bat --dbname=%DB% --basedir=%RUN DIR% --cfgxml %CFG%

