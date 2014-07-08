var app = angular.module("rl-admin", ['ui.bootstrap'])

app.controller('RLAdmin', function ($scope) {

    $scope.host = 'localhost';
    $scope.port = '8887';
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.call = function( methodName, arg, callback ) {
        var msg = MinBin.encode(
            new JInvocation({
                "name" : methodName,
                "argument" : arg,
                "cbId" : typeof callback === "undefined" ? "0" : 'mc'.concat($scope.ws.cbId)
            })
        );
        if ( ! (typeof callback === "undefined") ) {
            $scope.ws.cbMap['mc'.concat($scope.ws.cbId)] = callback;
            $scope.ws.cbId++;
        }
        $scope.ws.send(msg);
    };

    $scope.doConnect = function () {
        var ws = new WebSocket("ws://".concat($scope.host).concat(":").concat($scope.port).concat("/").concat($scope.websocketDir));
        ws.cbId = 1;
        ws.cbMap = [];
        ws.onopen = function () {
            console.log("open");
            $scope.$apply(function () {
                $scope.socketConnected = true;
                $scope.call("initModel", 0, function(retVal) {
                    console.log("Hallo:"+retVal);
                })
            });
        };
        ws.onerror = function () {
            console.log("error");
            $scope.$apply(function () {
                $scope.socketConnected = false;
            });
        };
        ws.onclose = function () {
            console.log("closed");
            $scope.$apply(function () {
                $scope.socketConnected = false;
            });
        };
        ws.onmessage = function (message) {
            var fr = new FileReader();
            if ( typeof message.data == 'string' ) {
                $scope.$apply(function () {
//                    $scope.resptext = message.data;
                });
            } else {
                fr.onloadend = function (event) {
                    var msg = MinBin.decode(event.target.result);
                    if ( msg instanceof JInvocationCallback ) {
                        var cb = $scope.ws.cbMap[msg.cbId];
                        if ( typeof cb === "function" ) {
                            cb.call(null,msg.result);
                            if ( msg.cbId.substring(0,2) == 'mc' ) { // single shot
                                delete $scope.ws.cbMap[msg.cbId];
                            }
                        } else {
                            console.log("unmapped callback "+msg.cbId+" "+msg);
                        }
                    }
//                    var strMsg = MinBin.prettyPrint(msg);
                    $scope.$apply(function () {
                        // handle message
                    });
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
        $scope.ws = ws;
    };

});
