## Orangebeard.io FitNesse TestSystemListener

A test output listener for Praegus Open Source Toolchain FitNesse tests.

### Configuration
Create `reportportal.properties` in your project's resource folder, containing:
 - rp.endpoint = https://yourinstance.orangebeard.app
 - rp.uuid = your_uuid_in_orangebard
 - rp.launch = launch_name
 - rp.project = project_name
 
 Configure your project to use the OrangebeardJunitRunner (when running from junit)
 
### Limitations
 - Currently, this runner/listener can not be used when you use >1 testsystem (i.e. Fit and Slim in one suite)
 - Full HTML report zip (and other attachments) will not be saved when its size is exceeds 1 MB due to an api limitation