## Introduction
JRuyi is a Java framework for easily developing efficient, scalable and flexible network applications.  It hides the Java socket API by providing an event-driven asynchronous API over various transports such as TCP and UDP via Java NIO.

Besides, an in-memory messaging framework (JRuyi Messaging Engine) is provided to help to develop a network application with more pluggability, more flexibility and more throughput.

Last but not least, JRuyi embraces OSGi as its foundation, which means it naturally inherits all the merits of OSGi.

### Essentials
* Modularity – JRuyi is built on OSGi platform which is a dynamic module system.
* Service Oriented – JRuyi uses OSGi services.
* Asynchronism – JRuyi provides an event-driven asynchronous IO framework and an in-memory asynchronous messaging framework.
* Performance – Higher throughput, lower latency; Provides a thread-local cache mechanism to avoid frequently creation of large objects such as buffers; Provides chained buffers to minimize unnecessary memory copy.

## License
JRuyi is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Further Reading
* What Is [Ruyi](http://en.wikipedia.org/wiki/Ruyi_(scepter\))?
* [OSGi](http://en.wikipedia.org/wiki/OSGi) Introduction
* Project [Wiki](https://github.com/jruyi/jruyi/wiki)

