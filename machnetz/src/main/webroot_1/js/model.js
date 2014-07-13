
var JClusterClients = function(obj) {
this.__typeInfo = 'ClusterClients';
    this.j_instanceNum = function() { return this.instanceNum; };
    this.j_version = function() { return this.version; };
    this.j_name = function() { return this.name; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_state = function() { return this.state; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JInvocationCallback = function(obj) {
this.__typeInfo = 'InvocationCallback';
    this.j_result = function() { return this.result; };
    this.j_cbId = function() { return this.cbId; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JAuthRequest = function(obj) {
this.__typeInfo = 'AuthRequest';
    this.j_misc = function() { return this.misc; };
    this.j_pwd = function() { return this.pwd; };
    this.j_user = function() { return this.user; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JRecordChange = function(obj) {
this.__typeInfo = 'RecordChange';
    this.j_recordId = function() { return this.recordId; };
    this.j_newVal = function() { return this.newVal; };
    this.j_oldVals = function() { return this.oldVals; };
    this.j_tableId = function() { return this.tableId; };
    this.j_fieldIndex = function() { return MinBin.i32(this.fieldIndex); };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JSysTable = function(obj) {
this.__typeInfo = 'SysTable';
    this.j_freeMB = function() { return this.freeMB; };
    this.j_numElems = function() { return this.numElems; };
    this.j_sizeMB = function() { return this.sizeMB; };
    this.j_version = function() { return this.version; };
    this.j_description = function() { return this.description; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_tableName = function() { return this.tableName; };
    this.j_meta = function() { return this.meta; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JTestRecord = function(obj) {
this.__typeInfo = 'TestRecord';
    this.j_version = function() { return this.version; };
    this.j_yearOfBirth = function() { return this.yearOfBirth; };
    this.j_name = function() { return this.name; };
    this.j_preName = function() { return this.preName; };
    this.j_profession = function() { return this.profession; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_sex = function() { return this.sex; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JTableMeta = function(obj) {
this.__typeInfo = 'TableMeta';
    this.j_columns = function() { return MinBin.jmap(val); };
    this.j_customMeta = function() { return this.customMeta; };
    this.j_description = function() { return this.description; };
    this.j_displayName = function() { return this.displayName; };
    this.j_name = function() { return this.name; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JMetadata = function(obj) {
this.__typeInfo = 'Metadata';
    this.j_tables = function() { return MinBin.jmap(val); };
    this.j_name = function() { return this.name; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JQueryTuple = function(obj) {
this.__typeInfo = 'QueryTuple';
    this.j_querySource = function() { return this.querySource; };
    this.j_tableName = function() { return this.tableName; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JInvocation = function(obj) {
this.__typeInfo = 'Invocation';
    this.j_argument = function() { return this.argument; };
    this.j_cbId = function() { return this.cbId; };
    this.j_name = function() { return this.name; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JAuthResponse = function(obj) {
this.__typeInfo = 'AuthResponse';
    this.j_sucess = function() { return this.sucess; };
    this.j_sessionKey = function() { return this.sessionKey; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JChangeBroadcast = function(obj) {
this.__typeInfo = 'ChangeBroadcast';
    this.j_type = function() { return this.type; };
    this.j_newRecord = function() { return this.newRecord; };
    this.j_appliedChange = function() { return this.appliedChange; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_tableId = function() { return this.tableId; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };


var JColumnMeta = function(obj) {
this.__typeInfo = 'ColumnMeta';
    this.j_fieldId = function() { return this.fieldId; };
    this.j_order = function() { return this.order; };
    this.j_customMeta = function() { return this.customMeta; };
    this.j_description = function() { return this.description; };
    this.j_displayName = function() { return this.displayName; };
    this.j_name = function() { return this.name; };
    this.fromObj = function(obj) {
            for ( var key in obj ) {
                var setter = 'j_'.concat(key);
                if ( this.hasOwnProperty(setter) ) {
                    this[key] = obj[key];
                }
            }
            return this;
        };
        if ( obj != null ) {
            this.fromObj(obj);
        }
    };



var mbfactory = function(clzname) {
switch (clzname) {
        case 'ClusterClients': return new JClusterClients();
        case 'InvocationCallback': return new JInvocationCallback();
        case 'AuthRequest': return new JAuthRequest();
        case 'RecordChange': return new JRecordChange();
        case 'SysTable': return new JSysTable();
        case 'TestRecord': return new JTestRecord();
        case 'TableMeta': return new JTableMeta();
        case 'Metadata': return new JMetadata();
        case 'QueryTuple': return new JQueryTuple();
        case 'Invocation': return new JInvocation();
        case 'AuthResponse': return new JAuthResponse();
        case 'ChangeBroadcast': return new JChangeBroadcast();
        case 'ColumnMeta': return new JColumnMeta();
        default: return { __typeInfo: clzname };
}
};

MinBin.installFactory(mbfactory);
