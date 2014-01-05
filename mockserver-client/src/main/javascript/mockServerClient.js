var mockServer = mockServer || {};

(function (baseUrl) {
    "use strict";

    mockServer.factory = function (testIdQueryString, baseUrl) {
        var xmlhttp = new XMLHttpRequest(),
            defaultHeaders = [
                {"name": "Content-Type", "values": ["application/json; charset=utf-8"]},
                {"name": "Cache-Control", "values": ["no-cache, no-store"]}
            ],
            createExpectation = function (path, responseBody, statusCode) {
                var headers = [];
                if (testIdQueryString) {
                    headers = [
                        {
                            "name": "Referer",
                            "values": [".*" + testIdQueryString + ".*"]
                        }
                    ];
                }
                return {
                    httpRequest: {
                        method: "",
                        path: path,
                        body: "",
                        headers: headers,
                        cookies: [],
                        parameters: []
                    },
                    httpResponse: {
                        statusCode: statusCode || 200,
                        body: JSON.stringify(responseBody),
                        cookies: [],
                        headers: defaultHeaders,
                        delay: {
                            timeUnit: "MICROSECONDS",
                            value: 0
                        }
                    },
                    times: {
                        remainingTimes: 1,
                        unlimited: false
                    }
                };
            },
            mockResponse = function (path, responseBody, statusCode) {
                var expectedResponse = createExpectation(path, responseBody, statusCode);
                xmlhttp.open("PUT", baseUrl, false);
                xmlhttp.setRequestHeader("Content-Type", "application/json; charset=utf-8");
                xmlhttp.send(JSON.stringify(expectedResponse));
            },
            clearMock = function () {
                xmlhttp.open("PUT", baseUrl + "/clear", false);
                xmlhttp.send(JSON.stringify(createExpectation(".*", "")));
            },
            dumpToLog = function () {
                xmlhttp.open("PUT", baseUrl + "/dumpToLog", false);
                xmlhttp.send(JSON.stringify(createExpectation(".*", "")));
            },
            setDefaultHeaders = function (headers) {
                defaultHeaders = headers;
            };

        return {
            mockResponse: mockResponse,
            clearMock: clearMock,
            dumpToLog: dumpToLog,
            setDefaultHeaders: setDefaultHeaders
        };
    };

    // ensure each test runs with an isolated set of mocks
    var testIdQueryString;
    if (window.location.href.match(/testId\=.*/g)) {
        testIdQueryString = window.location.href.match(/testId\=.*/g)[0];
    }
    mockServer.client = mockServer.factory(testIdQueryString || "testId=" + Math.floor(Math.random() * 0x100000000000).toString(16), baseUrl);

}());
