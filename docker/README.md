[MockServer](http://www.mock-server.com)
==========

### Dependencies

* [dockerfile/java](http://dockerfile.github.io/#/java)

### Installation

1. Install [Docker](https://www.docker.io/).

2. Download [trusted build](https://index.docker.io/u/jamesdbloom/mockserver/) from public [Docker Registry](https://index.docker.io/): `docker pull jamesdbloom/mockserver`

   (or build image from Dockerfile: `docker build -t="jamesdbloom/mockserver" http://raw.github.com/jamesdbloom/mockserver/master/docker/Dockerfile`)

### Usage

    docker run -d jamesdbloom/mockserver

### What is MockServer

MockServer is for mocking of any system you integrate with via HTTP or HTTPS (i.e. services, web sites, etc).

MockServer supports:

* mocking of any HTTP / HTTPS response when any request is matched (<a href="#mocking">see more below</a>)
* recording requests and responses to analyse how a system behaves (<a href="#proxying">see more below</a>)
* verifying which requests and responses have been sent as part of a test (<a href="#proxying">see more below</a>)

MockServer also provides an HTTP / HTTPS proxy that can be used to record requests / responses.

Once requests / responses have been recorded:

* They can be queried pragmatically to confirm what requests have been sent and what responses have been received.
 * This supports test assertions that confirm what requests the system-under-test has been sending during a test.
* They can be output in JSON format to log what request the system under has sent and what responses it received.
 * This is useful for analysing an existing system prior to writing automated tests.
 * This is also useful to simplify the creation of test setup code as the JSON output is in the MockServer expectation format.
* They can be output in human readable Java code format that can be pasted directly into a test setup method.
 * This is useful to simplify the creation of expectations in test setup methods as the code can be easily pasted directly into a test setup method and then modified as appropriate for the needs of the test scenario.

This docker container will run an instance of the MockServer on the following ports:

* serverPort 8080
* serverSecurePort 8090
* proxyPort 9080
* proxySecurePort 9090

For information on how to use the MockServer please see http://www.mock-server.com

### Issues

If you have any problems, please [check the project issues](https://github.com/jamesdbloom/mockserver/issues?state=open).

### Contributions

Pull requests are, of course, very welcome! Please read our [contributing to the project](https://github.com/jamesdbloom/mockserver/wiki/Contributing-to-the-project) guide first. Then head over to the [open issues](https://github.com/jamesdbloom/mockserver/issues?state=open) to see what we need help with. Make sure you let us know if you intend to work on something. Also, check out the [milestones](https://github.com/jamesdbloom/mockserver/issues/milestones) to see what is planned for future releases.

### Maintainers
* [James D Bloom](http://blog.jamesdbloom.com)
* [Samira Rabbanian](https://github.com/samirarabbanian)
