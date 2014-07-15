var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid']);

app.directive('rlTable', function() {
    return {
        restrict: 'E',
        scope: true,
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.height = '300px';
            $scope.gridOptions = {
                data: 'rlset.list',
                columnDefs: 'model.tables.'+$attrs.table+'.columnsNGTableConf',
                enableColumnResize: true,
                multiSelect: false,
                enableColumnReordering:false
            };
            console.log("Hallo");
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
                if ( $attrs.height ) {
                    $scope.height = $attrs.height;
                }
                RealLive.subscribeSet($attrs.table,"item.yearOfBirth > 1950", $scope.rlset,null); //$scope);
            });

        },
        template: '<div class="gridStyle" style="height: {{height}};" ng-grid="gridOptions"></div>'
    }
});

app.controller('RLAdmin', function ($scope) {

    $scope.host = 'localhost';
    $scope.port = '8887';
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.systables = new RLResultSet();
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
                    RealLive.callStreaming("streamTable", "SysTable", function(msg) {
                        console.log(msg);
                        $scope.systables.push(msg);
                        if ( msg.type == RL_SNAPSHOT_DONE ) {
                            $scope.$apply(function () {});
                            return true;
                        }
                        return false;
                    });
                }
            });
        };
        RealLive.doConnect($scope.host, $scope.port,$scope.websocketDir);
    };

});
