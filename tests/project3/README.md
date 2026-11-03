# Project 3 regression tests

Run from the repository root (Java 17+, ANTLR 4.13.2):

```bash
ANTLR_JAR=/path/to/antlr-4.13.2-complete.jar bash scripts/test_project3.sh
```

The runner builds the project and runs the eight supplied cases, the additional
input/output fixtures here, and `SemanticApiTest`. The API checks verify object
identity, registration order, scope boundaries, array nesting, and recovery state
that the framework's printed summary cannot expose. Expected fixture output is
specified independently of the implementation.

To validate with a specific JDK, set `JAVA=/path/to/jdk/bin/java` and
`JAVAC=/path/to/jdk/bin/javac` when invoking the runner.

These tests are local regression coverage, not the course's hidden tests.
`make handin` includes only the five required implementation files.
