@rem
@rem Copyright 2015 the original author or authors.
@rem
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set JAVA_EXE=java.exe
where java >nul 2>nul
if "%ERRORLEVEL%"=="0" goto execute
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
exit /b 1
:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
