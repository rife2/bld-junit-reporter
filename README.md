# JUnit Reporter Extension for [b<span style="color:orange">l</span>d](https://rife2.com/bld)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-junit-reporter%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-junit-reporter)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-junit-reporter%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-junit-reporter)
[![GitHub CI](https://github.com/rife2/bld-junit-reporter/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-junit-reporter/actions/workflows/bld.yml)

To install the latest version, add the following to the
`lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-reporter=com.uwyn.rife2:bld-junit-reporter
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.

## JUnit Failure Reports

To display JUnit failure reports, add the following to your `build` file:

```java
@BuildCommand(summary = "Runs the JUnit reporter")
public void reporter() throws Exception {
    new JUnitReporterOperation()
            .fromProject(this)
            .failOnSummary(true)
            .execute();
}

@Override
public void test() throws Exception {
    var op = testOperation().fromProject(this);
    // Set the reports directory
    op.testToolOptions().reportsDir(new File("build/test-results/test/"));
    op.execute();
}
```

- [View Example](https://github.com/rife2/bld-junit-reporter/blob/main/example/src/bld/java/com/example/ExampleBuild.java)

Please check the [JUnitReporter documentation](https://rife2.github.io/bld-junit-reporter/rife/bld/extension/JUnitReporterOperation.html#method-summary)
for all available configuration options.

### Failures Summary

To display a summary of all failures after running the tests, run the following:

```console
./bld compile test
./bld reporter
```

The summary will look something like:

```console
--------------------------------------------------
JUnit Failures Summary
--------------------------------------------------

[1] com.example.ExampleTests (3 failures, 0.011s)
  - [1.1] verifyFail(String)[1] ([1] foo)
  - [1.2] verifyFail(String)[2] ([2] bar)
  - [1.3] verifyHelloFoo()
[2] com.example.MoreTests (2 failures, 0.001s)
  - [2.1] verifyMore(String)[3] ([3] qux)
  - [2.2] verifyMore(String)[4] ([4] quux)
```

### Test Group Failures

To display details about the failures in the first test group,
use the group index:

```console
./bld reporter --index=1
```

The output will look something like:

```console
--------------------------------------------------
[1] com.example.ExampleTests
--------------------------------------------------

[1.1] Test: verifyFail(String)[1]
    - Name: [1] foo
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <foo> but was: <Hello World!>
    - Time: 0.009s

[1.2] Test: verifyFail(String)[2]
    - Name: [2] bar
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <bar> but was: <Hello World!>
    - Time: 0.001s
```
### Failure Detail

To display details about the second failure in the first test group,
use the group and failure indices:

```console
./bld reporter --i=1.2
```

The output will look something like:

```console
--------------------------------------------------
[1] com.example.ExampleTests
--------------------------------------------------

[1.2] Test: verifyFail(String)[2]
    - Name: [2] bar
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <bar> but was: <Hello World!>
    - Time: 0.001s
    - Trace:
        org.opentest4j.AssertionFailedError: expected: <bar> but was: <Hello World!>
                at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
                at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
                at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
                at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
                at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
                at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
                ...
```

### All Failures

To display all failures, use the `--all` option:

```console
./bld reporter --all
```

```console
--------------------------------------------------                                                                                                                                  
[1] com.example.ExampleTests
--------------------------------------------------

[1.1] Test: verifyFail(String)[1]
    - Name: [1] foo
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <foo> but was: <Hello World!>
    - Time: 0.008s

[1.2] Test: verifyFail(String)[2]
    - Name: [2] bar
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <bar> but was: <Hello World!>
    - Time: 0.001s

[1.3] Test: verifyHelloFoo()
    - Name: verifyHelloFoo()
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <Hello Foo!> but was: <Hello World!>
    - Time: 0.001s

--------------------------------------------------
[2] com.example.MoreTests
--------------------------------------------------

[2.1] Test: verifyMore(String)[3]
    - Name: [3] qux
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <true> but was: <false>
    - Time: 0.0s

[2.2] Test: verifyMore(String)[4]
    - Name: [4] quux
    - Type: org.opentest4j.AssertionFailedError
    - Message:
        expected: <true> but was: <false>
    - Time: 0.0s
```
