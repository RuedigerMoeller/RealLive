angular.module('rl-angular', ['ngGrid'])

.directive('rlDonutValue', function () {

    return {
        restrict: 'EA',
        scope: true,
        link: function ($scope, $element, $attrs) {
            // attributes + defaults
            var size = $attrs.size;
            var maxVal = $attrs.max;
            var value = $attrs.value;
            var color0 = '#99f';
            var color1 = '#77e';

            if (!size)
                size = 80;
            if (!maxVal)
                maxVal = 100;
            var svg = d3.select($element[0]).append("svg")
                .attr("width", size)
                .attr("height", size)
                .append("g")
                .attr("transform", "translate(" + size / 2 + "," + size / 2 + ")");
            var text_y = "+.30em";

            var dataset = {
                    lower: calcPercent(0),
                    upper: calcPercent(value)
                },
                radius = size / 2,
                pie = d3.layout.pie().sort(null),
                format = d3.format(".0%");

            var arc = d3.svg.arc()
                .innerRadius(radius - 20)
                .outerRadius(radius);


            var path = svg.selectAll("path")
                .data(pie(dataset.lower))
                .enter().append("path")
                .attr("fill", function(d, i) { return i == 0 ? color0 : color1; })
                .attr("d", arc)
                .each(function(d) { this._current = d; }); // store the initial values

            var text = svg.append("text")
                .attr("text-anchor", "middle")
                .attr("font-weight", "bold")
                .attr("font-size", "14px")
                .attr("dy", text_y);

            var progress = 0;
            var timeout = setTimeout(function () {
                clearTimeout(timeout);
                path = path.data(pie(dataset.upper)); // update the data
                path.transition().duration(2000).attrTween("d", function (a) {
                    // Store the displayed angles in _current.
                    // Then, interpolate from _current to the new angles.
                    // During the transition, _current is updated in-place by d3.interpolate.
                    var i  = d3.interpolate(this._current, a);
                    var i2 = d3.interpolate(progress, value);
                    this._current = i(0);
                    return function(t) {
                        text.text( format(i2(t) / 100) );
                        return arc(i(t));
                    };
                }); // redraw the arcs
            }, 200);

            function calcPercent(percent) {
                return [percent, 100-percent];
            }
        }
    };
})

