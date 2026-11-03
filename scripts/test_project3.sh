#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
ANTLR_JAR="${ANTLR_JAR:-libs/antlr-4.13.2-complete.jar}"
JAVA="${JAVA:-java}"
JAVAC="${JAVAC:-javac}"
# The supplied output-comparison script invokes java through PATH.
export PATH="$(dirname "$(command -v "$JAVA")"):$PATH"
make test ANTLR_JAR="$ANTLR_JAR" JAVA="$JAVA" JAVAC="$JAVAC"
bash scripts/run_tests.sh "$ANTLR_JAR" tests/project3/in tests/project3/out
mkdir -p target/test-classes
"$JAVAC" -encoding UTF-8 --release 17 -cp "target/project3.jar:$ANTLR_JAR" \
    -d target/test-classes tests/project3/SemanticApiTest.java
"$JAVA" -ea -cp "target/test-classes:target/project3.jar:$ANTLR_JAR" SemanticApiTest
