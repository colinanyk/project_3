# project_3

A simple Java project using JDK 24, without Maven or any package manager.

## Structure

- `src/Main.java` - the main Java entry point
- `project_3.iml` - IntelliJ Java module configuration

## Java Setup

1. Open this folder in IntelliJ IDEA.
2. In **File > Project Structure > Project**, select JDK **24** as the project SDK.
   If it is not registered, choose **Add SDK > JDK** and select
   `/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home`.
3. Open `src/Main.java` and click the run icon beside `main`.

No run script or build tool is required.

## Tests

JUnit 5 tests are in `tests/routines/ExperimentTest.java` and
`tests/routines/PayloadOptimizerTest.java`. In IntelliJ, right-click `tests`
and select **Run 'All Tests'**, or use the run icon beside a test class.

The standalone JUnit Platform 1.14.0 JAR in `lib/` includes the Jupiter API,
parameterized test support, and test engine. It is attached to the IntelliJ
module as a test-only dependency; no package manager is required.

`Experiment` currently accepts null names and any integer values. Tests check
that these are returned unchanged rather than imposing new validation rules.

