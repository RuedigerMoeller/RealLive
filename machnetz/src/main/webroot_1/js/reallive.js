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
                var rec = map[change.recordKey];
                if ( rec !== 'undefined') {
                    this.list.splice(rec._rlIdx,1);
                }
            } break;
            case RL_UPDATE: {
                var rec = map[change.recordKey];
                console.log(rec);
            } break;
        }
    }
}