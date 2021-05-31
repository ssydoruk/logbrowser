@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup
set REPO=


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\repo

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\org\apache\logging\log4j\log4j-api\2.13.3\log4j-api-2.13.3.jar;"%REPO%"\org\apache\logging\log4j\log4j-slf4j-impl\2.13.1\log4j-slf4j-impl-2.13.1.jar;"%REPO%"\org\slf4j\slf4j-api\1.7.25\slf4j-api-1.7.25.jar;"%REPO%"\org\apache\logging\log4j\log4j-core\2.13.3\log4j-core-2.13.3.jar;"%REPO%"\org\apache\commons\commons-lang3\3.7\commons-lang3-3.7.jar;"%REPO%"\commons-cli\commons-cli\1.4\commons-cli-1.4.jar;"%REPO%"\commons-io\commons-io\2.6\commons-io-2.6.jar;"%REPO%"\com\jidesoft\jide-oss\3.6.18\jide-oss-3.6.18.jar;"%REPO%"\org\json\json\20180813\json-20180813.jar;"%REPO%"\com\squareup\okhttp3\okhttp\4.5.0\okhttp-4.5.0.jar;"%REPO%"\com\eclipsesource\minimal-json\minimal-json\0.9.5\minimal-json-0.9.5.jar;"%REPO%"\org\xerial\sqlite-jdbc\3.32.3.2\sqlite-jdbc-3.32.3.2.jar;"%REPO%"\com\hynnet\jacob\1.18\jacob-1.18.jar;"%REPO%"\net\java\dev\jna\jna-platform\5.5.0\jna-platform-5.5.0.jar;"%REPO%"\net\java\dev\jna\jna\5.5.0\jna-5.5.0.jar;"%REPO%"\com\github\lgooddatepicker\LGoodDatePicker\11.0.0\LGoodDatePicker-11.0.0.jar;"%REPO%"\com\gliwka\hyperscan\hyperscan\1.0.0\hyperscan-1.0.0.jar;"%REPO%"\com\mycompany\Utils\1.0-SNAPSHOT\Utils-1.0-SNAPSHOT.jar;"%REPO%"\org\apache\commons\commons-compress\1.20\commons-compress-1.20.jar;"%REPO%"\org\apache\sshd\sshd-core\2.5.1\sshd-core-2.5.1.jar;"%REPO%"\org\apache\sshd\sshd-common\2.5.1\sshd-common-2.5.1.jar;"%REPO%"\org\apache\sshd\sshd-netty\2.5.1\sshd-netty-2.5.1.jar;"%REPO%"\io\netty\netty-transport\4.1.50.Final\netty-transport-4.1.50.Final.jar;"%REPO%"\io\netty\netty-resolver\4.1.50.Final\netty-resolver-4.1.50.Final.jar;"%REPO%"\io\netty\netty-handler\4.1.50.Final\netty-handler-4.1.50.Final.jar;"%REPO%"\io\netty\netty-codec\4.1.50.Final\netty-codec-4.1.50.Final.jar;"%REPO%"\io\netty\netty-transport-native-epoll\4.1.49.Final\netty-transport-native-epoll-4.1.49.Final.jar;"%REPO%"\io\netty\netty-common\4.1.49.Final\netty-common-4.1.49.Final.jar;"%REPO%"\io\netty\netty-buffer\4.1.49.Final\netty-buffer-4.1.49.Final.jar;"%REPO%"\io\netty\netty-transport-native-unix-common\4.1.49.Final\netty-transport-native-unix-common-4.1.49.Final.jar;"%REPO%"\org\apache\tomcat\tomcat-jni\9.0.5\tomcat-jni-9.0.5.jar;"%REPO%"\com\google\code\gson\gson\2.8.6\gson-2.8.6.jar;"%REPO%"\org\graalvm\sdk\graal-sdk\21.0.0.2\graal-sdk-21.0.0.2.jar;"%REPO%"\org\graalvm\truffle\truffle-api\21.0.0.2\truffle-api-21.0.0.2.jar;"%REPO%"\org\netbeans\external\AbsoluteLayout\RELEASE121\AbsoluteLayout-RELEASE121.jar;"%REPO%"\com\myutils\logbrowser\1.0\logbrowser-1.0.jar

set ENDORSED_DIR=
if NOT "%ENDORSED_DIR%" == "" set CLASSPATH="%BASEDIR%"\%ENDORSED_DIR%\*;%CLASSPATH%

if NOT "%CLASSPATH_PREFIX%" == "" set CLASSPATH=%CLASSPATH_PREFIX%;%CLASSPATH%

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS%  -classpath %CLASSPATH% -Dapp.name="indexer" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" com.myutils.logbrowser.indexer.Main %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
