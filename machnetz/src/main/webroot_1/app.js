var app = angular.module("rl-admin", ['ui.bootstrap'])

app.controller('RLAdmin', function ($scope) {

    $scope.host = 'localhost';
    $scope.port = '8887';
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.systables = [];

    $scope.unsubscribe = function( cbId ) {
        delete this.ws.cbMap[cbId];
    };

    $scope.subscribe = function( methodName, arg, callback ) {
        this.call(methodName,arg,callback,true);
    };

    $scope.call = function( methodName, arg, callback, stream ) {
        var msg = MinBin.encode(
            new JInvocation({
                "name" : methodName,
                "argument" : arg,
                "cbId" : typeof callback === "undefined" ? "0" : (stream ? 'st':'mc').concat($scope.ws.cbId)
            })
        );
        if ( ! (typeof callback === "undefined") ) {
            if ( ! stream ) {
                $scope.ws.cbMap['mc'.concat($scope.ws.cbId)] = callback;
            } else {
                $scope.ws.cbMap['st'.concat($scope.ws.cbId)] = callback; // stream
            }
            $scope.ws.cbId++;
        }
        $scope.ws.send(msg);
    };

    $scope.doConnect = function () {
        var ws = new WebSocket("ws://".concat($scope.host).concat(":").concat($scope.port).concat("/").concat($scope.websocketDir));
        ws.cbId = 1;
        ws.cbMap = {};
        ws.onopen = function () {
            console.log("open");
            $scope.$apply(function () {
                $scope.socketConnected = true;
                $scope.call("initModel", 0, function(retVal) {
                    console.log("Hallo:"+retVal);
                });
                $scope.subscribe("streamTables", "", function(msg) {
                    console.log(msg);
                    if ( msg.type == 1 ) // add
                    {
                        $scope.systables.push(msg.newRecord);
                    }
                    else if ( msg.type == 4 ) { // snap fin FIXME: add constant
                        return true;
                    }
                    return false;
                });
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
        ws.receiveQueue = [];
        ws.onmessage = function (message) {
            var fr = new FileReader();
            if ( typeof message.data == 'string' ) {
                $scope.$apply(function () {
//                    $scope.resptext = message.data;
                });
            } else {
//                this.receiveQueue.push(message);
                fr.onloadend = function (event) {
                    try {
                        var msg = MinBin.decode(event.target.result);
                        if (msg instanceof JInvocationCallback) {
                            var cb = $scope.ws.cbMap[msg.cbId];
                            if (typeof cb === "function") {
                                var unsubscribe = cb.call(null, msg.result);
                                if (unsubscribe || msg.cbId.substring(0, 2) == 'mc') { // single shot
                                    delete $scope.ws.cbMap[msg.cbId];
                                }
                            } else {
                                console.log("unmapped callback " + msg.cbId + " " + msg);
                            }
                        }
//                    var strMsg = MinBin.prettyPrint(msg);
                        $scope.$apply(function () {
                            // handle message
                        });
                    } catch (ex) {
                        console.log(ex)
                    }
//                    if ( ws.receiveQueue.length > 0 ) {
//                        var nextMsg = ws.receiveQueue.shift();
//                        ws.onmessage(nextMsg);
//                    }
                };
                // error handling is missing
//                if ( this.receiveQueue.length == 1 )
//                fr.readAsArrayBuffer(this.receiveQueue.shift().data);
                fr.readAsArrayBuffer(message.data);
            }
        };
        $scope.ws = ws;
    };

});
