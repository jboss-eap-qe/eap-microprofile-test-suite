# Module description
This module contains classes for configuring and managing the tested server instance.

## Creaper

Creaper core library [link](https://github.com/wildfly-extras/creaper) is meant to be used to actually perform the 
server instance configuration and management.
To get an `OnlineManagementClient` instance, you can use the `ManagementClientProvider` utility class.
The class reads Arquillian configuration automatically.

## Configure the tested server instance

To achieve the desired server configuration you'd most likely want to implement the `MicroProfileServerSetupTask` 
interface, which implements `ServerSetupTask`, and annotate a test class with the `@ServerSetup` annotation.
Setup tasks are executed after Arquillian observers, and before a deployment is actually deployed.

The interface provides two methods:
* setup(): implement desired changes
* teardown(): restore and rollback changes

When you need to configure, you'd typically request an `OnlineManagementClient` instance, as detailed in the 
[Creaper section](#Creaper).

## Logging

The module also contains tooling for checking server log files, i.e. `ModelNodeLogChecker`.