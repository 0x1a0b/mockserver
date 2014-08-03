describe("mockServerClient client:", function () {
    var xmlhttp;

    beforeEach(function () {
        xmlhttp = new XMLHttpRequest();
        mockServerClient("localhost", 1080).reset();
        proxyClient("localhost", 1090).reset();
    });

    it("should create full expectation with Base64 encoded body", function () {
        // when
        mockServerClient("localhost", 1080).mockAnyResponse(
            {
                'httpRequest': {
                    'method': 'POST',
                    'path': '/somePath',
                    'queryString': 'test=true',
                    'parameters': [
                        {
                            'name': 'test',
                            'values': [ 'true' ]
                        }
                    ],
                    'body': {
                        'type': "STRING",
                        'value': 'someBody'
                    }
                },
                'httpResponse': {
                    'statusCode': 200,
                    'body': JSON.stringify({ name: 'value' }),
                    'delay': {
                        'timeUnit': 'MILLISECONDS',
                        'value': 250
                    }
                },
                'times': {
                    'remainingTimes': 1,
                    'unlimited': false
                }
            }
        );

        // then - non matching request
        xmlhttp.open("GET", "http://localhost:1080/otherPath", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);

        // then - matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');

        // then - matching request, but no times remaining
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(404);
    });

    it("should create full expectation with string body", function () {
        // when
        mockServerClient("localhost", 1080).mockAnyResponse(
            {
                'httpRequest': {
                    'method': 'POST',
                    'path': '/somePath',
                    'queryString': 'test=true',
                    'parameters': [
                        {
                            'name': 'test',
                            'values': [ 'true' ]
                        }
                    ],
                    'body': {
                        'type': "STRING",
                        'value': 'someBody'
                    }
                },
                'httpResponse': {
                    'statusCode': 200,
                    'body': JSON.stringify({ name: 'value' }),
                    'delay': {
                        'timeUnit': 'MILLISECONDS',
                        'value': 250
                    }
                },
                'times': {
                    'remainingTimes': 1,
                    'unlimited': false
                }
            }
        );

        // then - non matching request
        xmlhttp.open("GET", "http://localhost:1080/otherPath", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);

        // then - matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');

        // then - matching request, but no times remaining
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(404);
    });

    it("should match on body only", function () {
        // when
        var client = mockServerClient("localhost", 1080);
        client.mockAnyResponse(
            {
                'httpRequest': {
                    'path': '/somePath',
                    'body': {
                        'type': "STRING",
                        'value': 'someBody'
                    }
                },
                'httpResponse': {
                    'statusCode': 200,
                    'body': JSON.stringify({ name: 'first_body' }),
                    'delay': {
                        'timeUnit': 'MILLISECONDS',
                        'value': 250
                    }
                },
                'times': {
                    'remainingTimes': 1,
                    'unlimited': false
                }
            }
        );
        client.mockAnyResponse(
            {
                'httpRequest': {
                    'path': '/somePath',
                    'body': {
                        'type': "REGEX",
                        'value': 'someOtherBody'
                    }
                },
                'httpResponse': {
                    'statusCode': 200,
                    'body': JSON.stringify({ name: 'second_body' }),
                    'delay': {
                        'timeUnit': 'MILLISECONDS',
                        'value': 250
                    }
                },
                'times': {
                    'remainingTimes': 1,
                    'unlimited': false
                }
            }
        );

        // then - non matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someIncorrectBody");

        expect(xmlhttp.status).toEqual(404);

        // then - matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"first_body"}');

        // then - matching request, but no times remaining
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someOtherBody");

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"second_body"}');
    });

    it("should create simple response expectation", function () {
        // when
        mockServerClient("localhost", 1080).mockSimpleResponse('/somePath', { name: 'value' }, 203);

        // then - non matching request
        xmlhttp.open("GET", "http://localhost:1080/otherPath", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);

        // then - matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(203);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');

        // then - matching request, but no times remaining
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(404);
    });

    it("should update default headers for simple response expectation", function () {
        // when
        var client = mockServerClient("localhost", 1080);
        client.setDefaultHeaders([
            {"name": "Content-Type", "values": ["application/json; charset=utf-8"]},
            {"name": "X-Test", "values": ["test-value"]}
        ]);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);

        // then - matching request
        xmlhttp.open("POST", "http://localhost:1080/somePath?test=true", false);
        xmlhttp.send("someBody");

        expect(xmlhttp.status).toEqual(203);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');
        expect(xmlhttp.getResponseHeader("X-Test")).toEqual("test-value");
    });

    it("should verify exact number of requests have been sent", function () {
        // given
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);

        // when
        client.verify(
            {
                'method': 'POST',
                'path': '/somePath',
                'body': 'someBody'
            }, 1, true);
    });

    it("should verify at least a number of requests have been sent", function () {
        // given
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);

        // when
        client.verify(
            {
                'method': 'POST',
                'path': '/somePath',
                'body': 'someBody'
            }, 1);
    });

    it("should fail when no requests have been sent", function () {
        // given
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);

        // when
        expect(function () {
            client.verify(
                {
                    'path': '/someOtherPath'
                }, 1);
        }).toThrow();
    });

    it("should fail when not enough exact requests have been sent", function () {
        // given
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);

        // when
        expect(function () {
            client.verify(
                {
                    'method': 'POST',
                    'path': '/somePath',
                    'body': 'someBody'
                }, 2, true);
        }).toThrow();
    });

    it("should fail when not enough at least requests have been sent", function () {
        // given
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePath', { name: 'value' }, 203);
        xmlhttp.open("POST", "http://localhost:1080/somePath", false);
        xmlhttp.send("someBody");
        expect(xmlhttp.status).toEqual(203);

        // when
        expect(function () {
            client.verify(
                {
                    'method': 'POST',
                    'path': '/somePath',
                    'body': 'someBody'
                }, 2);
        }).toThrow();
    });

    it("should clear expectations", function () {
        // when
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePathOne', { name: 'value' }, 200);
        client.mockSimpleResponse('/somePathOne', { name: 'value' }, 200);
        client.mockSimpleResponse('/somePathTwo', { name: 'value' }, 200);

        // then - matching request
        xmlhttp.open("GET", "http://localhost:1080/somePathOne", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');

        // when
        client.clear('/somePathOne');

        // then - matching request but cleared
        xmlhttp.open("GET", "http://localhost:1080/somePathOne", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);

        // then - matching request and not cleared
        xmlhttp.open("GET", "http://localhost:1080/somePathTwo", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}')
    });

    it("should reset expectations", function () {
        // when
        var client = mockServerClient("localhost", 1080);
        client.mockSimpleResponse('/somePathOne', { name: 'value' }, 200);
        client.mockSimpleResponse('/somePathOne', { name: 'value' }, 200);
        client.mockSimpleResponse('/somePathTwo', { name: 'value' }, 200);

        // then - matching request
        xmlhttp.open("GET", "http://localhost:1080/somePathOne", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(200);
        expect(xmlhttp.responseText).toEqual('{"name":"value"}');

        // when
        client.reset();

        // then - matching request but cleared
        xmlhttp.open("GET", "http://localhost:1080/somePathOne", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);

        // then - matching request but also cleared
        xmlhttp.open("GET", "http://localhost:1080/somePathTwo", false);
        xmlhttp.send();

        expect(xmlhttp.status).toEqual(404);
    });
});