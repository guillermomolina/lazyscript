# The lazy language component for GraalVM

Truffle languages can be packaged as components which can be installed into
GraalVM using the [Graal
updater](http://www.graalvm.org/docs/reference-manual/graal-updater/). 
Running `mvn package` in the lazylanguage folder also builds a
`ll-component.jar`. 
This file is the lazy languages component for GraalVM and can be installed by
running:

```
/path/to/graalvm/bin/gu install /path/to/ll-component.jar
```

