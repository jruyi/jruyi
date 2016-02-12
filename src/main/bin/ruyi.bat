@echo off
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem
rem -------------------------------------------------------------------------
rem JRuyi Bootstrap Script
rem -------------------------------------------------------------------------

if not "%JAVA_HOME%" == "" goto SET_JAVA

set JAVA=java
goto SKIP_JAVA

:SET_JAVA

set JAVA="%JAVA_HOME%\\bin\\java"

:SKIP_JAVA

set DIRNAME=..
if "%OS%" == "Windows_NT" set DIRNAME="%~dp0%.."

pushd %DIRNAME%
set JRUYI_HOME=%cd%
popd

rem Add -server to the JVM options, if supported.
%JAVA% -server -version 2>&1 | findstr /I hotspot > nul
if not errorlevel == 1 (set JAVA_OPTS=-server %JAVA_OPTS%)

rem JPDA options. Uncomment and modify as appropriate to enable remote debugging.
rem set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n %JAVA_OPTS%

rem Set program name
set PROGNAME_PROP="-Dprogram.name=ruyi.bat"
if "%OS%" == "Windows_NT" set PROGNAME_PROP="-Dprogram.name=%~nx0%"

set JAVA_OPTS=%JAVA_OPTS% %PROGNAME_PROP%

set EXE_JAR="%JRUYI_HOME%\\main\\jruyi-launcher-${jruyiLauncherVersion}.jar"

%JAVA% %JAVA_OPTS% -jar %EXE_JAR% %*

:END
