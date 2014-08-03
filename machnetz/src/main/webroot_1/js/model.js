
var JTrader = function(obj) {
    this.__typeInfo = 'Trader';
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
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
    this.j_sequence = function() { return MinBin.parseIntOrNan(this.sequence); };
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


var JSession = function(obj) {
    this.__typeInfo = 'Session';
    this.j_bcasts = function() { return MinBin.parseIntOrNan(this.bcasts); };
    this.j_requests = function() { return MinBin.parseIntOrNan(this.requests); };
    this.j_subscriptions = function() { return MinBin.parseIntOrNan(this.subscriptions); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
    this.j_loginTime = function() { return this.loginTime; };
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


var JRecordChange = function(obj) {
    this.__typeInfo = 'RecordChange';
    this.j_originator = function() { return MinBin.parseIntOrNan(this.originator); };
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
    this.j_freeMB = function() { return MinBin.parseIntOrNan(this.freeMB); };
    this.j_numElems = function() { return MinBin.parseIntOrNan(this.numElems); };
    this.j_sizeMB = function() { return MinBin.parseIntOrNan(this.sizeMB); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
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
    this.j_isBuy = function() { return this.isBuy?1:0; };
    this.j_tradePrice = function() { return MinBin.parseIntOrNan(this.tradePrice); };
    this.j_tradeQty = function() { return MinBin.parseIntOrNan(this.tradeQty); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
    this.j_tradeTimeStamp = function() { return MinBin.parseIntOrNan(this.tradeTimeStamp); };
    this.j_buyOrderId = function() { return this.buyOrderId; };
    this.j_buyTraderKey = function() { return this.buyTraderKey; };
    this.j_instrumentKey = function() { return this.instrumentKey; };
    this.j_recordKey = function() { return this.recordKey; };
    this.j_sellOrderId = function() { return this.sellOrderId; };
    this.j_sellTraderKey = function() { return this.sellTraderKey; };
    this.j_tradeTime = function() { return this.tradeTime; };
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
    this.j_avgPrice = function() { return MinBin.parseIntOrNan(this.avgPrice); };
    this.j_qty = function() { return MinBin.parseIntOrNan(this.qty); };
    this.j_sumPrice = function() { return MinBin.parseIntOrNan(this.sumPrice); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
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
    this.j_ask = function() { return MinBin.parseIntOrNan(this.ask); };
    this.j_askQty = function() { return MinBin.parseIntOrNan(this.askQty); };
    this.j_bid = function() { return MinBin.parseIntOrNan(this.bid); };
    this.j_bidQty = function() { return MinBin.parseIntOrNan(this.bidQty); };
    this.j_lastPrc = function() { return MinBin.parseIntOrNan(this.lastPrc); };
    this.j_lastQty = function() { return MinBin.parseIntOrNan(this.lastQty); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
    this.j_lastMatch = function() { return MinBin.parseIntOrNan(this.lastMatch); };
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
    this.j_limitPrice = function() { return MinBin.parseIntOrNan(this.limitPrice); };
    this.j_qty = function() { return MinBin.parseIntOrNan(this.qty); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
    this.j_creationTime = function() { return MinBin.parseIntOrNan(this.creationTime); };
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
    this.j_margined = function() { return MinBin.parseIntOrNan(this.margined); };
    this.j_openBuyQty = function() { return MinBin.parseIntOrNan(this.openBuyQty); };
    this.j_openSellQty = function() { return MinBin.parseIntOrNan(this.openSellQty); };
    this.j_qty = function() { return MinBin.parseIntOrNan(this.qty); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
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
    this.j_contractsTraded = function() { return MinBin.parseIntOrNan(this.contractsTraded); };
    this.j_version = function() { return MinBin.parseIntOrNan(this.version); };
    this.j_volumeTraded = function() { return MinBin.parseIntOrNan(this.volumeTraded); };
    this.j_expiryDate = function() { return MinBin.parseIntOrNan(this.expiryDate); };
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
    this.j_originator = function() { return MinBin.parseIntOrNan(this.originator); };
    this.j_type = function() { return MinBin.parseIntOrNan(this.type); };
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
    this.j_fieldId = function() { return MinBin.parseIntOrNan(this.fieldId); };
    this.j_order = function() { return MinBin.parseIntOrNan(this.order); };
    this.j_align = function() { return this.align; };
    this.j_bgColor = function() { return this.bgColor; };
    this.j_customMeta = function() { return this.customMeta; };
    this.j_description = function() { return this.description; };
    this.j_displayName = function() { return this.displayName; };
    this.j_displayWidth = function() { return this.displayWidth; };
    this.j_javaType = function() { return this.javaType; };
    this.j_name = function() { return this.name; };
    this.j_renderStyle = function() { return this.renderStyle; };
    this.j_textColor = function() { return this.textColor; };
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
        case 'Session': return new JSession();
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
