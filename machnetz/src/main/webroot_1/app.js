
var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid', 'ngRoute']);

app .config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/admin', {
                templateUrl: 'admin.html'
            }).
            when('/market', {
                templateUrl: 'market.html'
            }).
            when('/testing', {
                templateUrl: 'testing.html'
            }).
            when('/position', {
                templateUrl: 'position.html',
                controller: 'PositionController'
            }).
            otherwise({
                templateUrl: 'testing.html'
//                controller: 'LoginController'
            });
    }]);

app.directive('rlPopover', function ($compile,$templateCache,$http) {

    return {
        //restrict: "E",
        link: function (scope, element, $attrs) {
            $http.get($attrs.template,{cache:true}).success(function(data, status) {
                var options = {
                    content: function() {
                        return $compile(data)(scope);
                    },
                    placement: "right",
                    html: true,
                    animation: true,
//                    viewport: {selector: '#viewport', padding: 0},
//                trigger: 'hover',
                    date: scope.date,
                    template: "<div class='popover' style='border-radius: 3px; padding: 8px;'><div class='arrow'></div><div style='padding: 0px;' class='popover-content'></div></div>"
                };
                $(element).popover(options);
            });
        }
    };
});

app.directive('rlHi', function() {
    var rl_elemid = 1;

    function genId(prefix) {
        rl_elemid++;
        return prefix.concat(rl_elemid.toString());
    }

    return {
        transclude:true,
//        replace: true,
//        template: '<b>uh-<div ng-transclude></div>-uh</b>'
        template: '<span style="border-radius: 4px; transition: background-color .2s ease-out; padding: 4px;" id="{{itid}}" ng-transclude></span>',
        scope: true,
        link: function( $scope, $element, $attrs ) {
            $scope.itid = genId('rlhi');
            $element.id = $scope.itid;
            $scope.$watch(
                function() {
                    return $element[0].children[0].innerHTML;
                },
                function(newVal,oldVal) {
                    if (newVal!=oldVal) {
                        var elementId = $scope.itid;
                        RealLive.highlightElem(elementId);
                    }
                }
            );
//            setTimeout(function() {
//                document.getElementById($scope.itid).style.backgroundColor = "#F2E38A";
//            },5000);
        }
    }
});

app.directive( 'rlRecord', function()  {
   return {
       restrict: 'E',
       table: 'no-table',
       recordKey: 'no-key',
       snapFin: false,
       record: {},
       scope: true,
//       transclude: true,
//       template:'<span ng-transclude></span>',
       link: function( $scope, $element, $attrs) {
           $scope.table = $attrs.table;
           $scope.recordKey = $attrs.recordKey;
           if ( $attrs.hilight ) {
               $scope.hilight = $attrs.hilight;
           }
           RealLive.onModelLoaded(function() {
               RealLive.subscribeKey($scope.table,$scope.recordKey,function(change) {
                   switch ( change.type ) {
                       case RL_ADD: {
                           $scope.$apply(function() {
                               $scope.record = change.newRecord;
                           });
                       } break;
                       case RL_REMOVE: {
                           $scope.record = null;
                       } break;
                       case RL_SNAPSHOT_DONE:
                           $scope.snapFin = true;
//                           scope.$digest();
                           break;
                       case RL_UPDATE: {
                           var rec = $scope.record;
                           if ( rec ) {
                               $scope.$apply(function() {
                                   var changeArray = change.appliedChange.fieldIndex;
                                   for ( var i = 0; i < changeArray.length; i++ ) {
                                       var fieldId = changeArray[i];
                                       var newValue = change.appliedChange.newVal[i];
                                       var fieldName = RealLive.getFieldName(change.tableId,fieldId);
                                       rec[fieldName] = newValue;
                                   }
                                   $scope.record = JSON.parse(JSON.stringify(rec));
                               });
                           }
                       } break;
                   }
               });
           });
       }
   };
});

