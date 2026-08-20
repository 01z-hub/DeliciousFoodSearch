@ECHO OFF
SET APP_BASE_DIR=%~dp0
java -classpath "%APP_BASE_DIR%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

