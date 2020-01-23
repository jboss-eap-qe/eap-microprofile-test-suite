# Module description
Module contains classes for configuring and managing server.

## Creaper

Creaper core library [link](https://github.com/wildfly-extras/creaper) is meant to be used to configure and manage server.
To get `OnlineManagementClient` you can use `ManagementClientProvider` class.
The class reads Arquillian configuration autimatically.

## Configure server

To achieve desired configuration of server you can implement `MicroProfileServerSetupTask` that extends `ServerSetupTask` interface and annotate testclass with `@ServerSetup` annotation.
The setup task in invoked after Arquillian observers and before a deployment is deployed.
The interface provides two methods:
* setup(): implement desired changes
* teardown(): restore and rollback changes

If you need to configure a server see [Creaper section](#Creaper) to get a instance of `OnlineManagementClient`

## Logging

Module also contains tooling for checking server log files - `ModelNodeLogChecker`.