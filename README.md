<img src="logo/reg-lisa-logo.png">

# REGLiSA: a static analyzer for the REG language

REGLiSA is a static analyzer for programs written in the REG language, based on [LiSA](https://github.com/lisa-analyzer/lisa/). It builds the control flow graph of a `.reg` program and performs abstract interpretation-based static analysis over it. The REG language is inspired by the _regular commands_, whose syntax and semantics can be found in the following papers:

> Peter W. O'Hearn: Incorrectness logic. POPL 2020. 10:1-10:32

> Roberto Bruni, Roberto Giacobazzi, Roberta Gori, Francesco Ranzato: A Logic for Locally Complete Abstract Interpretations. LICS 2021. 1-13

## Table of Contents

- [REG Language Syntax](#reg-language-syntax)
- [Requirements](#requirements)
- [Usage](#usage)
- [Example](#example)
- [Contributors](#contributors)

---

## REG Language Syntax

### 🧱 Statements

- **Assignment**  
  `var := expr;`  
  Assigns the result of `expr` to `var`.

- **Conditional**  
  - `( condition ? ; statement )` Executes `statement` only if `condition`
  - `( condition ? )` Terminates the program if `condition` is false

- **Loop**  
  `( condition ? ; statement )* ;`  
  Repeats `statement` as long as the `condition` is true.

- **Sequence**  
  `statement1 ; statement2`  
  Executes statements in order.

- **Skip**  
  `skip;`  
  No operation, that is the smallest program that does nothing.

### ➗ Expressions

- **Arithmetic**: `+`, `-` , `*`
  Operate on integer values.

- **Comparison**: `<`, `<=`, `=`  
  Used in conditionals.

- **Boolean**: `!` (negation), `&` (conjunction)
  Applied to boolean expressions.

### 📌 Notes

- All __non-terminal__ statements end with a semicolon (`;`).
- Parentheses are used to group conditionals and loop bodies.

### 📜 Example

This code calculates the sum of even numbers and the product of odd numbers from 0 to 9.

```reg
n := 0;
even := 0;
odd := 1;

(
    n < 10 ? ;
    (
        y := n;

        (
            !(y <= 1)? ;
                y := y - 2
        )* ;

        (
            (y = 0)? ;
                even := even + n
        );
        (
            (y = 1) ? ;
                odd := odd * n
        );

        n := n + 1
    )
)*
```

---

## Requirements

The building process has been tested **only** in the following environment:
- Java 11
- Gradle 6.8

To build the analyzer:

```bash
./gradlew build
```

This produces a fat JAR file in `build/libs`.

---

## Usage

The analyzer is executed via command line, passing the `.reg` file as the only argument:

```bash
java -jar reg-lisa-all.jar [-a] [-f <file>] [-g <type>] [-o <dir>] [-h] [-v]
```

### Arguments

- `-a`, `--analysis`  Enable analysis (default: disabled)
- `-f`, `--file <arg>`  Input `.reg` file (default: `reglisa-testcases/runtime.reg`)
- `-g`, `--graph <arg>`  Graph type: DOT or HTML (default: HTML)
- `-h`, `--help`  Print help
- `-o`, `--output <arg>`  Output directory (default: `reglisa-outputs`)
- `-v`, `--version`  Print version

---

## Example

```bash
java -jar build/libs/reg-lisa.jar -a -f reglisa-testcases/runtime.reg -g HTML -o reglisa-outputs
```

There's already a run configuration in the project for IntelliJ IDEA and, by default, it runs the program with the default values.
The same is true for the tests, which can be run using the run configuration for JUnit.

## Contributors

<a href="https://github.com/lisa-analyzer/reg-lisa/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=lisa-analyzer/reg-lisa" />
</a>
