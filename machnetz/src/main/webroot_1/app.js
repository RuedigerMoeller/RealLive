var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid']);

app.directive('rlTable', function() {
    return {
        restrict: 'E',
        scope: true,
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.gridOptions = {
                data: 'rlset.list',
                columnDefs: 'model.tables.'+$attrs.table+'.columnsNGTableConf',
                enableColumnResize: true,
                multiSelect: false
            };
            console.log("Hallo");
            RealLive.onModelLoaded(function() {
                RealLive.querySet($attrs.table,$scope.rlset,$scope);
            });

        },
//    {
//            set: new RLResultSet(),
//            gridOptions: {
//                data: [], // '$parent.systables.list',
//                columnDefs: [], //'$parent.model.tables.SysTable.columnsNGTableConf',
//                enableColumnResize: true,
//                multiSelect: false
//            }
//        },
        template: '<div class="gridStyle" ng-grid="gridOptions"></div>'
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
