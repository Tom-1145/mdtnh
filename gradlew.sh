#!/bin/sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

# Gradle startup script for Linux (converted from gradlew.bat)

# Turn off command echoing if DEBUG is not set (simulating @echo off)
if [ -z "$DEBUG" ]; then
    set +x
fi

# Determine the directory where this script is located
DIRNAME=$(dirname "$0")
if [ -z "$DIRNAME" ]; then
    DIRNAME=.
fi

APP_BASE_NAME=$(basename "$0")

# Resolve "." and ".." to make APP_HOME an absolute path
APP_HOME=$(cd "$DIRNAME" && pwd -P) || exit 1

# Default JVM options (equivalent to -Xmx64m -Xms64m)
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Find java
if [ -n "$JAVA_HOME" ]; then
    # Remove surrounding double quotes from JAVA_HOME if present
    JAVA_HOME="${JAVA_HOME#\"}"
    JAVA_HOME="${JAVA_HOME%\"}"
    JAVA_EXE="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVA_EXE" ]; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation." >&2
        exit 1
    fi
else
    JAVA_EXE="java"
    if ! command -v "$JAVA_EXE" >/dev/null 2>&1; then
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation." >&2
        exit 1
    fi
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Execute Gradle
exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"