.directive('rlMonitorChart', function () {

    return {
        restrict: 'EA',
        scope: true,
        link: function ($scope, $element, $attrs, $filter) {
            // attributes + defaults
            var title = $attrs.title;
            var field = $attrs.field;
            var table = $attrs.table;
            var recordKey = $attrs.recordKey; //
            var color = $attrs.color;
            var width = $attrs.width;
            var height = $attrs.height;
            var maxVal = $attrs.max;
            var interval = $attrs.interval;
            var n = $attrs.points;
            var xfilter = $attrs.xfilter;
            var yfilter = $attrs.yfilter;
            var diff = $attrs.diff;

            if ( ! width )
                width = 600;
            if ( ! height )
                height = 150;
            if ( ! title )
                title = 'title';
            if ( ! color )
                color = "steelblue";
            if ( ! maxVal )
                maxVal = 10;
            if ( ! interval )
                interval = 1000;
            if ( ! n )
                n = 120;


            // chart ..

            var data = d3.range(n).map(function(d) { return 0;});

            var margin = {top: 20, right: 50, bottom: 20, left: 40};

            width = width - margin.left - margin.right;
            height = height - margin.top - margin.bottom;

            var x = d3.scale.linear()
                .domain([0, n - 1])
                .range([0, width]);

            var y = d3.scale.linear()
                .domain([0, maxVal])
                .range([height, 0]);

            var line = d3.svg.area()
                .x(function(d, i) {
                    return x(i);
                })
                .y(function(d, i) {
                    return y(d);
                })
                .y0(height);

            var svg = d3.select($element[0]).append("svg")
                .attr("class", "monline")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            svg.append("text")      // title
                .attr("x", width/2 )
                .attr("y",  height/2 )
                .style("text-anchor", "middle")
                .attr("font-size", '20px')
                .attr("font-weight", 'bold')
                .attr("opacity", 0.4)
                .text(title);

            svg.append("defs").append("clipPath")
                .attr("id", "clip")
                .append("rect")
                .attr("width", width)
                .attr("height", height);

            var xaxis =
                d3.svg.axis().scale(x).orient("bottom")
                .ticks(width > 600 ? 10 : 5)
                .tickSize(-height, 0, 0);

            if ( xfilter ) {
                var fun = function(d) {
                    var res = $scope.$eval(xfilter, {val:n-d});
                    return res;
                };
                xaxis.tickFormat(fun);
            } else {
                xaxis.tickFormat(function(d) { return ""+((n-d)*interval)/1000+"s"; });
            }
            svg.append("g")
                .attr("class", "x axis monline")
                .attr("transform", "translate(0," + y(0) + ")")
                .call(xaxis);

            var yaxis = d3.svg.axis().scale(y).orient("left")
                .ticks(height > 150 ? 5 : 2)
                .tickSize(-width, 0, 0);
            if ( yfilter ) {
                yaxis.tickFormat(function(d) { return $scope.$eval(yfilter, {val:d}); });
            }
            svg.append("g")
                .attr("class", "y axis monline")
                .call( yaxis );

            var path = svg.append("g")
                .attr("clip-path", "url(#clip)")
                .append("path")
                .datum(data)
                .attr("fill", color)
                .attr("class", "line monline")
                .attr("d", line );

            // tooltip stuff
            var focus = svg.append("g")
                .attr("class", "focus")
                .style("display", "none");

            focus.append("circle")
                .attr("r", 3.0);

            focus.append("text")
                .attr("x", 9)
                .attr("font-size", '12px')
                .attr("dy", "-1.0em");

            svg.append("rect")
                .attr("class", "overlay")
                .attr("width", width)
                .attr("height", height)
                .on("mouseover", function() { focus.style("display", null); })
                .on("mouseout", function() { focus.style("display", "none"); })
                .on("mousemove", mousemove);

            function mousemove() {
                var x0 = x.invert(d3.mouse(this)[0]),
                    i = Math.floor(x0),
                    d = data[i];
                focus.attr("transform", "translate(" + x(i) + "," + y(d) + ")");
                focus.select("text").text("x:"+i+" y:"+d);
            }

            // animation
            tick();
            function tick() {
                if ( ! lastValue ) {
                    lastValue = 0; prevLast = 0;
                }
                if ( diff ) {
                    // push a new data point onto the back
                    data.push(lastValue-prevLast);
                } else {
                    // push a new data point onto the back
                    data.push(lastValue);
                }

                var max = d3.max(data, function(d) { return d; });
                if ( max > maxVal ) {
                    y.domain([0, max]).nice();
                    var t = svg.transition().duration(500);
                    t.select(".y.axis").call(yaxis);
                }

                // redraw the line, and slide it to the left
                path
                    .attr("d", line)
                    .attr("transform", null)
                    .transition()
                    .duration(interval)
                    .ease("linear")
                    .attr("transform", "translate(" + x(-1) + ",0)")
                    .each("end", tick);

                // pop the old data point off the front
                data.shift();
            }

            // data subscription
            var subsId = 0;
            var lastValue = 0, prevLast = 0;
            $attrs.$observe('recordKey', function(data) {
                recordKey = data;
                if ( RealLive.socketConnected ) {
                    RealLive.unsubscribe(subsId);
                    subsId = RealLive.subscribeKey(table, recordKey, function(change) {
                        switch (change.type) {
                            case RL_ADD:
                            case RL_UPDATE:
                                prevLast = lastValue;
                                lastValue = change.newRecord[field];
                                break;
                            case RL_REMOVE:
                                break;
                            case RL_SNAPSHOT_DONE:
                                break;
                        }
                    });
                }
            }, true);

            $scope.$on("$destroy", function() {
                console.log("destroy "+subsId);
                RealLive.unsubscribe(subsId);
            });

        }
    };
})

