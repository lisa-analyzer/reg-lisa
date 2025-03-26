<img src="logo/reg-lisa-logo.png">

# REGLiSA: a static analyzer for the REG language

REGLiSA is a static analyzer for programs written in the REG language, based on [LiSA](https://github.com/lisa-analyzer/lisa/). It builds the control flow graph of a `.reg` program and performs abstract interpretation-based analysis over it.

## Table of Contents

- [Requirements](#requirements)
- [Usage](#usage)
- [Example](#example)

---

## Requirements

- Java 11 or higher
- Gradle 8.0 or higher

To build the analyzer:

```bash
./gradlew assemble
```

This produces a JAR file in `build/libs`.

---

## Usage

The analyzer is executed via command line, passing the `.reg` file as the only argument:

```bash
java -jar build/libs/reg-lisa.jar path/to/file.reg
```

---

## Example

```bash
java -jar build/libs/reg-lisa.jar reglisa-testcases/runtime.reg
```

There's already a run configuration in the project for IntelliJ IDEA, which can be used to run the analyzer on a `.reg` file.
By default, it runs the `runtime.reg` file, but this can be changed in the run configuration.
The same is true for the tests, which can be run using the run configuration for JUnit.

## Contributors

<a href="https://github.com/lisa-analyzer/reg-lisa/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=lisa-analyzer/reg-lisa" />
</a>