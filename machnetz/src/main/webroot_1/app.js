var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid', 'ngRoute']);

app .config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/market', {
                templateUrl: 'marketController.html',
                controller: 'MarketController'
            }).
            when('/position', {
                templateUrl: 'position.html',
                controller: 'PositionController'
            }).
            otherwise({
                templateUrl: 'market.html'
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
                        var test = document.getElementById($scope.itid);
                        if (!test.hicount && test.hicount != 0) {
                            test.hicount = 1;
                        } else {
                            test.hicount++;
                        }
                        test.style.backgroundColor = '#F2E38A';
                        (function () {
                            var current = test;
                            var prevKey = elementId;
                            setTimeout(function () {
                                if (current.hicount <= 1 || prevKey != current.id) {
                                    current.style.backgroundColor = 'rgba(230,230,230,0.0)';
                                    current.hicount = 0;
                                } else {
                                    current.hicount--;
                                }
                            }, 2000);
                        }());
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
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.height = '300px';
            if ( $attrs.height ) {
                $scope.height = $attrs.height;
            }
            $scope.gridOptions = {
                data: 'rlset.list',
                columnDefs: 'model.tables.'+$attrs.table+'.columnsNGTableConf',
                enableColumnResize: true,
                multiSelect: false,
                enableColumnReordering:false
            };
            RealLive.onModelLoaded(function() {
                $scope.rlset.preChangeHook = function(change,snapFin) {
                    if (change.type==RL_UPDATE) {
                        var fieldList = $scope.rlset.getChangedFieldNames(change);
                        var recKey = change.recordKey;
                        for (var i=0; i < fieldList.length; i++) {
                            var elementId = recKey + '#row.entity.' + fieldList[i];
                            var test = document.getElementById(elementId);
//                            console.log('hi '+elementId);
                            if ( test != null ) {
                                if ( ! test.hicount && test.hicount != 0 ) {
                                    test.hicount = 1;
                                } else {
                                    test.hicount++;
                                }
                                test.style.backgroundColor = '#F2E38A';
                                (function () {
                                    var current = test;
                                    var prevKey = elementId;
                                    setTimeout(function () {
                                        if ( current.hicount <= 1 || prevKey != current.id ) {
                                            current.style.backgroundColor = 'rgba(230,230,230,0.0)';
                                            current.hicount = 0;
                                        } else {
                                            current.hicount--;
                                        }
                                    }, 2000);
                                }());
                            }
                        }
                        if ( snapFin )
                            $scope.$apply(new function() {});
                    }
                };
                RealLive.subscribeSet($attrs.table,"item.yearOfBirth > 1950", $scope.rlset,null); //$scope);
            });

        },
        template: '<div class="gridStyle" style="height: {{height}};" ng-grid="gridOptions"></div>'
    }
});

app.controller('RLAdmin', function ($scope,$modal) {

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
            size:'sm',
            backdrop:false,
            keyboard:false,
            reallive:'reallive',
            user: '',
            loggedin: false,
            controller: function($scope) {
                $scope.reallive = 'RealLive',
                window.setTimeout(function() {
                    $scope.reallive='Realtime';
                    window.setTimeout(function() {
                        $scope.reallive='EveryThing';
                        window.setTimeout(function() {
                            $scope.reallive='.';
                            window.setTimeout(function() {
                                $scope.reallive='All Data. Real Time.';
                            }, 3000);
                        }, 3000);
                    }, 3000);
                }, 3000);
                $scope.doLogin = function() {
                    var self = this;
                    RealLive.subscribeKey('person', this.user, function(change) {
                        if ( change.type == RL_ADD ) {
                            self.loggedIn = true;
                            instance.close(true);
                        } else {
                            if (!self.loggedIn) {
                                self.msg = "Invalid user or password. retry.";
                            }
                        }
                    });
                }
            }
        })

    };
    $scope.doConnect();

});
