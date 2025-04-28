#!/usr/bin/env bash

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

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS*|MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=""
if $cygwin ; then
    CLASSPATH=$(cygpath --path --windows "$CLASSPATH")
fi
if $msys ; then
    CLASSPATH=$(mixpath --path --windows "$CLASSPATH")
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done

APP_HOME=$(dirname "$PRG")

# Add ordnance-loader to CLASSPATH
# This is used to download the gradle-wrapper.jar
if [ -z "${APP_HOME}" ] ; then
    die "APP_HOME is not set."
fi
APP_HOME=$(cd "${APP_HOME}/." && pwd)
LOADER_JAR="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase maximum file descriptors if necessary
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=$(ulimit -H -n)
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            # use the system max
            MAX_FD="$MAX_FD_LIMIT"
        fi
        if [ "$MAX_FD" -gt "$MAX_FD_LIMIT" ] ; then
           warn "Max file descriptors (MAX_FD) cannot be GREATER than the system limit ($MAX_FD_LIMIT), using system limit instead."
           MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n "$MAX_FD"
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptors (MAX_FD) to $MAX_FD"
        fi
    else
        warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# Add the loader jar to the CLASSPATH
CLASSPATH="${LOADER_JAR}${CLASSPATH:+":$CLASSPATH"}"
export CLASSPATH

# Set the JVM options
GRADLE_OPTS="${DEFAULT_JVM_OPTS} ${GRADLE_OPTS:-} ${JAVA_OPTS:-}"

# Split up the JVM options found in GRADLE_OPTS
JVM_OPTS=()
while read -r; do
  if [ -n "$REPLY" ]; then
     JVM_OPTS+=( "$REPLY" )
  fi
done < <( echo "$GRADLE_OPTS" | awk '{$1=$1;print}' RS=' ' )

# Add the main class as the last command line argument.
JVM_OPTS+=( "org.gradle.wrapper.GradleWrapperMain" )

# Launch the Gradle command line client
exec "$JAVACMD" "${JVM_OPTS[@]}" "$@"
