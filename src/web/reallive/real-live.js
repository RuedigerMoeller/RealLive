// version 2 of js RealLive facade as callback/connection management moved to kontraktor
// requires kontraktor.js

var RL_UPDATE    = 0;
var RL_ADD       = 1;
var RL_REMOVE    = 2;
var RL_OPERATION = 3;
var RL_SNAPSHOT_DONE = 4;
var RL_ERROR = 5;

var RealLive = new function() {

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

};