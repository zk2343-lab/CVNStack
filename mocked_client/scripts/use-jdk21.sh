#!/usr/bin/env bash
# AI-assisted.
# Points Maven and java at a JDK 21 home when the current java is not 21.
if [[ -n "${JAVA_HOME:-}" ]] && "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
  return 0 2>/dev/null || exit 0
fi
for candidate in \
  /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  /usr/lib/jvm/java-21-openjdk \
  /usr/lib/jvm/temurin-21
do
  if [[ -x "${candidate}/bin/java" ]]; then
    export JAVA_HOME="${candidate}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
    return 0 2>/dev/null || exit 0
  fi
done
echo "JDK 21 is required. Set JAVA_HOME and run again." >&2
exit 1
