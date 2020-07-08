# Orangebeard.io FitNesse TestSystemListener

A test output listener for Praegus Open Source Toolchain FitNesse tests.

## Installation
[![Maven Central](https://img.shields.io/maven-central/v/io.orangebeard/fitnesse-toolchain-listener.svg?maxAge=3600)](https://mvnrepository.com/artifact/io.orangebeard/allure-fitnesse-toolchain-listener)

 ### 1. Add dependency
 Add this project as a dependency to your pom
 ```xml
 <dependency>
     <groupId>io.orangebeard</groupId>
     <artifactId>fitnesse-toolchain-listener</artifactId>
     <version>version</version>
 </dependency>
 ```

### Configuration
Create `reportportal.properties` in your project's resource folder, containing:
 - rp.endpoint = https://yourinstance.orangebeard.app
 - rp.uuid = your_uuid_in_orangebard
 - rp.launch = launch_name
 - rp.project = project_name
 
For more optional parameters, see: https://github.com/reportportal/client-java/blob/master/README.md
Async reporting is working with this logger.

 
 #### Environment properties
  Properties can also be set in the build, by passing them to the maven build. For example: 
  ```
  mvn clean test-compile ... -Drp.tags=Jenkins;Chrome -Drp.launch=small-regression
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
 in the test that is started from your pipeline (which is probably src/test/java/.../FixtureDebugTest.java)
 
### Limitations
 - Currently, this runner/listener will start separate launches for each testsystem if you use >1 testsystem (i.e. Fit and Slim)in one suite
 - Full HTML report zip (or any other attachments) will not be saved when its size is exceeds 1 MB due to an api limitation
