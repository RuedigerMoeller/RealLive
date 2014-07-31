
var app = angular.module("rl-admin", ['ui.bootstrap', 'ngGrid', 'ngRoute']);

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

app.directive('rlBubbles', function () {

    return {
        restrict: 'EA',
        scope: true,
        link: function ($scope, $element, $attrs) {
            $scope.field = $attrs.field;
            $scope.textAttr = $attrs.textAttr;
            $scope.table = $attrs.table;
            $scope.query =$attrs.query;
            $scope.size = $attrs.size; // always quadratic
            $scope.color = $attrs.color;

            var resultSet = new RLResultSet();

            if ( ! $scope.size )
                $scope.size = 400;
            if ( ! $scope.textAttr )
                $scope.textAttr = 'recordKey';
            if ( ! $scope.color )
                $scope.color = "steelblue";


            $scope.canvas = d3.select($element[0]).append("svg")
                .attr('width',  ($scope.size)+"px")
                .attr('height', ($scope.size)+"px");
//                .append("g");
//                .attr("transform", "translate(" + 0 + "," + 0 + ")"); // origin
            $scope.pack = d3.layout.pack().size([$scope.size-10,$scope.size-10]).padding(5);

            var tip = d3.tip()
                .attr('class', 'd3-tip')
                .html(function(d) {
                    if (d.children)
                        return '';
                    return d[$scope.textAttr] +' '+d[$scope.field];
                })
                .offset([0, 3]);

            $scope.subsId = 0;
            $attrs.$observe('query', function(data) {
                $scope.query = data;
                if ( RealLive.socketConnected ) {
                    RealLive.subscribeSet($scope.table,$scope.query,resultSet, $scope );
                }
                resultSet.postChangeHook = function (change, snapFin) {
                    $scope.bubbles(resultSet.list);
                };
            }, true);

            $scope.bubbles = function(dataSet) {

                var max = 0;
                for ( var i = 0; i < dataSet.length; i++ ) {
                    var newVar = dataSet[i][$scope.field];
                    if ( newVar > max )
                        max = newVar;
                }

                var fac = 1;
                if ( max > 1 ) {
                    fac = $scope.size/max;
                }

                for ( var i = 0; i < dataSet.length; i++ ) {
                    var newVar = dataSet[i][$scope.field];
                    dataSet[i].value = newVar * fac;
                }

                var nodeSet = $scope.pack.nodes({ name: 'top', children: dataSet });

                $scope.canvas.call(tip);

                var nodes = $scope.canvas.selectAll('.node').data(nodeSet);
                var node = nodes.enter()
                    .append('g')
                    .attr("class", "node")
                    .attr("transform", function(d) { return "translate("+ d.x +"," + d.y + ")"; } );

                node
                    .append("circle")
                    .attr("r", function(d) { return d.r; })
                    .attr("fill", function(d) { return d.children ? "#fff": $scope.color; } )
                    .attr("stroke", "black" ) //function(d) { return d.children ? "#fff": $scope.color; })
                    .attr("stroke-width", "2")
                    .attr("stroke-opacity", ".8")
                    .attr("opacity",.25)
                    .on('mousedown', tip.show )
                    .on('mouseout', tip.hide );
                node
                    .append("text")
                    .text( function(d) { return d.children ? "" : d[$scope.textAttr];}).attr("style", 'text-anchor: middle;')
                      .on('mousedown', tip.show )
                      .on('mouseout', tip.hide );


                var trans = nodes.transition();

                // ... update circle radius
                trans.select("circle")
                    .transition()
                    .attr("r", function(d) { return d.r; });

                trans.select("text").transition()
                    .text( function(d) { return d.children ? "" : d[$scope.textAttr];}).attr("style", 'text-anchor: middle;');

                trans.duration(500)
                    .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })
                    .attr("r", function(d) { return d.r; });

                var exit = nodes.exit();

                exit.select("g")
                    .remove();
//                exit.select("circle")
//                    .remove();
//                exit.select("text")
//                    .remove();
            };

//            $scope.$watch('data', function(){
//                $scope.bubbles($scope.resultSet.list);
//            }, true);
        }
    };
});

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
           $scope.subsId = 0;
           $scope.table = $attrs.table;
           $scope.recordKey = $attrs.recordKey;
           if ( $attrs.hilight ) {
               $scope.hilight = $attrs.hilight;
           }
           $attrs.$observe('recordKey', function(data) {
               console.log("Updated record key ", data);
               $scope.recordKey = data;
               if ( RealLive.socketConnected ) {
                   RealLive.unsubscribe($scope.subsId);
                   $scope.subsribeKey();
               }
           }, true);

           $scope.subsribeKey = function() {
               $scope.subsId = RealLive.subscribeKey($scope.table, $scope.recordKey, function (change) {
                   switch (change.type) {
                       case RL_ADD:
                       {
                           $scope.$apply(function () {
                               $scope.record = change.newRecord;
                           });
                       }
                           break;
                       case RL_REMOVE:
                       {
                           $scope.record = null;
                       }
                           break;
                       case RL_SNAPSHOT_DONE:
                           $scope.snapFin = true;
//                           scope.$digest();
                           break;
                       case RL_UPDATE:
                       {
                           var rec = $scope.record;
                           if (rec) {
                               $scope.$apply(function () {
                                   var changeArray = change.appliedChange.fieldIndex;
                                   for (var i = 0; i < changeArray.length; i++) {
                                       var fieldId = changeArray[i];
                                       var newValue = change.appliedChange.newVal[i];
                                       var fieldName = RealLive.getFieldName(change.tableId, fieldId);
                                       rec[fieldName] = newValue;
                                   }
                                   $scope.record = JSON.parse(JSON.stringify(rec));
                               });
                           }
                       }
                        break;
                   }
               })
           };

           RealLive.onModelLoaded( $scope.subsribeKey );
       }
   }
});

app.directive('rlTable', function() {
    return {
        restrict: 'E',
        scope: true,
        controller: function( $scope, $attrs ) {
            $scope.rlset = new RLResultSet();
            $scope.height = '300px';
            $scope.exclude = {};
            $scope.links = {};   // ',' separated list of clickable column field names
            $scope.action = null; // plain html of action column template. row denotes the record

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

            if ( $attrs.action ) {
                $scope.action = $attrs.action;
            }

            if ( $attrs.actionWidth ) {
                $scope.actionWidth = $attrs.actionWidth;
            } else {
                $scope.actionWidth = "20px";
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
                if ( $scope.action ) {
                    res.push({
                        field: 'action',
                        displayName: '',
                        width: $scope.actionWidth,
                        sortable: false,
                        enableCellEdit: false,
                        cellTemplate: '<div class="ngCellText colt{{$index}}">'+$scope.action+'</div>'
                    })
                }
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
