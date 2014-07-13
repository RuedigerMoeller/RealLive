const RL_UPDATE    = 0;
const RL_ADD       = 1;
const RL_REMOVE    = 2;
const RL_OPERATION = 3;
const RL_SNAPSHOT_DONE = 4;
const RL_ERROR = 5;

function RLResultSet() {
    this.map = {};
    this.list = [];

    this.push = function(change) {
        switch ( change.type ) {
            case RL_ADD: {
                var rec = change.newRecord;
                this.map[rec.recordKey] = rec;
                this.list.push(rec);
                rec._rlIdx = this.list.length-1;
            } break;
            case RL_REMOVE: {
                var rec = this.map[change.recordKey];
                if ( rec !== 'undefined') {
                    this.list.splice(rec._rlIdx,1);
                }
            } break;
            case RL_UPDATE: {
                var rec = this.map[change.recordKey];
                console.log(rec);
            } break;
        }
    }
}

var RealLive = new function() {

    // currently active websocket
    this.ws = null;
    this.socketConnected = false;
    this.model = null; // RealLive data model
    this.onChange = null; // callback function
    this.toDo = [];

    this.onModelLoaded = function(func) {
        if ( this.socketConnected && this.model ) {
            func.apply();
        } else {
            this.toDo.push(func);
        }
    };

    this.runToDos = function() {
        for ( var i = 0; i < this.toDo.length; i++ ) {
            this.toDo[i].apply();
        }
        this.toDo = [];
    };

    this.sendChange = function(what) {
        if ( this.onChange ) {
            this.onChange(what);
        }
    };

    this.doConnect = function (host,port,websocketDir) {
        var _this = this;

        this.ws = new WebSocket("ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir));
        this.ws.cbId = 1;
        this.ws.cbMap = {};
        this.ws.onopen = function () {
            console.log("open");
            _this.socketConnected = true;
            _this.call("initModel", 0, function(retVal) {
                console.log("model:"+retVal);

                retVal.tables.SysTable.columns.meta.hidden = true;
                // for ng table
                for ( conf in retVal.tables ) {
                    console.log(conf);
                    if ( conf != '__typeInfo' ) {
                        var colConf = [];
                        retVal.tables[conf].columnsNGTableConf = colConf;
                        var cols = _this.visibleColumns(retVal.tables[conf].columns);
                        for ( col in  cols ) {
                            if ( col != '__typeInfo' && ! col.hidden ) {
    //                                    colConf.push( { fieldName: cols, displayName: cols[col].displayName } )
                                colConf.push( { field: cols[col].name, displayName: cols[col].displayName } );
                            }
                        }
                    }
                }

                _this.model = retVal;
                _this.sendChange('ModelLoaded');
                _this.runToDos();
            });
        };

        this.ws.onerror = function () {
            console.log("error");
            _this.socketConnected = false;
            _this.sendChange('State');
        };

        this.ws.onclose = function () {
            console.log("closed");
            _this.socketConnected = false;
            _this.sendChange('State');
        };

        this.ws.onmessage = function (message) {
            var _thisWS = this;
            var fr = new FileReader();
            if ( typeof message.data == 'string' ) {
            } else {
                fr.onloadend = function (event) {
                    try {
                        var msg = MinBin.decode(event.target.result);
                        if (msg instanceof JInvocationCallback) {
                            var cb = _thisWS.cbMap[msg.cbId];
                            if (typeof cb === "function") {
                                var unsubscribe = cb.call(null, msg.result);
                                if (unsubscribe || msg.cbId.substring(0, 2) == 'mc') { // single shot
                                    delete _thisWS.cbMap[msg.cbId];
                                }
                            } else {
                                console.log("unmapped callback " + msg.cbId + " " + msg);
                            }
                        }
//                    var strMsg = MinBin.prettyPrint(msg);
//                    // handle message
                    } catch (ex) {
                        console.log(ex)
                    }
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
    };

    this.unsubscribe = function( cbId ) {
        delete this.ws.cbMap[cbId];
    };

    // call streaming, callback must return true to unsubscribe
    this.callStreaming = function( methodName, arg, callback ) {
        this.call(methodName,arg,callback,true);
    };

    this.querySet = function( queryId, resultset, scope ) {
        this.callStreaming("streamTable", queryId, function(change) {
            console.log(change);
            resultset.push(change);
            if ( change.type == RL_SNAPSHOT_DONE ) {
                scope.$apply(function () {});
                return true;
            }
            return false;
        },true);
    };

    this.call = function( methodName, arg, callback, stream ) {
        var msg = MinBin.encode(
            new JInvocation({
                "name" : methodName,
                "argument" : arg,
                "cbId" : typeof callback === "undefined" ? "0" : (stream ? 'st':'mc').concat(this.ws.cbId)
            })
        );
        if ( ! (typeof callback === "undefined") ) {
            if ( ! stream ) {
                this.ws.cbMap['mc'.concat(this.ws.cbId)] = callback;
            } else {
                this.ws.cbMap['st'.concat(this.ws.cbId)] = callback; // stream
            }
            this.ws.cbId++;
        }
        this.ws.send(msg);
    };

    this.visibleColumns = function( columns ) {
        var result = [];
        for( key in columns ) {
            if ( columns.hasOwnProperty(key) ) {
                var value = columns[key];
                if (!value['hidden']) {
                    value._key = key;
                    result.push(value);
                }
            }
        }
        result.sort(function (a,b) { return a.order- b.order; });
        return result;
    };

};