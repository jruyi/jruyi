## Introduction

JRuyi is a Java framework for easily developing efficient, scalable and flexible network applications.  It hides the Java socket API by providing an event-driven asynchronous API over various transports such as TCP and UDP via [Java NIO](http://en.wikipedia.org/wiki/New_I/O).

In addition, an in-memory messaging framework (JRuyi Messaging Engine) is provided to help to develop network applications with more pluggability, more flexibility and more throughput.

Last but not least, JRuyi embraces [OSGi](http://www.osgi.org/Technology/WhatIsOSGi) as its foundation, which means it naturally inherits all the [merits of OSGi](http://www.osgi.org/Technology/WhyOSGi).

### Essentials

* Modularity – JRuyi is built on OSGi framework which is a dynamic module system and service platform.

* Service Oriented – JRuyi is an OSGi based framework; its functionality is mainly provided through services.

* Asynchronism – JRuyi provides an event-driven asynchronous IO framework and an in-memory asynchronous messaging framework.

* Performance – Higher throughput, lower latency; provides a thread-local cache mechanism to avoid frequent creation of large objects such as buffers; provides chained buffers to minimize unnecessary memory copy.

## License

JRuyi is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Further Reading

* What's [Ruyi](http://en.wikipedia.org/wiki/Ruyi_(scepter\))?
* [JRuyi Website](http://www.jruyi.org)

