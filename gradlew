#!/bin/sh

APP_BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
exec java -classpath "$APP_BASE_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"

