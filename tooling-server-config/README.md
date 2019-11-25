# tooling-server-config
 Leverages [Creaper] library to perform basic server configuration tasks.

### Description 
Based on a factory pattern in order to build instances of `OnlineManagementClient` or
`OfflineManagementClient` instances provided by Creaper.

The factory behavior is defined by `ManagmentClientFactory` interface which is implemented
both by `StandaloneManagmentClientFactory` and `DomainManagmentClientFactory`, respectively able to provide
management clients for instances of Wildfly running in _standalone_ or _domain_ mode.

Common implementation for both the above mentioned classes is provided By `BaseManagementClientFactory`.

### Tests
Tests for this module aim at assessing that:
 - Wildfly is running 
 - Management client object instances:
    - are returned by Creaper
    - can execute common CLI commands like `whoami`
    - can execute administrative operations (e.g.: `reload`)
    - can add and remove extension/subsystem from server configuration
    
Common test behavior is provided by abstract base class `BaseManagmentClientTestCase`.

See test cases, e.g. ManagementClientAgainstStandaloneModeTest.  

Run tests with the following command, against a *running* Wildfly instance:
```
mvn clean verify -pl tooling-server-config -Dtest=ManagementClientAgainstStandaloneModeTest
```

or 

```
mvn clean verify -pl tooling-server-config -Dtest=ManagementClientAgainstDomainModeTest
```

