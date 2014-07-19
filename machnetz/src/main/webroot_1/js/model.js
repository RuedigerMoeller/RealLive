
var JTrader = function(obj) {
this.__typeInfo = 'Trader';
    this.j_cashBalance = function() { return this.cashBalance; };
    this.j_version = function() { return this.version; };
    this.j_email = function() { return this.email; };
    this.j_recordKey = function() { return this.recordKey; };
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
    this.j_sequence = function() { return this.sequence; };
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


var JTrade = function(obj) {
this.__typeInfo = 'Trade';
    this.j_version = function() { return this.version; };
    this.j_tradeTime = function() { return this.tradeTime; };
    this.j_buyOrderId = function() { return this.buyOrderId; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_sellOrderId = function() { return this.sellOrderId; };
    this.j_tradeTimeStringUTC = function() { return this.tradeTimeStringUTC; };
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


var JMarket = function(obj) {
this.__typeInfo = 'Market';
    this.j_ask = function() { return this.ask; };
    this.j_askQty = function() { return this.askQty; };
    this.j_bid = function() { return this.bid; };
    this.j_bidQty = function() { return this.bidQty; };
    this.j_lastPrc = function() { return this.lastPrc; };
    this.j_lastQty = function() { return this.lastQty; };
    this.j_version = function() { return this.version; };
    this.j_lastMatch = function() { return this.lastMatch; };
    this.j_lastMatchTimeUTC = function() { return this.lastMatchTimeUTC; };
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


var JOrder = function(obj) {
this.__typeInfo = 'Order';
    this.j_buy = function() { return this.buy; };
    this.j_limitPrice = function() { return this.limitPrice; };
    this.j_qty = function() { return this.qty; };
    this.j_version = function() { return this.version; };
    this.j_instrumentKey = function() { return this.instrumentKey; };
    this.j_originatingOrderId = function() { return this.originatingOrderId; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_traderKey = function() { return this.traderKey; };
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


var JInstrument = function(obj) {
this.__typeInfo = 'Instrument';
    this.j_version = function() { return this.version; };
    this.j_expiryDate = function() { return this.expiryDate; };
    this.j_description = function() { return this.description; };
    this.j_expiryDateString = function() { return this.expiryDateString; };
    this.j_recordKey = function() { return this.recordKey; };
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
    this.j_align = function() { return this.align; };
    this.j_customMeta = function() { return this.customMeta; };
    this.j_description = function() { return this.description; };
    this.j_displayName = function() { return this.displayName; };
    this.j_javaType = function() { return this.javaType; };
    this.j_name = function() { return this.name; };
    this.j_renderStyle = function() { return this.renderStyle; };
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
        case 'Trader': return new JTrader();
        case 'InvocationCallback': return new JInvocationCallback();
        case 'RecordChange': return new JRecordChange();
        case 'SysTable': return new JSysTable();
        case 'TestRecord': return new JTestRecord();
        case 'Trade': return new JTrade();
        case 'TableMeta': return new JTableMeta();
        case 'Market': return new JMarket();
        case 'Metadata': return new JMetadata();
        case 'QueryTuple': return new JQueryTuple();
        case 'Order': return new JOrder();
        case 'Invocation': return new JInvocation();
        case 'Instrument': return new JInstrument();
        case 'ChangeBroadcast': return new JChangeBroadcast();
        case 'ColumnMeta': return new JColumnMeta();
        default: return { __typeInfo: clzname };
}
};

MinBin.installFactory(mbfactory);
