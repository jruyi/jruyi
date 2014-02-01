@echo off
rem -------------------------------------------------------------------------
rem JRuyi Bootstrap Script
rem -------------------------------------------------------------------------

if not "%JAVA_HOME%" == "" goto SET_JAVA

set JAVA=java
goto SKIP_JAVA

:SET_JAVA

set JAVA=%JAVA_HOME%\bin\java

:SKIP_JAVA

set DIRNAME=..
if "%OS%" == "Windows_NT" set DIRNAME="%~dp0%.."

pushd %DIRNAME%
set JRUYI_HOME=%cd%
popd

rem Set JRuyi home dir
set JAVA_OPTS="-Djruyi.home.dir=%JRUYI_HOME%"

rem JVM memory allocation pool parameters. Modify as appropriate.
rem set JAVA_OPTS=-Xms128m -Xmx512m %JAVA_OPTS%

rem Add -server to the JVM options, if supported.
"%JAVA%" -server -version 2>&1 | findstr /I hotspot > nul
if not errorlevel == 1 (set JAVA_OPTS=-server %JAVA_OPTS%)

rem JPDA options. Uncomment and modify as appropriate to enable remote debugging.
rem set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n %JAVA_OPTS%

rem Set program name
set PROGNAME_PROP="-Dprogram.name=ruyi.bat"
if "%OS%" == "Windows_NT" set PROGNAME_PROP="-Dprogram.name=%~nx0%"

set JAVA_OPTS=%JAVA_OPTS% %PROGNAME_PROP%

set EXE_JAR=%JRUYI_HOME%\main\jruyi-launcher-${jruyi-launcher.version}.jar

"%JAVA%" %JAVA_OPTS% -jar "%EXE_JAR%" %*

:END

pause
