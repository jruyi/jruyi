@echo off
rem -------------------------------------------------------------------------
rem JRuyi CLI Client
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

rem JPDA options. Uncomment and modify as appropriate to enable remote debugging.
rem set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8010,server=y,suspend=y %JAVA_OPTS%

rem Set JRuyi home dir
set JAVA_OPTS=%JAVA_OPTS% "-Djruyi.home.dir=%JRUYI_HOME%"

rem Set program name
set PROGNAME_PROP="-Dprogram.name=jruyi.bat"
if "%OS%" == "Windows_NT" set PROGNAME_PROP="-Dprogram.name=%~nx0%"

set JAVA_OPTS=%JAVA_OPTS% %PROGNAME_PROP%

set EXE_JAR=%JRUYI_HOME%\main\jruyi-cli-${jruyi-cli.version}.jar

"%JAVA%" %JAVA_OPTS% -jar "%EXE_JAR%" %*
