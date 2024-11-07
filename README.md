# EAP MicroProfile test suite
 
A small standalone test suite for MicroProfile on WildFly/EAP.

## Supported MicroProfile version

Tests have been executed against WildFly and EAP XP versions supporting 
[MicroProfile 6.1 specs](https://microprofile.io/compatible/6-1/#overview) 

## Supported MicroProfile specs

Currently, the following MicroProfile specs are supported:

* [MicroProfile Config](https://github.com/eclipse/microprofile-config)
* [MicroProfile JWT RBAC](https://github.com/eclipse/microprofile-jwt-auth)
* [MicroProfile Health](https://github.com/eclipse/microprofile-health)
* [MicroProfile Fault Tolerance](https://github.com/eclipse/microprofile-fault-tolerance)
* [MicroProfile OpenAPI](https://github.com/eclipse/microprofile-open-api)

## Observability

Although mostly related to MicroProfile platform integration, the test suite contains observability related tests, 
too, which verify the `Micrometer` integration into WildFly/EAP.

* [Micrometer](./micrometer) 

## Requirements for the testsuite
- JDK 11
- Docker
 
Maven is not a hard requirement as `mvnw` is delivered with this repository.

Podman can be aliased to be used as a Docker alternative - i.e. `alias docker=podman`, or you can pass the 
`-Ddocker.command=podman` system property to the test suite when executing Maven.

## Modules

### MicroProfile specs testing modules
There's one module per each supported MicroProfile specs, see [Supported MicroProfile specs](#supported-microprofile-specs).

### Tooling modules
- [tooling-cpu-load](./tooling-cpu-load) - utilities to support performance testing as the 
  [microprofile-fault-tolerance  `CpuLoadTest`](./microprofile-fault-tolerance/src/test/java/org/jboss/eap/qe/microprofile/fault/tolerance/CpuLoadTest.java)
- [tooling-docker](./tooling-docker) - utilities needed to run and manage `docker` as a part a given test scenario,
  e.g.: the 
  [microprofile-fault-tolerance `DatabaseCrashTest`](./microprofile-fault-tolerance/src/test/java/org/jboss/eap/qe/microprofile/fault/tolerance/DatabaseCrashTest.java)
- [tooling-mp-jwt-auth-tool](./tooling-mp-jwt-auth-tool) - utilities required by security operations, e.g.: keys' 
  generation utilities, which are used by the [microprofile-jwt](./microprofile-jwt) module
- [tooling-server-configuration](./tooling-server-configuration) - server configuration utilities, see the module [README](./tooling-server-configuration/README.md)
- [tooling-observability](./tooling-observability) - tooling which is used for Observability related tasks, e.g.: 
  Jaeger or OTel Docker containers

## Run the testsuite
Just execute:
```shell
mvn clean verify
```
or
```shell
./mvnw clean verify
```

### Bootable Jar
You might want to use profile `bootablejar.profile` (activation property `ts.bootable`) to test with bootable jar.
In case you need to provide a channel manifest for testing with bootable jar, use properties `channel-manifest.*`, e.g.:
```asciidoc
-Dchannel-manifest.groupId=org.jboss.eap.channels 
-Dchannel-manifest.artifactId=eap-8.0-plus-eap-xp-5.0 
-Dchannel-manifest.version=1.0.1.GA-redhat-20240304
```
which implicitly activate profile `bootablejar.profile.channels`;

## Export testing deployments
Test deployments can be 
[exported by Arquillian](https://arquillian.org/guides/getting_started_rinse_and_repeat/#export_the_deployment):  
```shell
mvn clean verify -Darquillian.deploymentExportPath=target/deployments/
```
or
```shell
./mvnw clean verify -Darquillian.deploymentExportPath=target/deployments/
```

## Run one module against custom build
The test suite can be used to test custom WildFly and EAP builds, just execute something like:
```shell
mvn clean verify -pl microprofile-health -am -Djboss.dist=$HOME/projects/wildfly-27.0.0.Beta1-SNAPSHOT
```
or
```shell
./mvnw clean verify -pl microprofile-health -am -Djboss.dist=$HOME/projects/wildfly-27.0.0.Beta1-SNAPSHOT
```

## Run one module against running server
The test suite can be used to execute against a running instance of WildFly or EAP, just start the server in advance 
and type:
```shell
mvn clean verify -pl microprofile-health -am
```
or
```shell
./mvnw clean verify -pl microprofile-health -am
```
Please note `allowConnectingToRunningServer` property in `arquillian.xml`, which is the feature that supports this very 
use case.

## Run the testsuite on Jenkins instance using Jenkinsfile:

- Follow instructions on <https://jenkins.io/doc/book/installing/> to install and run Jenkins
- Install [Pipeline](https://plugins.jenkins.io/workflow-aggregator/) and 
  [AnsiColor](https://plugins.jenkins.io/ansicolor/) plugins - see <https://jenkins.io/doc/book/managing/plugins/> for 
  more details
- Create a new `pipeline` job (`New Item` -> Choose `Pipeline` option and enter job name `microprofile-test-suite`)
- Copy content of [Jenkinsfile](./Jenkinsfile) into Pipeline script section and `Save`
    + Change `linux` label in `agent { label 'linux' }` to reflect the label assigned to your Jenkins nodes
    + Change `oracle-java-11` in `jdk 'oracle-java-11'` to a label matching an installed JDK
- Trigger the build by `Build Now` button - 
  NOTE FIRST RUN WILL FAIL DUE TO ISSUES <https://issues.jenkins-ci.org/browse/JENKINS-40574/> 
  and <https://issues.jenkins-ci.org/browse/JENKINS-41929> 
  but all the subsequent runs will work

## Quick compilation of the code
The following command can be used to ensure that code changes are compilable, `-Djboss.dist=foo` is just a workaround 
to skip unpacking the WildFly zip.
```shell
mvn clean verify -DskipTests -DskipITs -Djboss.dist=foo
```
or
```shell
./mvnw clean verify -DskipTests -DskipITs -Djboss.dist=foo
```

## Zip distribution bundle
The distribution bundle contains this testsuite, a pre-loaded local Maven repository and a dump of Docker images used in 
tests.
Generating the `eap-microprofile-test-suite-dist.zip` bundle is done via the `./mp-ts.sh` script.
This script assumes that all the necessary artifacts are present in `${user.home}/.m2/repository/`, in other words that 
the TS was compiled and executed on the local machine prior the bundle creation.

To create the distribution bundle just run:
```
./mp-ts.sh dist-zip
```

Execution on target machine:
```
unzip -q eap-microprofile-test-suite-dist.zip
./mp-ts.sh dist-run
```
Tests output is redirected into `${TEST_FQCN}-output.txt` files in `target/surefire-reports/` directory.

For more details have a look to the `mp-ts.sh` script

## Code format and imports order
Code gets formatted and imports ordered during the build time (process-sources phase) automatically:

```shell
mvn process-sources
```
or
```shell
./mvnw process-sources
```

## Tagging and branching strategy

#### Version policy

Naming convention: `$MAJOR.$MINOR.$MICRO.Final-SNAPSHOT` (`1.0.0.Final-SNAPSHOT`)

Start with `1.0.0.Final`

#### Tagging policy

Naming convention: `$MAJOR.$MINOR.$MICRO.Final` (`1.0.0.Final`)

###### When?

New WildFly major version is released and covers a MicroProfile update

#### Branching policy

Naming convention: `$MAJOR.$MINOR.z` (`1.0.z`)

###### When?

No branches unless really needed

### How to release a new version

```
mvn release:prepare release:clean
```

In order to skip tests run following command:

```
mvn release:prepare release:clean -Darguments=-DskipTests
```

The above will allow you to define a release, a tag and a next development version name in interactive way.

## Issues found by the testsuite
Any issue found in WildFly/EAP release will be reported to the Red Hat issue tracker, i.e.: <https://issues.redhat.com/>. 
The following Jira projects exist for different goals:
* https://issues.redhat.com/projects/WFWIP - for issues in unreleased WildFly version (like WF dev branches)
* https://issues.redhat.com/projects/WFLY - for released WildFly versions
* https://issues.redhat.com/projects/JBEAP - for released EAP versions

Every reported issue must contain:
* Title (affected MP spec may be part of the title, proper Component must be set)
* Type: Bug
* Component: related Microprofile component (`mp-health`)
* Affected Version - WildFly/EAP version (for WFLY/JBEAP issues)
* Target Version (when present) 
* Description of the issue
  * provide high-level description of the issue
  * information about tested WildFly/EAP build (or used commit/branch)
  * impact on the user
  * test scenario (actual and expected result)
  * add logs/stacktraces
  * automatic reproducer (if possible)
