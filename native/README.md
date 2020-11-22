# The lazy language native image

Truffle language implementations can be AOT compiled using the GraalVM
[native-image](https://www.graalvm.org/docs/reference-manual/aot-compilation/)
tool.  Running `mvn package` in the lazylanguage folder also builds a
`llnative` executable This executable is the full Lazy Language
implementation as a native application, and has no need for a JVM to run.
