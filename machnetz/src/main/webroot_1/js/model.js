
var JTrader = function(obj) {
    this.__typeInfo = 'Trader';
    this.j_version = function() { return parseInt(this.version,10); };
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
    this.j_sequence = function() { return parseInt(this.sequence,10); };
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
    this.j_originator = function() { return parseInt(this.originator,10); };
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
    this.j_freeMB = function() { return parseInt(this.freeMB,10); };
    this.j_numElems = function() { return parseInt(this.numElems,10); };
    this.j_sizeMB = function() { return parseInt(this.sizeMB,10); };
    this.j_version = function() { return parseInt(this.version,10); };
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


var JTrade = function(obj) {
    this.__typeInfo = 'Trade';
    this.j_tradePrice = function() { return parseInt(this.tradePrice,10); };
    this.j_tradeQty = function() { return parseInt(this.tradeQty,10); };
    this.j_version = function() { return parseInt(this.version,10); };
    this.j_tradeTime = function() { return parseInt(this.tradeTime,10); };
    this.j_buyOrderId = function() { return this.buyOrderId; };
    this.j_buyTraderKey = function() { return this.buyTraderKey; };
    this.j_instrumentKey = function() { return this.instrumentKey; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_sellOrderId = function() { return this.sellOrderId; };
    this.j_sellTraderKey = function() { return this.sellTraderKey; };
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


var JPosition = function(obj) {
    this.__typeInfo = 'Position';
    this.j_avgPrice = function() { return parseInt(this.avgPrice,10); };
    this.j_qty = function() { return parseInt(this.qty,10); };
    this.j_sumPrice = function() { return parseInt(this.sumPrice,10); };
    this.j_version = function() { return parseInt(this.version,10); };
    this.j_instrKey = function() { return this.instrKey; };
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
    this.j_ask = function() { return parseInt(this.ask,10); };
    this.j_askQty = function() { return parseInt(this.askQty,10); };
    this.j_bid = function() { return parseInt(this.bid,10); };
    this.j_bidQty = function() { return parseInt(this.bidQty,10); };
    this.j_lastPrc = function() { return parseInt(this.lastPrc,10); };
    this.j_lastQty = function() { return parseInt(this.lastQty,10); };
    this.j_version = function() { return parseInt(this.version,10); };
    this.j_lastMatch = function() { return parseInt(this.lastMatch,10); };
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
    this.j_buy = function() { return this.buy?1:0; };
    this.j_limitPrice = function() { return parseInt(this.limitPrice,10); };
    this.j_qty = function() { return parseInt(this.qty,10); };
    this.j_version = function() { return parseInt(this.version,10); };
    this.j_creationTime = function() { return parseInt(this.creationTime,10); };
    this.j_creationTimeString = function() { return this.creationTimeString; };
    this.j_instrumentKey = function() { return this.instrumentKey; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_text = function() { return this.text; };
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


var JAsset = function(obj) {
    this.__typeInfo = 'Asset';
    this.j_margined = function() { return parseInt(this.margined,10); };
    this.j_openBuyQty = function() { return parseInt(this.openBuyQty,10); };
    this.j_openSellQty = function() { return parseInt(this.openSellQty,10); };
    this.j_qty = function() { return parseInt(this.qty,10); };
    this.j_version = function() { return parseInt(this.version,10); };
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
    this.j_contractsTraded = function() { return parseInt(this.contractsTraded,10); };
    this.j_version = function() { return parseInt(this.version,10); };
    this.j_volumeTraded = function() { return parseInt(this.volumeTraded,10); };
    this.j_expiryDate = function() { return parseInt(this.expiryDate,10); };
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
    this.j_originator = function() { return parseInt(this.originator,10); };
    this.j_type = function() { return parseInt(this.type,10); };
    this.j_error = function() { return this.error; };
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
    this.j_hidden = function() { return this.hidden?1:0; };
    this.j_fieldId = function() { return parseInt(this.fieldId,10); };
    this.j_order = function() { return parseInt(this.order,10); };
    this.j_align = function() { return this.align; };
    this.j_bgColor = function() { return this.bgColor; };
    this.j_customMeta = function() { return this.customMeta; };
    this.j_description = function() { return this.description; };
    this.j_displayName = function() { return this.displayName; };
    this.j_displayWidth = function() { return this.displayWidth; };
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
        case 'Trade': return new JTrade();
        case 'Position': return new JPosition();
        case 'TableMeta': return new JTableMeta();
        case 'Market': return new JMarket();
        case 'Metadata': return new JMetadata();
        case 'QueryTuple': return new JQueryTuple();
        case 'Order': return new JOrder();
        case 'Asset': return new JAsset();
        case 'Invocation': return new JInvocation();
        case 'Instrument': return new JInstrument();
        case 'ChangeBroadcast': return new JChangeBroadcast();
        case 'ColumnMeta': return new JColumnMeta();
        default: return { __typeInfo: clzname };
}
};

MinBin.installFactory(mbfactory);
