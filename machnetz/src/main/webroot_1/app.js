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
                           $scope.record = change.newRecord;
                       } break;
                       case RL_REMOVE: {
                           $scope.record = null;
                       } break;
                       case RL_SNAPSHOT_DONE:
                           $scope.snapFin = true;
                           break;
                       case RL_UPDATE: {
                           $scope.$apply( function() {
                               var rec = $scope.record;
                               if ( rec ) {
                                   var changeArray = change.appliedChange.fieldIndex;
                                   for ( var i = 0; i < changeArray.length; i++ ) {
                                       var fieldId = changeArray[i];
                                       var newValue = change.appliedChange.newVal[i];
                                       var fieldName = RealLive.getFieldName(change.tableId,fieldId);
                                       rec[fieldName] = newValue;
                                   }
                               }
                           });
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
        exclude: {},
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.height = '300px';
            if ( $attrs.rlExclude ) {
                var list = $attrs.rlExclude.split(",");
                $scope.exclude = {};
                for ( var i = 0; i < list.length; i++ )
                    $scope.exclude[list[i]] = true;
            } else {
                $scope.exclude = {};
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
                        res.push(col);
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
                    '<span id="row#{{row.entity.recordKey}}" style="border-radius: 4px; transition: background-color .2s ease-out; padding: 4px;">'+
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
                            RealLive.highlightElem(elementId);
                        }
                    }
                    if (snapFin) {
                        $scope.$digest();
                    }
                    if (change.type == RL_ADD) {
                        var elementId = 'row#' + change.recordKey;
                        RealLive.highlightElem(elementId);
                    }
                };
                RealLive.subscribeSet($attrs.table, $attrs.rlQuery ? $attrs.rlQuery : "true", $scope.rlset, null); //$scope);
            };

            $scope.$watch('rlset.list.length');
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

app.controller('OrderEntry', function($scope) {
    $scope.order = new JOrder();
    $scope.order.instrumentKey = 'Germany';

    $scope.doOrder = function(buy) {
        $scope.order.buy = buy;
        $scope.order.traderKey = $scope.user.recordKey;
        var transmittedOrder = new JOrder($scope.order);
        RealLive.call("addOrder", transmittedOrder, function(result) {
            console.log("Order sucess "+result);
        });
    }
});

app.controller('RLAdmin', function ($scope,$modal) {

    $scope.isOECollapsed = true;

    $scope.loggedIn = false;
    $scope.host = window.location.hostname;
    $scope.port = window.location.port;
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.model = null;
    $scope.gridOptions = {
        data: 'systables.list',
        columnDefs: 'model.tables.SysTable.columnsNGTableConf',
        enableColumnResize: true,
        multiSelect: false
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

    $scope.openLogin = function() {
        var instance = $modal.open({
            templateUrl: "login.html",
//            size:'sm',
            backdrop:false,
            keyboard:false,
            reallive:'reallive',
            user: '',
            loggedin: false,
            controller: function($scope) {
                window.setTimeout( function() {
                    var loginuser = document.getElementById("loginuser");
                    if ( loginuser != null )
                        loginuser.focus();
                },1000);
                $scope.size = 'sm';
                $scope.reallive = 'All data ..',
                window.setTimeout(function() {
                    $scope.reallive='in RealTime';
                    $scope.$digest();
                    window.setTimeout(function() {
                        $scope.reallive='.';
                        $scope.$digest();
                        window.setTimeout(function() {
                            $scope.reallive='..';
                            $scope.$digest();
                            window.setTimeout(function() {
                                $scope.reallive='RealLive';
                                $scope.$digest();
                            }, 3000);
                        }, 2000);
                    }, 2000);
                }, 3000);
                $scope.doLogin = function() {
                    var self = this;
                    RealLive.subscribeKey('Trader', this.user, function(change) {
                        if ( change.type == RL_ADD ) {
                            self.loggedIn = true;
                            $scope.$parent.user = change.newRecord;
                            instance.close(true);
                            document.getElementById('rl-app-overlay').style.background='rgba(0,0,0,0)';
                            setTimeout(function() {document.getElementById('rl-app-overlay').style.display='none';}, 2000);
                        } else {
                            if (!self.loggedIn) {
                                self.msg = "Invalid user or password. retry.";
                                $scope.$digest();
                            }
                        }
                    });
                }
            }
        })

    };
    $scope.doConnect();

});
