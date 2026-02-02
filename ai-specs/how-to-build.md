# Running the build and tests

The project is built with Gradle. You can run the main test suite for the AI plugin with:

```
cd /Users/dimitry/git-repo/freeplane/freeplane
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-zulu"
gradle :freeplane_plugin_ai:test
```

The command compiles the entire workspace and executes the tests under `freeplane_plugin_ai`. The `JAVA_HOME` export ensures Java 21 is used (the project is compiled with `sourceCompatibility = 21`). Gradle may print warnings about deprecated APIs and missing sub-JARs, but test failures are reported at `freeplane_plugin_ai/build/reports/tests/test/index.html`.

If you run Gradle without a working `sdkman` installation, make sure the Java version satisfies the required release. Use `sdk use java 21.0.5-zulu` (or set `JAVA_HOME` manually) before running the Gradle command.

Because Gradle may require installing native components or accessing parts of the file system that need escalated permissions, request approval before running the command in this environment.
