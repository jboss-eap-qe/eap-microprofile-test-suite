# eap-microprofile-test-suite
 a small standalone test suite for MicroProfile on WF/EAP

## Requirements for the testsuite
 - JDK 8+
 - Docker
 
 Maven is not a hard requirement as `mvnw` is delivered with this repository.

 Podman with docker command bridge can be used as a Docker alternative.

## Run the testsuite
```
mvn clean verify

./mvnw clean verify
```

## Export testing deployments
```
mvn clean verify -Darquillian.deploymentExportPath=target/deployments/

./mvnw clean verify -Darquillian.deploymentExportPath=target/deployments/
```

## Run one module against custom build
```
mvn clean verify -pl microprofile-health -am -Djboss.dist=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final

./mvnw clean verify -pl microprofile-health -am -Djboss.dist=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final
```

## Run one module against running server
```
mvn clean verify -pl microprofile-health -am

./mvnw clean verify -pl microprofile-health -am
```
Please note `allowConnectingToRunningServer` property in `arquillian.xml`.

## Run tests against manual-mode Arquillian container

Tests that should be executed against manually managed Aqruillian containers are implemented in 
`microprofile-manual-mode` module.

Of course the above mentioned tests can only be executed against a non-running server instance, as it is supposed to be 
started by test classes:

```
mvn clean verify -pl microprofile-manual-mode -am -Djboss.dist=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final

./mvnw clean verify -pl microprofile-manual-mode -am -Djboss.dist=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final
```

## Run the testsuite on Jenkins instance using Jenkinsfile:

- Follow instructions on <https://jenkins.io/doc/book/installing/> to install and run Jenkins
- Install `Pipeline` and `AnsiColor` plugins - see https://jenkins.io/doc/book/managing/plugins/ for more details
- Create a new `pipeline` job (`New Item` -> Choose `Pipeline` option and enter job name `microprofile-test-suite`)
- Copy content of Jenkinsfile into Pipeline script section and `Save`
    - Change `linux` label in `agent { label 'linux' }` to contain label assigned to your slaves
    - Change `java-1.8.0-oracle` in `jdk 'java-1.8.0-oracle'` to installed JDK label
- Trigger build by `Build Now` button - NOTE FIRST RUN WILL FAIL DUE TO ISSUES <https://issues.jenkins-ci.org/browse/JENKINS-40574/> <https://issues.jenkins-ci.org/browse/JENKINS-41929> but all the subsequent runs will work

## Quick compilation of the code
Can be used to ensure code changes are compilable, `-Djboss.dist=foo` is workaround to skip unpacking of WildFly zip.
```
mvn clean verify -DskipTests -DskipITs -Djboss.dist=foo

./mvnw clean verify -DskipTests -DskipITs -Djboss.dist=foo
```

## Zip distribution bundle
Distribution bundle contains this testsuite, pre-loaded local maven repository and dump of Docker images used in tests.
Creation of the `eap-microprofile-test-suite-dist.zip` bundle is managed via `./mp-ts.sh` script.
This script assumes that all the necessary artifacts present in `${user.home}/.m2/repository/`, in other words that the TS was compiled and executed on local machine prior the bundle creation.

Bundle creation:
```
./mp-ts.sh dist-zip
```

Execution on target machine:
```
unzip -q eap-microprofile-test-suite-dist.zip
./mp-ts.sh dist-run
```
Tests output is redirected into `${TEST_FQCN}-output.txt` files in `target/surefire-reports/` directory.

For more details consult with `mp-ts.sh` script

## Code format and imports order
Code gets formatted and imports ordered during the build time (process-sources phase) automatically.

```
mvn process-sources

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

With commands you are able to define a release, a tag and a next development version name in interactive way.

## Issues found by the testsuite
Any issue found in WildFly/EAP release will be reported into the Red Hat issue tracker: https://issues.redhat.com/ . There are the following Jira projects:
* https://issues.redhat.com/projects/WFWIP - for issues in unreleased WildFly version (like WF dev branches)
* https://issues.redhat.com/projects/WFLY - for released WildFly versions
* https://issues.redhat.com/projects/JBEAP - for released EAP versions

Every reported issue must contain:
* Title (affected MP spec may be part of the title, proper Component must be set)
* Type: Bug
* Component: related Microprofile component (`mp-health`)
* Affected Version - WildFly/EAP version (for WFLY/JBEAP issues)
* Description of the issue
  * provide high-level description of the issue
  * information about tested WF/EAP build (or used commit/branch)
  * impact on the user
  * test scenario (actual and expected result)
  * add logs/stacktraces
  * automatic reproducer (if possible)
