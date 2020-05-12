# Orangebeard.io FitNesse TestSystemListener

A test output listener for Praegus Open Source Toolchain FitNesse tests.

## Installation

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
 @RunWith(ReportPortalEnabledToolchainTestRunner.class)
 ````
 in the test that is started from your pipeline (which is probably src/test/java/.../FixtureDebugTest.java)
 
### Limitations
 - Currently, this runner/listener can not be used when you use >1 testsystem (i.e. Fit and Slim in one suite)
 - Full HTML report zip (and other attachments) will not be saved when its size is exceeds 1 MB due to an api limitation
