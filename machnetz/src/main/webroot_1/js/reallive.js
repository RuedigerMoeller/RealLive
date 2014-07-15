var RL_UPDATE    = 0;
var RL_ADD       = 1;
var RL_REMOVE    = 2;
var RL_OPERATION = 3;
var RL_SNAPSHOT_DONE = 4;
var RL_ERROR = 5;

function RLResultSet() {
    this.map = {};
    this.list = [];
    this.preChangeHook = null;
    this.snapFin = false;

    this.push = function(change) {
        if (this.preChangeHook) {
            this.preChangeHook.call(null,change,this.snapFin);
        }
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
            case RL_SNAPSHOT_DONE:
                this.snapFin = true;
                break;
            case RL_UPDATE: {
                var rec = this.map[change.recordKey];
                if ( rec ) {
                    RealLive.model.systables
                    var changeArray = change.appliedChange.fieldIndex;
                    for ( var i = 0; i < changeArray.length; i++ ) {
                        var fieldId = changeArray[i];
                        var newValue = change.appliedChange.newVal[i];
                        var fieldName = RealLive.getFieldName(change.tableId,fieldId);
                        rec[fieldName] = newValue;
                        //var oldValue = change.appliedChange.oldVal[i];
                        //var error = rec[fieldName] != oldValue;
                    }
                }
//                console.log(rec);
            } break;
        }
    }

    this.getChangedFieldNames = function(change) {
        var res = [];
        if (change.appliedChange) {
            var changeArray = change.appliedChange.fieldIndex;
            for ( var i = 0; i < changeArray.length; i++ ) {
                var fieldId = changeArray[i];
                res.push(RealLive.getFieldName(change.tableId,fieldId));
            }
        }
        return res;
    }
}

var RealLive = new function() {

    // currently active websocket
    this.ws = null;
    this.socketConnected = false;
    this.model = null; // RealLive data model
    this.onChange = null; // callback function
    this.toDo = [];

    this.getTableMeta = function(tableId,columnName) {
        var res = this.model.tables[tableId];
        if ( columnName ) {
            return res.columns[columnName];
        }
        return res;
    };

    this.getFieldName = function(tableId,fieldId) {
        return this.getTableMeta( tableId).fieldId2Name[fieldId];
    };

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
        var self = this;

        this.ws = new WebSocket("ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir));
        this.ws.cbId = 1;
        this.ws.cbMap = {};
        this.ws.onopen = function () {
            console.log("open");
            self.socketConnected = true;
            self.call("initModel", 0, function(retVal) {
                console.log("model:"+retVal);

                retVal.tables.SysTable.columns.meta.hidden = true;

                // setup column config for ng table
                for ( conf in retVal.tables ) {
                    console.log(conf);
                    if ( conf != '__typeInfo' ) {
                        var colConf = [];
                        var indexToFieldName = [];

                        retVal.tables[conf].columnsNGTableConf = colConf;
                        retVal.tables[conf].fieldId2Name = indexToFieldName;
                        var cols = self.visibleColumns(retVal.tables[conf].columns);
                        for ( col in  cols ) {
                            if ( col != '__typeInfo' && ! col.hidden ) {
                                var align = '';
                                if ( cols[col].align ) {
                                    align = cols[col].align;
                                } else {
                                    var type = cols[col].javaType.toLowerCase();
                                    switch (type) {
                                        case 'int':
                                        case 'byte':
                                        case 'short':
                                        case 'double':
                                        case 'float':
                                        case 'integer':
                                            align = 'right'; break;
                                        default: align='left';
                                    }
                                }
                                colConf.push(
                                    {   field: cols[col].name,
                                        displayName: cols[col].displayName,
                                        groupable: false,
//                                        cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()" id="{{row.entity.recordKey}}#COL_FIELD"><span ng-cell-text>{{COL_FIELD}}</span></div>'
                                        cellTemplate:
                                           '<div class="ngCellText" style="text-align: '+align + ';" ' +
                                               'ng-class="col.colIndex()"><span style="border-radius: 4px; transition: background-color .2s ease-out; padding: 4px;" ' +
                                               'ng-cell-text id="{{row.entity.recordKey}}#COL_FIELD">{{COL_FIELD}}</span></div>'
                                    }
                                );
                                indexToFieldName[cols[col].fieldId] = cols[col].name;
                            }
                        }
                    }
                }
                self.model = retVal;
                self.sendChange('ModelLoaded');
                self.runToDos();
            });
        };

        this.ws.onerror = function () {
            console.log("error");
            self.socketConnected = false;
            self.sendChange('State');
        };

        this.ws.onclose = function () {
            console.log("closed");
            self.socketConnected = false;
            self.sendChange('State');
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

    this.deletCBId = function( cbId ) {
        delete this.ws.cbMap[cbId];
    };

    // call streaming, callback must return true to unsubscribe
    this.callStreaming = function( methodName, arg, callback ) {
        this.call(methodName,arg,callback,true);
    };

    /////////////// subs/query

    this.subscribe = function( tableName, queryString, callback ) {
        this.callStreaming(
            "subscribe",
            new JQueryTuple({ tableName: tableName, querySource: queryString }),
            callback
        );
    };

    // if scope is set => apply after each change
    this.subscribeSet = function( tableName, queryString, resultset, scope ) {
        this.callStreaming(
            "subscribe",
            new JQueryTuple({ tableName: tableName, querySource: queryString }),
            function(change) {
                resultset.push(change);
                if ( scope != null ) {
                    scope.$apply(function () {});
                }
                return false;
            }
        );
    };

    /////////////// .. end subs query

    // tmp method
    this.querySet = function( queryId, resultset, scope ) {
        this.callStreaming("streamTable", queryId, function(change) {
            resultset.push(change);
            if ( change.type == RL_SNAPSHOT_DONE ) {
                scope.$apply(function () {});
                return true;
            }
            return false;
        });
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