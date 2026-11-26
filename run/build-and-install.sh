#!/usr/bin/env sh
# Stops the local test server, builds and tests Tweaks, installs the plugin on the server as plugins/Tweaks.jar.
# Options and settings: see run/DevInstall.java.
cd "$(dirname "$0")/.." || exit 1
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" run/DevInstall.java "$@"
