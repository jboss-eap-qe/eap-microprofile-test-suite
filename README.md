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
mvn clean verify -pl microprofile-health -Djboss.home=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final

./mvnw clean verify -pl microprofile-health -Djboss.home=/Users/rsvoboda/Downloads/wildfly-18.0.0.Final
```

## Run one module against running server
```
mvn clean verify -pl microprofile-health

./mvnw clean verify -pl microprofile-health
```
Please note `allowConnectingToRunningServer` property in `arquillian.xml`.

## Quick compilation of the code
Can be used to ensure code changes are compilable, `-Djboss.home=foo` is workaround to skip unpacking of WildFly zip.
```
mvn clean verify -DskipTests -DskipITs -Djboss.home=foo

./mvnw clean verify -DskipTests -DskipITs -Djboss.home=foo
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
