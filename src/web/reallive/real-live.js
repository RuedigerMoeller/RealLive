// version 2 of js RealLive facade as callback/connection management moved to kontraktor
// requires kontraktor.js

var RL_UPDATE    = 0;
var RL_ADD       = 1;
var RL_REMOVE    = 2;
var RL_OPERATION = 3;
var RL_SNAPSHOT_DONE = 4;
var RL_ERROR = 5;

var RealLive = new function() {

    var self = this;

    this.getTableMeta = function(tableId,columnName) {
        var res = Server.meta().tables[tableId];
        if ( columnName ) {
            return res.columns[columnName];
        }
        return res;
    };

    this.getFieldName = function(tableId,fieldId) {
        if ( this.getTableMeta( tableId) ) {
            return this.getTableMeta(tableId).fieldId2Name[fieldId];
        }
        console.log("unknown table "+tableId+" "+fieldId);
    };

    // compute ordered visible columns
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

        var names = [];
        var i;
        for( i = 0; i < result.length; i++ ) {
            names.push(result[i].name);
        }
        return names;
    };

    this.enrichModel = function (model) {
        console.log("model:"+model);
        model.tables.SysTable.columns.meta.hidden = true;

        // add fieldId2Name map to each table
        var tableName;
        for ( tableName in model.tables ) {
            console.log(tableName);
            if ( tableName != '__typeInfo' ) {
                var indexToFieldName = [];
                model.tables[tableName].fieldId2Name = indexToFieldName;
                var cols = model.tables[tableName].columns;
                model.tables[tableName].visibleColumnNames = self.visibleColumns(cols);
                var colName;
                for ( colName in  cols ) {
                    if ( colName != '__typeInfo' && ! colName.hidden ) {
                        indexToFieldName[cols[colName].fieldId] = cols[colName].name;
                    }
                }
            }
        }
    };

};

function RLResultSet() {

    var self = this;

    this.map = {};
    this.list = [];
    this.preChangeHook = null;
    this.postChangeHook = null;
    this.snapFin = false;
    this.subsId = null;

    this.subscribe = function( table, query ) {
        self.unsubscribe();
        Server.session().$subscribe( table, query, function(r,e) {

        });
    };

    this.unsubscribe = function() {
        if ( this.subsId ) {
            if ( RealLive ) {
                RealLive.unsubscribe(this.subsId); // FIXME: port
                this.subsId = null;
            }
        }
    };

    this.unsubscribeAndClear = function() {
        this.unsubscribe();
        this.map = {};
        this.list = [];
        this.snapFin = false;
        this.subsId = null;
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
