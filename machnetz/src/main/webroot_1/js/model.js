
var JSysTable = function(obj) {
    this.__typeInfo = 'SysTable';
    this.setFreeMB = function(val) { this.freeMB = val; };
    this.setNumElems = function(val) { this.numElems = val; };
    this.setSizeMB = function(val) { this.sizeMB = val; };
    this.setVersion = function(val) { this.version = val; };
    this.setDescription = function(val) { this.description = val; };
    this.setKey = function(val) { this.key = val; };
    this.setTableName = function(val) { this.tableName = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
    this.setArgument = function(val) { this.argument = val; };
    this.setCbId = function(val) { this.cbId = val; };
    this.setName = function(val) { this.name = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
    this.setResult = function(val) { this.result = val; };
    this.setCbId = function(val) { this.cbId = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
    this.setMisc = function(val) { this.misc = val; };
    this.setPwd = function(val) { this.pwd = val; };
    this.setUser = function(val) { this.user = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
    this.setSucess = function(val) { this.sucess = val; };
    this.setSessionKey = function(val) { this.sessionKey = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
    this.setType = function(val) { this.type = val; };
    this.setNewRecord = function(val) { this.newRecord = val; };
    this.setAppliedChange = function(val) { this.appliedChange = val; };
    this.setRecordKey = function(val) { this.recordKey = val; };
    this.setTableId = function(val) { this.tableId = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
            }
        }
        return this;
    };
    if ( obj != null ) {
        this.fromObj(obj);
    }
};


var JClusterClients = function(obj) {
    this.__typeInfo = 'ClusterClients';
    this.setInstanceNum = function(val) { this.instanceNum = val; };
    this.setVersion = function(val) { this.version = val; };
    this.setKey = function(val) { this.key = val; };
    this.setName = function(val) { this.name = val; };
    this.setState = function(val) { this.state = val; };
    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'set'.concat(key.substr(0,1).toUpperCase()).concat(key.substr(1));
            if ( this.hasOwnProperty(setter) ) {
                this[setter](obj[key]);
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
        case 'SysTable': return new JSysTable();
        case 'Invocation': return new JInvocation();
        case 'InvocationCallback': return new JInvocationCallback();
        case 'AuthRequest': return new JAuthRequest();
        case 'AuthResponse': return new JAuthResponse();
        case 'ChangeBroadcast': return new JChangeBroadcast();
        case 'ClusterClients': return new JClusterClients();
        default: return { __typeInfo: clzname };
    }
};

MinBin.installFactory(mbfactory);