app.directive('rlTable', function() {
    return {
        restrict: 'E',
        scope: true,
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.height = '300px';
            $scope.exclude = {};
            $scope.links = {};

            if ( $attrs.rlExclude ) {
                var list = $attrs.rlExclude.split(",");
                for ( var i = 0; i < list.length; i++ )
                    $scope.exclude[list[i]] = true;
            }
            if ( $attrs.links ) {
                var list = $attrs.links.split(",");
                for ( var i = 0; i < list.length; i++ )
                    $scope.links[list[i]] = true;
            }

            if ( $attrs.height ) {
                $scope.height = $attrs.height;
            }

            $scope.$on("$destroy", function() {
                console.log("destroy");
                $scope.rlset.unsubscribe();
            });

            $scope.getColumns = function() {
                if ( RealLive.model == null ) {
                    return [];
                }
                var cols = $scope.model.tables[$attrs.table].columnsNGTableConf;
                var res = [];
                for (var i=0; i < cols.length; i++ ) {
                    var col = cols[i];
                    if ( ! $scope.exclude[col.field] ) {
                        var copiedCol = JSON.parse(JSON.stringify(col));
                        if ( $scope.links[col.field] ) {
                            copiedCol._fieldExpr = '<span class="rlhover" ng-click="cellClicked(\''+$attrs.table+'\',\''+col.field+'\' ,row.entity,$event)">'+copiedCol._fieldExpr+"</span>";
                        }
                        copiedCol.cellTemplate =
                           '<div class="ngCellText" style="text-align: '+copiedCol._align + '; '+(copiedCol._bgColor?'background-color:'+copiedCol._bgColor+';':'')+'"'+
                            'ng-class="col.colIndex()"><span style="transition: background-color .2s ease-out; padding: 3px; " ' +
                            'ng-cell-text id="{{row.entity.recordKey}}#COL_FIELD">'+copiedCol._fieldExpr+'</span></div>';
                        res.push(copiedCol);
                    }
                }
                return res;
            };
            $scope.gridOptions = {
                data: 'rlset.list',
                columnDefs: [],
                enableColumnResize: true,
                multiSelect: false,
                rowHeight: 27,
                enableColumnReordering:false,
                rowTemplate:
                    '<span id="row#{{row.entity.recordKey}}" style="transition: background-color .2s ease-out; padding: 3px;">'+
                    "<div ng-style=\"{ 'cursor': row.cursor }\" ng-repeat=\"col in renderedColumns\" ng-class=\"col.colIndex()\" class=\"ngCell {{col.cellClass}}\">" +
                    "\n" +
                    "\t<div class=\"ngVerticalBar\" ng-style=\"{height: rowHeight}\" ng-class=\"{ ngVerticalBarVisible: !$last }\">&nbsp;</div>\r" +
                    "\n" +
                    "\t<div ng-cell></div>\r" +
                    "\n" +
                    "</div></span>"
            };

            var subscribe = function () {
                $scope.rlset.postChangeHook = function (change, snapFin) {
                    if (change.type == RL_UPDATE) {
                        var fieldList = $scope.rlset.getChangedFieldNames(change);
                        var recKey = change.recordKey;
                        for (var i = 0; i < fieldList.length; i++) {
                            var elementId = recKey + '#row.entity.' + fieldList[i];
                            var elementById = document.getElementById(elementId);
                            if ( elementById ) {
                                var cell = angular.element(elementById).scope();
                                if (cell)
                                    cell.$digest();
                            }
                            RealLive.highlightElem(elementId);
                        }
//                        $scope.$digest();
                    } else
                    if (change.type == RL_SNAPSHOT_DONE) {
                        $scope.$digest();
                    } else
                    if (change.type == RL_ADD && $scope.rlset.snapFin ) {
                        var elementId = 'row#' + change.recordKey;
                        RealLive.highlightElem(elementId);
                        $scope.$digest();
                    } else
                    if (change.type == RL_REMOVE && $scope.rlset.snapFin ) {
                        var elementId = 'row#' + change.recordKey;
                        RealLive.highlightElem(elementId);
                        $scope.$digest();
                    }
                };
                RealLive.subscribeSet($attrs.table, $attrs.rlQuery ? $attrs.rlQuery : "true", $scope.rlset, null); //$scope);
            };

            $attrs.$observe('rlQuery', function() {
                $scope.rlset.unsubscribe();
                RealLive.onModelLoaded(subscribe);
            });

            RealLive.onModelLoaded(function() {
                $scope.gridOptions.columnDefs = $scope.getColumns();
            });

        },
        template: '<div class="gridStyle" style="height: {{height}};" ng-grid="gridOptions"></div>'
    }
});

