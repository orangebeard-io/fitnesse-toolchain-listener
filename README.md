# Orangebeard.io FitNesse TestSystemListener

A test output listener for Praegus Open Source Toolchain FitNesse tests.

<p align="center">
<img src="https://raw.githubusercontent.com/orangebeard-io/fitnesse-toolchain-listener/master/.github/logo.svg" alt="Orangebeard.io Java Client" height="200"><br />
  <a href="https://repo.maven.apache.org/maven2/io/orangebeard/fitnesse-toolchain-listener/">
    <img src="https://img.shields.io/maven-central/v/io.orangebeard/fitnesse-toolchain-listener.svg?maxAge=3600&style=flat-square"
      alt="MVN Version" />
  </a>
  <a href="https://github.com/orangebeard-io/fitnesse-toolchain-listener/actions">
    <img src="https://img.shields.io/github/workflow/status/orangebeard-io/fitnesse-toolchain-listener/release?style=flat-square"
      alt="Build Status" />
  </a>
  <a href="https://github.com/orangebeard-io/fitnesse-toolchain-listener/blob/master/LICENSE.txt">
    <img src="https://img.shields.io/github/license/orangebeard-io/fitnesse-toolchain-listener?style=flat-square"
      alt="License" />
  </a>
</p>

## Installation

[![Maven Central](https://img.shields.io/maven-central/v/io.orangebeard/fitnesse-toolchain-listener.svg?maxAge=3600)](https://mvnrepository.com/artifact/io.orangebeard/fitnesse-toolchain-listener)

### 1. Add dependency
Add this project as a dependency to your pom:
```xml
<dependency>
    <groupId>io.orangebeard</groupId>
    <artifactId>fitnesse-toolchain-listener</artifactId>
    <version>version</version>
</dependency>
```

### Configuration
Create `orangebeard.properties` in your project's test resource folder, containing:

```properties
orangebeard.endpoint=<ORANGEBEARD-ENDPOINT>
orangebeard.accessToken=<XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX>
orangebeard.project=<PROJECT_NAME>
orangebeard.testset=<TESTSET_NAME>

# optional
orangebeard.description=<DESCRIPTION>
orangebeard.attributes=key:value; value;
```

#### Environment properties
Properties can also be set in the build, by passing them to the maven build. For example:

```
mvn clean test-compile ... -Dorangebeard.attributes=Jenkins;Chrome -Dorangebeard.testset=small-regression
```
 
### Use the runner
Replace the
```java
@RunWith(ToolchainTestRunner.class)
```
with
```java
@RunWith(OrangebeardJunitRunner.class)
````

in the test that is started from your pipeline (which is probably `src/test/java/.../FixtureDebugTest.java`)
 
### Limitations
 - Currently, this runner/listener will start separate launches for each testsystem if you use >1 testsystem (i.e. Fit and Slim)in one suite
 - Full HTML report zip (or any other attachments) will not be saved when its size is exceeds 1 MB due to an api limitation