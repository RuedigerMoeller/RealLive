var RL_UPDATE    = 0;
var RL_ADD       = 1;
var RL_REMOVE    = 2;
var RL_OPERATION = 3;
var RL_SNAPSHOT_DONE = 4;
var RL_ERROR = 5;



var RealLive = new function() {

    // currently active websocket
    this.ws = null;
    this.socketConnected = false;
    this.model = null; // RealLive data model
    this.onChange = null; // callback function
    this.toDo = [];

    this.highlightElem = function(elementId) {
        if ( !elementId )
            return;
        var element = document.getElementById(elementId);
        if ( !element )
            return;
        if (!element.hicount && element.hicount != 0) {
            element.hicount = 1;
        } else {
            element.hicount++;
        }
        element.style.backgroundColor = '#F2E38A';
        (function () {
            var current = element;
            var prevKey = elementId;
            setTimeout(function () {
                if (current.hicount <= 1 || prevKey != current.id) {
                    current.style.backgroundColor = 'rgba(230,230,230,0.0)';
                    current.hicount = 0;
                } else {
                    current.hicount--;
                }
            }, 3000);
        }());
    };

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

    this.lastSeq = 0;

    this.renderStyles = {
        "Price": "<b>{{COL_FIELD/100|number:2}}</b>",
        "Qty"  : "{{COL_FIELD|number:0}}"
    };

    this.doConnect = function (host,port,websocketDir) {
        var self = this;

        this.ws = new WebSocket("ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir));
        this.ws.cbId = 1;
        this.ws.cbMap = {};
        this.ws.onopen = function () {
            this.lastSeq = 0;
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
                                var bgColor = null;
                                var fieldExpr = '{{COL_FIELD}}';

                                if ( cols[col].renderStyle ) {
                                    var expr = self.renderStyles[cols[col].renderStyle];
                                    if ( expr ) {
                                        fieldExpr = expr;
                                    }
                                }

                                if ( cols[col].bgColor ) {
                                    bgColor = cols[col].bgColor;
                                }

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
                                            align = 'right';
                                            break;
                                        default: align='left';
                                    }
                                }
                                var colWidth = (cols[col].name.length*14).toString()+"px";
                                if ( cols[col].displayWidth) {
                                    colWidth = cols[col].displayWidth;
                                }
                                colConf.push(
                                    {   field: cols[col].name,
                                        displayName: cols[col].displayName,
                                        width:  colWidth,
                                        groupable: false,
//                                        cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()" id="{{row.entity.recordKey}}#COL_FIELD"><span ng-cell-text>{{COL_FIELD}}</span></div>'
                                        cellTemplate:
                                           '<div class="ngCellText" style="text-align: '+align + '; '+(bgColor?'background-color:'+bgColor+';':'')+'"'+
                                            'ng-class="col.colIndex()"><span style="border-radius: 4px; transition: background-color .2s ease-out; padding: 4px; " ' +
                                            'ng-cell-text id="{{row.entity.recordKey}}#COL_FIELD">'+fieldExpr+'</span></div>'
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
//                    try
                    {
                        var msg = MinBin.decode(event.target.result);
                        if (msg instanceof JInvocationCallback) {
                            if ( _thisWS.lastSeq != 0 ) {
                                if ( _thisWS.lastSeq != msg.sequence-1 ) {
                                    console.log("ERROR: lastSeq ".concat(_thisWS.lastSeq)+" newSeq:".concat(msg.sequence));
                                }
                            }
                            _thisWS.lastSeq = msg.sequence;
                            var cb = _thisWS.cbMap[msg.cbId];
                            if (typeof cb === "function") {
                                var unsubscribe = cb.call(null, msg.result);
                                if (unsubscribe || msg.cbId.substring(0, 2) == 'mc') { // single shot
                                    delete _thisWS.cbMap[msg.cbId];
                                }
                            } else {
                                if ( msg )
                                    console.log("unmapped callback " + msg.cbId + " " + msg);
                                else {
                                    console.log("corrupted message unmapped callback " + msg.cbId + " " + msg);
                                }
                            }
                        }
//                    var strMsg = MinBin.prettyPrint(msg);
//                    // handle message
                    }
//                    catch (ex) {
//                        console.log(ex);
//                    }
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
    };

    this.unsubscribe = function(cbid) {
        if ( this.ws.cbMap[cbid])
            delete this.ws.cbMap[cbid];
        this.call("unsubscribe",cbid);
    };

    this.deletCBId = function( cbId ) {
        delete this.ws.cbMap[cbId];
    };

    // call streaming, callback must return true to unsubscribe
    this.callStreaming = function( methodName, arg, callback ) {
        return this.call(methodName,arg,callback,true);
    };

    /////////////// subs/query

    this.subscribeKey = function( tableName, recordKey, callback ) {
        this.callStreaming(
            "subscribeKey",
            new JQueryTuple({ tableName: tableName, querySource: recordKey }),
            callback
        );
    };

    this.subscribe = function( tableName, queryString, callback ) {
        this.callStreaming(
            "subscribe",
            new JQueryTuple({ tableName: tableName, querySource: queryString }),
            callback
        );
    };

    // if scope is set => apply after each change
    this.subscribeSet = function( tableName, queryString, resultset, scope ) {
        console.log("** SUBSCRIBE "+tableName+" query:"+queryString);
        var cb = function(change) {
            resultset.push(change);
            return false;
        };
        resultset.subsId = this.callStreaming(
            "subscribe",
            new JQueryTuple({ tableName: tableName, querySource: queryString }),
            cb
        );
    };

    /////////////// .. end subs query

    // tmp method
    this.querySet = function( queryId, resultset, scope ) {
        this.callStreaming("streamTable", queryId, function(change) {
            resultset.push(change);
            if ( change.type == RL_SNAPSHOT_DONE ) {
                return true;
            }
            return false;
        });
    };

    this.call = function( methodName, arg, callback, stream ) {
        var res = null;
        var msg = MinBin.encode(
            new JInvocation({
                "name" : methodName,
                "argument" : arg,
                "cbId" : typeof callback === "undefined" ? "0" : (stream ? 'st':'mc').concat(this.ws.cbId)
            })
        );
        if ( ! (typeof callback === "undefined") ) {
            if ( ! stream ) {
                this.ws.cbMap[res = 'mc'.concat(this.ws.cbId)] = callback;
            } else {
                this.ws.cbMap[res = 'st'.concat(this.ws.cbId)] = callback; // stream
            }
            this.ws.cbId++;
        }
        this.ws.send(msg);
        return res;
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

function RLResultSet() {
    this.map = {};
    this.list = [];
    this.preChangeHook = null;
    this.postChangeHook = null;
    this.snapFin = false;
    this.subsId = null;

    this.unsubscribe = function() {
        if ( this.subsId ) {
            if ( RealLive ) {
                RealLive.unsubscribe(this.subsId);
                this.subsId = null;
            }
        }
    };

    this.push = function(change) {
        if (this.preChangeHook) {
            this.preChangeHook.call(null,change,this.snapFin);
        }
        switch ( change.type ) {
            case RL_ADD: {
//                console.log( "add "+change.recordKey);
                var rec = change.newRecord;
                if ( this.map[change.recordKey] ) {
                    console.log('double add rec '+change.recordKey);
                }
                this.map[rec.recordKey] = rec;
                this.list.push(rec);
            } break;
            case RL_REMOVE: {
//                console.log( "remove "+change.recordKey);
                var rec = this.map[change.recordKey];
                if ( rec !== 'undefined') {
                    delete this.map[change.recordKey];
                    for ( var x = 0; x < this.list.length; x++) {
                        if ( this.list[x].recordKey == change.recordKey ) {
                            this.list.splice(x,1);
                        }
                    }
//                    if (this.map[change.recordKey]) {
//                        console.log("FAIL-------------------------------REMOVE MAP "+change.recordKey);
//                    }
                } else {
                    console.log('could not find removed rec '+change.recordKey+" "+this.map[change.recordKey]);
                }
            } break;
            case RL_SNAPSHOT_DONE:
                this.snapFin = true;
//                console.log("** snapfin on set ** size:"+this.list.length);
//                for (var i=0; i < this.list.length; i++) {
//                    console.log(this.list[i].recordKey);
//                }
                break;
            case RL_UPDATE: {
                var rec = this.map[change.recordKey];
                if ( rec ) {
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
        if (this.postChangeHook) {
            this.postChangeHook.call(null,change,this.snapFin);
        }
    };

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
};
