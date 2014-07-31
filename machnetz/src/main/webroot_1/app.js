
var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid', 'ngRoute', 'rl-angular']);

app.config(['$routeProvider',
    function($routeProvider) {

        var requireAuthentication = function () {
            return {
                load: function ($q) {
                    if (app.mainScope.loggedIn) { // fire $routeChangeSuccess
                        var deferred = $q.defer();
                        deferred.resolve();
                        return deferred.promise;
                    } else { // fire $routeChangeError
                        return $q.reject("'/login'");
                    }
                }
            };
        };


        $routeProvider.
            when('/admin', {
                templateUrl: 'admin.html',
                resolve: requireAuthentication()
            }).
            when('/market', {
                templateUrl: 'market.html',
                resolve: requireAuthentication()
            }).
            when('/login', {
                templateUrl: 'empty.html'
            }).
            when('/showcase', {
                templateUrl: 'loginview.html'
            }).
            when('/position', {
                templateUrl: 'position.html',
                controller: 'PositionController',
                resolve: requireAuthentication()
            }).
            otherwise({
                templateUrl: 'empty.html'
            });
    }]);

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

    app.mainScope = $scope;
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

    $scope.deleteOrder = function(order) {
        RealLive.call("deleteOrder", order, function(result) {
            if ( result.indexOf('not found') >= 0 ) // dirty ..
            {
                $scope.showAlert( result, 'danger' );
            } else {
                $scope.showAlert(result);
            }
        });
    };

    $scope.cellClicked = function(table,field,row, event) {
        var recordkey = row.recordKey;
        var orderFilled = false;
        console.log("clicked "+table+' '+field+' '+recordkey+" "+row);

        if ( ! rlGlobalOrderContext )
            rlGlobalOrderContext = { order: new JOrder(), orderUnderway:false, ordermsg: '' };

        if ( 'Position' == table ) {
            var isBuy = row.qty > 0;

            rlGlobalOrderContext.order.instrumentKey = row.instrKey;
            rlGlobalOrderContext.order.limitPrice = (row.avgPrice < 0 ? -row.avgPrice : row.avgPrice) / 100;
            rlGlobalOrderContext.order.buy = isBuy ? 0 : 1; // reverse
            rlGlobalOrderContext.order.qty = row.qty < 0 ? -row.qty : row.qty;
            rlGlobalOrderContext.ordermsg = '';
            orderFilled = true;

        } else if ( 'Market' == table && (field == 'ask' || field == 'bid' || field == 'bidQty' || field == 'askQty' ) ) {
            var isBuy = field == 'ask' || field == 'askQty';

            rlGlobalOrderContext.order.instrumentKey = recordkey;
            rlGlobalOrderContext.order.limitPrice = (isBuy ? row.ask : row.bid) / 100;
            rlGlobalOrderContext.order.buy = isBuy ? 1 : 0;
            rlGlobalOrderContext.order.qty = field == 'bidQty' ? row.bidQty : field == 'askQty' ? row.askQty : 1;

            rlGlobalOrderContext.ordermsg = '';
            orderFilled = true;
        } else if ( 'Market' == table && (field == 'buyAction' || field == 'sellAction' ) ) {
            var isBuy = field == 'buyAction';

            rlGlobalOrderContext.order.instrumentKey = recordkey;
            rlGlobalOrderContext.order.limitPrice = (isBuy ? row.ask : row.bid) / 100;
            rlGlobalOrderContext.order.buy = isBuy ? 1 : 0;
            rlGlobalOrderContext.order.qty = field == 'bidQty' ? row.bidQty : field == 'askQty' ? row.askQty : 1;

            rlGlobalOrderContext.ordermsg = '';
            orderFilled = true;
        }

        if (orderFilled) {
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
            size:'sm',
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
                            $scope.$digest();
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