.directive('rlBubbles', function () {

    return {
        restrict: 'EA',
        scope: true,
        link: function ($scope, $element, $attrs) {
            var field = $attrs.field;
            var textAttr = $attrs.textAttr;
            var table = $attrs.table;
            var query =$attrs.query;
            var size = $attrs.size; // always quadratic
            var color = $attrs.color;

            var resultSet = new RLResultSet();

            if ( ! size )
                size = 400;
            if ( ! textAttr )
                textAttr = 'recordKey';
            if ( ! color )
                color = "steelblue";


            var canvas = d3.select($element[0]).append("svg")
                .attr('width',  (size)+"px")
                .attr('height', (size)+"px")
                .append("g")
                .attr("transform", "translate(" + 5 + "," + 5 + ")"); // origin

            var pack = d3.layout.pack().size([size-10,size-10]).padding(5);

            var tip = d3.tip()
                .attr('class', 'd3-tip')
                .html(function(d) {
                    if (d.children)
                        return '';
                    return d[textAttr] +' '+d[field];
                })
                .offset([0, 3]);

            var subsId = 0;
            var bubblesQueue = [];
            $attrs.$observe('query', function(data) {
                query = data;
                if ( RealLive.socketConnected ) {
                    RealLive.unsubscribe(subsId);
                    subsId = RealLive.subscribeSet(table,query,resultSet, $scope );
                }
                resultSet.postChangeHook = function (change, snapFin) {
                    setTimeout( function() {
                        if ( bubblesQueue.length < 2 ) {
                            bubblesQueue.push(function () {
                                bubbles(resultSet.list);
                            });
                        }
                        if ( bubblesQueue.length == 1 ) {
                            bubblesQueue[0].apply();
                            bubblesQueue = [];
                        }
                    }, 1000);
                };
            }, true);

            $scope.$on("$destroy", function() {
                console.log("destroy "+subsId);
                RealLive.unsubscribe(subsId);
            });

            var bubbles = function(dataSet) {

                var max = 0;
                for ( var i = 0; i < dataSet.length; i++ ) {
                    var newVar = dataSet[i][field];
                    if ( newVar > max )
                        max = newVar;
                }

                var fac = 1;
                if ( max > 1 ) {
                    fac = size/max;
                }

                for ( var i = 0; i < dataSet.length; i++ ) {
                    var newVar = dataSet[i][field];
                    dataSet[i].value = newVar * fac;
                }

                var nodeSet = pack.nodes({ name: 'top', children: dataSet });

                canvas.call(tip);

                var nodes = canvas.selectAll('.node').data(nodeSet);
                var node = nodes.enter()
                    .append('g')
                    .attr("class", "node")
                    .attr("transform", function(d) { return "translate("+ d.x +"," + d.y + ")"; } );

                node
                    .append("circle")
                    .attr("r", function(d) { return d.r; })
                    .attr("fill", function(d) { return d.children ? "#fff": color; } )
                    .attr("stroke", "black" ) //function(d) { return d.children ? "#fff": color; })
                    .attr("stroke-width", "2")
                    .attr("stroke-opacity", ".8")
                    .attr("opacity",.25)
                    .on('mousedown', tip.show )
                    .on('mouseout', tip.hide );
                node
                    .append("text")
                    .text( function(d) { return d.children ? "" : d[textAttr];}).attr("style", 'text-anchor: middle;')
                    .on('mousedown', tip.show )
                    .on('mouseout', tip.hide );


                var trans = nodes.transition();

                // ... update circle radius
                trans.select("circle")
                    .transition()
                    .attr("r", function(d) { return d.r; });

                trans.select("text").transition()
                    .text( function(d) { return d.children ? "" : d[textAttr];}).attr("style", 'text-anchor: middle;');

                trans.duration(500)
                    .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })
                    .attr("r", function(d) { return d.r; })
                    .each( "end", function() {
                        if ( bubblesQueue.length > 0 ) {
                            bubblesQueue[0].apply();
                            bubblesQueue = [];
                        }
                    });

                var exit = nodes.exit();

                exit.select(".node")
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
})

.directive('rlPopover', function ($compile,$templateCache,$http) {

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
})

.directive('rlHi', function() {
    var rl_elemid = 1;

    function genId(prefix) {
        rl_elemid++;
        return prefix.concat(rl_elemid.toString());
    }

    return {
        transclude:true,
//        replace: true,
//        template: '<b>uh-<div ng-transclude></div>-uh</b>'
        template: '<span style="border-radius: 4px; transition: background-color .3s linear, color .3s linear; padding: 4px;" id="{{itid}}" ng-transclude></span>',
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
                        RealLive.highlightElem(elementId,$attrs.color);
                    }
                }
            );
//            setTimeout(function() {
//                document.getElementById($scope.itid).style.backgroundColor = "#F2E38A";
//            },5000);
        }
    }
})

.directive( 'rlRecord', function()  {
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
            $scope.$on("$destroy", function() {
                console.log("destroy "+$scope.subsId);
                RealLive.unsubscribe($scope.subsId);
            });
        }
    }
})

.directive('rlTable', function() {
    return {
        restrict: 'E',
        scope: true,
        controller: function( $scope, $attrs, $element ) {
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
                            '<div class="ngCellText" style="text-align: '+copiedCol._align + '; '+(copiedCol._bgColor?'background-color:'+copiedCol._bgColor+';':' ')+(copiedCol._txtColor?'color:'+copiedCol._txtColor+';':' ')+'"'+
                            'ng-class="col.colIndex()"><span style="transition: background-color .3s linear, color .3s linear; padding: 3px; " ' +
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
                    '<span id="row#{{row.entity.recordKey}}" style="transition: background-color .3s linear, color .3s linear; padding: 3px;">'+
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
                            if ( cell ) {
                                var fieldMeta = $scope.model.tables[$attrs.table].columnsNGTableConf[cell.$index];
                                if ( fieldMeta._txtColor ) {
                                    RealLive.highlightElem(elementId, fieldMeta._txtColor );
                                } else {
                                    RealLive.highlightElem(elementId);
                                }
                            }
                        }
//                        $scope.$digest();
                    } else
                    if (change.type == RL_SNAPSHOT_DONE) {
                        $scope.$digest();
                    } else
                    if (change.type == RL_ADD ) {
                        var elementId = 'row#' + change.recordKey;
                        if ($scope.rlset.snapFin) {
//                            RealLive.highlightElem(elementId);
                            $scope.$digest();
                        }
                    } else
                    if (change.type == RL_REMOVE ) {
                        var elementId = 'row#' + change.recordKey;
                        if ($scope.rlset.snapFin) {
//                            RealLive.highlightElem(elementId);
                            $scope.$digest();
                        }
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
//        template: '<div class="gridStyle" ng-grid="gridOptions"></div>'
    }
});