var rlGlobalOrderContext;

app.directive('rlOrder', function() {
    return {
        scope: false,
        controller: function($scope) {

            $scope.ctx = rlGlobalOrderContext;
            $scope.doOrder = function() {
                rlGlobalOrderContext.doOrder();
            }
        }
    };
});

app.controller('RLAdmin', function ($scope,$modal,$http,$compile) {

    var mainScope = $scope;
    $scope.isOECollapsed = true;

    $scope.loggedIn = false;
    $scope.host = window.location.hostname;
    $scope.port = window.location.port;
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.model = null;
    $scope.alertmsg = null;
    $scope.gridOptions = {
        data: 'systables.list',
        columnDefs: 'model.tables.SysTable.columnsNGTableConf',
        enableColumnResize: true,
        multiSelect: false
    };

    $scope.showAlert = function(message,style) {
        $scope.alertmsg = message;
        if ( ! style ) {
            style = 'warning';
        }
        $scope.alertstyle = style;
    };

    $scope.hideAlert = function() {
        $scope.alertmsg = null;
    };

    $scope.cellClicked = function(table,field,row, event) {
        var recordkey = row.recordKey;
        console.log("clicked "+table+' '+field+' '+recordkey+" "+row);
        if ( 'Market' == table && (field == 'ask' || field == 'bid' || field == 'bidQty' || field == 'askQty' ) ) {
            var isBuy = field == 'ask' || field == 'askQty';
            if ( ! rlGlobalOrderContext )
                rlGlobalOrderContext = { order: new JOrder(), orderUnderway:false, ordermsg: '' };

            rlGlobalOrderContext.order.instrumentKey = recordkey;
            rlGlobalOrderContext.order.limitPrice = (isBuy ? row.ask : row.bid) /100;
            rlGlobalOrderContext.order.buy = isBuy ? 1 : 0;
            rlGlobalOrderContext.order.qty = field == 'bidQty' ? row.bidQty : field == 'askQty' ? row.askQty : 1;

            rlGlobalOrderContext.ordermsg = '';

            rlGlobalOrderContext.doOrder = function() {
                rlGlobalOrderContext.order.traderKey = $scope.user.recordKey;
                var transmittedOrder = new JOrder(rlGlobalOrderContext.order);
                transmittedOrder.limitPrice = (transmittedOrder.limitPrice*100)|0;
                rlGlobalOrderContext.orderUnderway = true;
                RealLive.call("addOrder", transmittedOrder, function(result) {
                    if ( result == '' ) {
                        $scope.showAlert("Placed " + (transmittedOrder.buy ? 'Buy' : 'Sell') + " Order for " + transmittedOrder.instrumentKey + " " + transmittedOrder.qty + "@" + transmittedOrder.limitPrice / 100 + "€");
                    } else {
                        $scope.showAlert(result+"   [ " + (transmittedOrder.buy ? 'Buy' : 'Sell') + " Order for " + transmittedOrder.instrumentKey + " " + transmittedOrder.qty + "@" + transmittedOrder.limitPrice / 100 + "€ ]", "danger");
                    }
                    rlGlobalOrderContext.orderUnderway = false;
                    rlGlobalOrderContext.elem.popover('destroy');
                    document.getElementById('rl-app-overlay').style.display='none';
                    document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,0)';
                    $scope.$digest();
                });
                setTimeout(function() {
                    if ( rlGlobalOrderContext.orderUnderway ) {
                        $scope.showAlert( "no system response in time. Check order overview.", "danger" );
                        rlGlobalOrderContext.orderUnderway = false;
                        rlGlobalOrderContext.elem.popover('destroy');
                        document.getElementById('rl-app-overlay').style.display='none';
                        document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,0)';
                    }
                    $scope.$digest();
                },5000)
            };

            $http.get("oepopover.html",{cache:true}).success(function(data, status) {
                var options = {
                    content: $compile(data)($scope),
                    placement: "bottom",
                    container: 'body',
                    html: true,
                    animation: true,
                    date: $scope.date,
                    template: "<div class='popover' style='border-radius: 3px; padding: 8px;'><div class='arrow'></div><div style='padding: 0px;' class='popover-content'></div></div>"
                };


                var elem = $(event.target);
                rlGlobalOrderContext.elem = elem;
                elem.popover(options);
                elem.popover('show');
                document.getElementById('rl-app-overlay').style.display='block';
                document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,.2)';
                document.getElementById('rl-app-overlay').onclick = function() {
                    elem.popover('destroy');
                    document.getElementById('rl-app-overlay').style.display='none';
                    document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,0)';
                };
            });
        }
    };

    $scope.doConnect = function () {
        RealLive.onChange = function(event) {
            $scope.$apply( function() {
                $scope.socketConnected = RealLive.socketConnected;
                if ( "ModelLoaded" == event ) {
                    $scope.model = RealLive.model;
                    $scope.openLogin($scope);
                }
            });
        };
        RealLive.doConnect($scope.host, $scope.port,$scope.websocketDir);
    };

    $scope.onLoggedIn = function() {
        mainScope.loggedIn = true;
        mainScope.positions = new RLResultSet();
        RealLive.subscribeSet("Position", 'it.qty!=0', mainScope.positions);
        mainScope.positions.postChangeHook = function(change, snapfin) { if ( snapfin ) $scope.$digest(); };
    };

    $scope.openLogin = function() {
        var instance = $modal.open({
            templateUrl: "login.html",
//            size:'sm',
            backdrop:false,
            keyboard:false,
            reallive:'reallive',
            user: '',
            loggedin: false,
            loginunderway: false,
            controller: function($scope) {
                window.setTimeout( function() {
                    var loginuser = document.getElementById("loginuser");
                    if ( loginuser != null )
                        loginuser.focus();
                },1000);
                $scope.size = 'sm';
                $scope.reallive = 'RealLive',
                $scope.doLogin = function() {
                    if ( $scope.loginunderway )
                        return;
                    var self = this;
                    $scope.loginunderway = true;
                    RealLive.call( "login", MinBin.jlist([self.user,self.pwd]), function(result) {
                        $scope.loginunderway = false;
                        if ( result.indexOf("success") < 0 ) {
                            self.msg = result;
                            return;
                        }
                        var subsId = RealLive.subscribeKey('Trader', self.user, function(change) {
                            if ( change.type == RL_ADD ) {
                                mainScope.user = change.newRecord;
                                mainScope.onLoggedIn();
                                document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,0)';
                                setTimeout(function() {document.getElementById('rl-app-overlay').style.display='none';}, 1000);
                                instance.close(true);
                            } else {
                                if (!mainScope.loggedIn) {
                                    self.msg = "Invalid user or password. retry.";
                                    RealLive.unsubscribe(subsId);
                                    $scope.$digest();
                                } else if (change.newRecord) {
                                    mainScope.user = change.newRecord;
                                    mainScope.$digest();
                                }
                            }
                        });
                    });
                }
            }
        })

    };

    $scope.doConnect();

});
