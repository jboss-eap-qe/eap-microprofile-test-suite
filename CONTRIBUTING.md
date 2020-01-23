## Contributing
Standard PR workflow - [link](https://www.atlassian.com/git/tutorials/comparing-workflows/forking-workflow)

## Structure of the testsuite
Group all tests for given MP spec into single module
* for example to group all tests for MP Metric together
* in case that tests for integration of multiple MP specs are developed and it's not clear to which module they belong then new module can be created. Preferable a name of such module should reflect MP specs.

Tooling should be in separate module so it is isolated and easily used by other (test) modules.
Try to add tooling to existing module.
If there is no module that reflect tooling purpose you can create new module.
The name **must** include `tooling-` prefix.

Rather than add new dependency try to extend existing tooling or add new one yet still consider the trade-off.

Examples of available tooling:
* tooling-cpu-load
* tooling-docker
* tooling-server-configuration

**In order to configure and manage server you need to use tooling-server-configuration and Creaper core library, see [the module description](tooling-server-configuration/README.md)**

## Test documentation
For documentation and test plan purposes use following annotations for each test to properly document its purpose, passing criteria and version of EAP since a test is valid from:
```
/**
 * @tpTestDetails __PUT TEST DETAILS__
 * @tpPassCrit __PUT PASSING CRITERIA__
 * @tpSince __VERSION OF WILDFLY/EAP WITH A FEATURE__
*/
```

## Coding conventions
To enforce common codestyle the testsuite uses formatter plugin which automatically formats a code. To run it manually run `mvn -B formatter:validate impsort:check`.

## Another best practices
To get arquillian properties such ach server address and so on, use:
```
ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
arqProps.getDefaultManagementAddress();
```

To validate server functionality via REST point use restassured library:
* if possible import statically `get()`, `given()`,... method
* use one `baseUri()` with fully-qualified URL instead of
```
.baseUri(...)
.port(...)
.basePath(...)
```