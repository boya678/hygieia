(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('RepoViewController', RepoViewController);

    RepoViewController.$inject = ['$q', '$scope', 'codeRepoData', 'pullRepoData', 'issueRepoData', 'collectorData', '$uibModal'];
    function RepoViewController($q, $scope, codeRepoData, pullRepoData, issueRepoData, collectorData, $uibModal) {
        var ctrl = this;

        ctrl.combinedChartOptions = {
            plugins: [
                Chartist.plugins.gridBoundaries(),
                Chartist.plugins.lineAboveArea(),
                Chartist.plugins.pointHalo(),
                Chartist.plugins.ctPointClick({
                    onClick: showDetail
                }),
                Chartist.plugins.axisLabels({
                    stretchFactor: 1.4,
                    axisX: {
                        labels: [
                            moment().subtract(14, 'days').format('MMM DD'),
                            moment().subtract(7, 'days').format('MMM DD'),
                            moment().format('MMM DD')
                        ]
                    }
                }),
                Chartist.plugins.ctPointLabels({
                    textAnchor: 'middle'
                })
            ],

            showArea: false,
            lineSmooth: false,
            fullWidth: true,
            axisY: {
                offset: 30,
                showGrid: true,
                showLabel: true,
                labelInterpolationFnc: function (value) {
                    return Math.round(value * 100) / 100;
                }
            }
        };
        ctrl.minitabs = [];
        ctrl.isvisible = true;
        ctrl.combinedChartData2 = [{}];
            
        ctrl.commits = [];
        ctrl.pulls = [];
        ctrl.issues = [];

        //functions
        ctrl.showDetail = showDetail;
        ctrl.miniToggleView = miniToggleView;

        var commits = [];
        var groupedCommitData = [];

        function miniToggleView(index){
            ctrl.miniWidgetView = typeof ctrl.minitabs[index] === 'undefined' ? ctrl.minitabs[0].name : ctrl.minitabs[index].name;
            ctrl.combinedChartData2 = ctrl.commitChartData[ctrl.miniWidgetView];
            ctrl.isvisible = true;
        }


        ctrl.load = function () {
            var deferred = $q.defer();
            var params = {
                componentId: $scope.widgetConfig.componentId,
                numberOfDays: 14
            };

            codeRepoData.details(params).then(function (data) {
                processCommitResponse(data.result, params.numberOfDays);
                ctrl.lastUpdated = data.lastUpdated;
            }).then(function () {
                collectorData.getCollectorItem($scope.widgetConfig.componentId, 'scm').then(function (data) {
                    deferred.resolve(ctrl.lastUpdated);
                });
            });
            return deferred.promise;
        };

        function showDetail(evt) {
            var target = evt.target,
                pointIndex = target.getAttribute('ct:point-index');

            var seriesIndex = target.getAttribute('ct:series-index');

            //alert(ctrl);
            $uibModal.open({
                controller: 'RepoDetailController',
                controllerAs: 'detail',
                templateUrl: 'components/widgets/repo/detail.html',
                size: 'lg',

                resolve: {
                    commits: function () {
                        if (seriesIndex == "0") {
                            return groupedCommitData[ctrl.miniWidgetView][pointIndex];
                        }
                    }
                }
            });
        }

        function processCommitResponse(data, numberOfDays) {
            commits = {};
            groupedCommitData = {};
            if (data.length == 0)
                return;
            // get total commits by day
            var groups = _(data).sortBy('scmBranch')
                .groupBy(function (item) {
                    return item.scmBranch;
                }).value();

            //obtener las ramas para los tabs
            var keys = Object.keys(groups);
            ctrl.minitabs = [];
            angular.forEach(keys, function (value, key) {
                ctrl.minitabs.push({ "name": value, "data": [] });
            });
            ctrl.miniWidgetView = ctrl.minitabs[0].name;

            //ordenamiento del contenido de cada rama
            angular.forEach(keys, function (value, key) {
                var elements = groups[value];
                groups[value] = _(elements).sortBy('timestamp')
                    .groupBy(function (item) {
                        return -1 * Math.floor(moment.duration(moment().diff(moment(item.scmCommitTimestamp))).asDays());
                    }).value();
            });
            angular.forEach(keys, function (value, key) {
                var x = 0;
                for (x = -1 * numberOfDays + 1; x <= 0; x++) {
                    if (!(value in commits)) {
                        commits[value] = [];
                        groupedCommitData[value] = [];
                    }
                    if (groups[value][x]) {
                        commits[value].push(groups[value][x].length);
                        groupedCommitData[value].push(groups[value][x]);
                    }
                    else {
                        commits[value].push(0);
                        groupedCommitData[value].push([]);
                    }
                }
            });
            var labels = {};
            angular.forEach(keys, function (value, key) {
                _(commits[value]).forEach(function (c) {
                    if (!(value in labels)) {
                        labels[value] = [];
                    }
                    labels[value].push('');
                });
            });
            //update charts

            angular.forEach(keys, function (value, key) {
                if (commits[value].length) {
                    if (ctrl.commitChartData === undefined && ctrl.combinedChartData === undefined) {
                        ctrl.commitChartData = {};
                        ctrl.combinedChartData = {};
                    }
                    if (!(value in ctrl.commitChartData)) {
                        ctrl.commitChartData[value] = [];
                    }
                    ctrl.commitChartData[value] = {
                        series: [commits[value]],
                        labels: labels[value]
                    };
                }
                if (!(value in ctrl.combinedChartData)) {
                    ctrl.combinedChartData[value] = [];
                }
                ctrl.combinedChartData[value] = {
                    labels: labels[value],
                    series: [{
                        name: 'commits',
                        data: commits[value]
                    }]
                };
                ctrl.combinedChartData2 = ctrl.combinedChartData[ctrl.miniWidgetView]
            });

            // group get total counts and contributors
            var today = toMidnight(new Date());
            var sevenDays = toMidnight(new Date());
            var fourteenDays = toMidnight(new Date());
            sevenDays.setDate(sevenDays.getDate() - 7);
            fourteenDays.setDate(fourteenDays.getDate() - 14);

            var lastDayCommitCount = {};
            var lastDayCommitContributors = {};

            var lastSevenDayCommitCount = {};
            var lastSevenDaysCommitContributors = {};

            var lastFourteenDayCommitCount = {};
            var lastFourteenDaysCommitContributors = {};

            // loop through and add to counts
            var groupsByBranch = _(data).sortBy('scmBranch')
                .groupBy(function (item) {
                    return item.scmBranch;
                }).value();
            angular.forEach(keys, function (value, key) {
                _(groupsByBranch[value]).forEach(function (commit) {
                    if (!(value in lastDayCommitCount) && !(value in lastSevenDayCommitCount) && !(value in lastFourteenDayCommitCount)
                        && !(value in lastDayCommitContributors) && !(value in lastSevenDaysCommitContributors)
                        && !(value in lastFourteenDaysCommitContributors)) {
                        lastDayCommitCount[value] = 0;
                        lastSevenDayCommitCount[value] = 0;
                        lastFourteenDayCommitCount[value] = 0;
                        lastDayCommitContributors[value] = [];
                        lastSevenDaysCommitContributors[value] = [];
                        lastFourteenDaysCommitContributors[value] = [];
                    }

                    if (commit.scmCommitTimestamp >= today.getTime()) {
                        lastDayCommitCount[value]++;
                        if (lastDayCommitContributors[value].indexOf(commit.scmAuthor) == -1) {
                            lastDayCommitContributors[value].push(commit.scmAuthor);
                        }
                    }

                    if (commit.scmCommitTimestamp >= sevenDays.getTime()) {
                        lastSevenDayCommitCount[value]++;
                        if (lastSevenDaysCommitContributors[value].indexOf(commit.scmAuthor) == -1) {
                            lastSevenDaysCommitContributors[value].push(commit.scmAuthor);
                        }
                    }
                    if (commit.scmCommitTimestamp >= fourteenDays.getTime()) {
                        lastFourteenDayCommitCount[value]++;
                        ctrl.commits.push(commit);
                        if (lastFourteenDaysCommitContributors[value].indexOf(commit.scmAuthor) == -1) {
                            lastFourteenDaysCommitContributors[value].push(commit.scmAuthor);
                        }
                    }
                });
            });
            ctrl.lastDayCommitCount = lastDayCommitCount;
            ctrl.lastDayCommitContributorCount = lastDayCommitContributors;
            ctrl.lastSevenDaysCommitCount = lastSevenDayCommitCount;
            ctrl.lastSevenDaysCommitContributorCount = lastSevenDaysCommitContributors;
            ctrl.lastFourteenDaysCommitCount = lastFourteenDayCommitCount;
            ctrl.lastFourteenDaysCommitContributorCount = lastFourteenDaysCommitContributors;
            function toMidnight(date) {
                date.setHours(0, 0, 0, 0);
                return date;
            }
        }
    }
})();
