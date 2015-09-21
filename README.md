## Introduction

JRuyi is a Java framework for easily developing **efficient**, **scalable** and **flexible** network applications.  It hides the Java socket API by providing an event-driven asynchronous API over various transports, such as TCP and UDP, via [Java NIO](http://en.wikipedia.org/wiki/New_I/O).

The key features of JRuyi are listed as follows.

#### Modularity
JRuyi is built on OSGi framework which is a dynamic module system and service platform.

#### Service Oriented
JRuyi is an OSGi based framework; its functionality is mainly provided through services.

#### Asynchronism
JRuyi provides an event-driven asynchronous IO framework.

#### Performance
High throughput, low latency; provides a thread-local cache mechanism to avoid frequent creation of large objects such as buffers; provides chained buffers to minimize unnecessary memory copy.

#### TCP Connection Pooling and Multiplexing
Provides an efficient IO service with TCP connection pooling and multiplexing.

#### Extensible Command Line
A command-line/shell backed by Apache Felix Gogo Runtime. New commands can be easily added via OSGi services.

#### Dynamic Configuration
Configurations can be created and updated dynamically through ruyi-cli.

#### Hot Deployment
Bundles can be installed, updated, started, stopped and uninstalled on the fly through ruyi-cli.

## License

JRuyi is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Further Reading

* What's [Ruyi](http://en.wikipedia.org/wiki/Ruyi_\(scepter\))?
* [JRuyi Website](http://www.jruyi.org)
