/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

angular
    .module('configHub.repository.contextItem', [
    ])

    .controller('ExistingCiController', ['$state', '$scope', '$rootScope', '$http', '$stateParams', '$httpParamSerializer', 'editorInit',
        function ($state, $scope, $rootScope, $http, $stateParams, $httpParamSerializer, editorInit)
        {

            $scope.labels = [];
            $scope.series = [];
            $scope.data = [];
            $scope.charOptions = { };


            $scope.isNew = null == $stateParams.contextItem ? true : false;
            $rootScope.selectedTab = 0;

            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;

            $scope.initialized = false;
            $scope.message = '';
            $scope.newName = '';
            $scope.ci = {};
            $scope.standalone = true;

            $scope.usageLoaded = false;

            $scope.assigned = {
                'Group': [],
                'Member': []
            };

            $scope.validateNameChange = function ()
            {
                if ($scope.isNew) return;
                // ToDo ???
            };


            if ($scope.isNew)
            {
                $http
                    .get('/rest/canUserManageContext/' + $scope.account + "/" + $scope.repoName)
                    .then(function (response)
                    {
                        $scope.ci = {
                            name: '',
                            type: 'Standalone',
                            depthLabel: $stateParams.depthLabel,
                            contextClustersEnabled: $scope.repository.contextClustersEnabled
                        };

                        $scope.canManageContext = response.data.canManageContext;
                        $scope.standalone = true;
                        $scope.ciCount = 0;
                        $scope.newName = '';

                        $scope.initialized = true;
                    });
            }
            else
            {
                editorInit.setState('contextItem');
                $http
                    .get('/rest/getContextItem/' + $scope.account + "/" + $scope.repoName + '/' + $stateParams.depthLabel + '/' + $stateParams.contextItem)
                    .then(function (response)
                    {
                        if (response.data.success) {
                            $scope.ci = response.data.ci;
                            $scope.newName = $scope.ci.name;
                            $scope.standalone = $scope.ci.type == 'Standalone';
                            $scope.canManageContext = response.data.canManageContext;

                            if (!$scope.standalone)
                                $scope.assigned[$scope.ci.type] = $scope.ci.assignments;

                            $scope.ciCount = $scope.ci.assignments ? $scope.ci.assignments.length : 0;
                            editorInit.initialize($scope);
                        }
                        else {
                            $scope.message = response.data.message;
                        }

                        $scope.initialized = true;
                    });
            }

            var o,
                i,
                index,
                toAssign = [],
                e;

            $scope.cancel = function()
            {
                o = $rootScope.getLastInfo();
                if (o && o.name === 'repo.contextItem' &&
                    o.params.depthLabel === $stateParams.depthLabel &&
                    o.params.contextItem === $stateParams.contextItem)
                    $state.go('repo.context', {owner: $scope.account, name: $scope.repoName});

                else if (!$rootScope.goToLastLocation())
                    $state.go('repo.context', {owner: $scope.account, name: $scope.repoName});

            };

            $scope.deleteCi = function() {

                $http({
                    method: 'POST',
                    url: '/rest/deleteContextItem/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({id: $scope.ci.id}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {
                        if (!$rootScope.goToLastLocation())
                            $state.go('repo.context', {owner: $scope.account, name: $scope.repoName});
                    }
                    else {
                        $scope.errorMessage = response.data.message;
                    }
                });
            };

            $scope.getCiType = function() {
                if ($scope.ci.type == 'Group')
                    return "Context Group";

                if ($scope.ci.type == 'Member')
                    return "Group Member";

                return "Default";
            };

            $scope.setType = function(newType)
            {
                $scope.ci.type = newType;
                $scope.standalone = $scope.ci.type == 'Standalone';
                refreshAssignments();
            };

            $scope.showAllCis = false;
            $scope.toggleAllCis = function()
            {
                $scope.showAllCis = !$scope.showAllCis;
                refreshAssignments();
            };

            $scope.iterableAssignments = function()
            {
                if ($scope.showAllCis)
                    return $scope.ci.assignments;

                return $scope.assigned[$scope.ci.type];
            };

            function refreshAssignments()
            {
                $http({
                    method: 'POST',
                    url: '/rest/allDepthLevels/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        id: $scope.ci.id,
                        type: $scope.ci.type,
                        depthLabel: $stateParams.depthLabel,
                        all: $scope.showAllCis
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {
                        $scope.ci.assignments = response.data.levels;
                        for (i in $scope.assigned[$scope.ci.type])
                        {
                            o = $scope.assigned[$scope.ci.type][i];
                            index = indexOf($scope.ci.assignments, 'id', o.id);
                            if (-1 != index)
                            {
                                $scope.ci.assignments[index].state = o.state;
                            }
                        }

                        $scope.ciCount = $scope.ci.assignments ? $scope.ci.assignments.length : 0;
                    }
                    else {
                        $scope.message = response.data.message;
                    }
                });
            }

            $scope.assignMessage = '';

            $scope.isCluster = function() { return $scope.ci.type === 'Group'; };
            $scope.isNode = function() { return $scope.ci.type === 'Member'; };

            $scope.save = function()
            {
                if ($scope.ci.type != 'Standalone')
                {
                    e = $scope.assigned[$scope.ci.type];
                    for (i in e) toAssign.push(e[i].id);
                }

                $http({
                    method: 'POST',
                    url: '/rest/saveOrUpdateContextItem/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        id: $scope.ci.id,
                        name: $scope.newName,
                        type: $scope.ci.type,
                        assignments: toAssign.join(","),
                        depthLabel: $stateParams.depthLabel
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        $scope.cancel();
                    }
                    else {
                        $scope.errorMessage = response.data.message;
                    }
                });
            };


            $scope.assign = function(assigned, opp)
            {
                if ($scope.ci.type == 'Standalone')
                    return;
                if ('add' === opp)
                {
                    $scope.assigned[$scope.ci.type].push(assigned);
                    assigned.state = 2;
                }
                else {
                    index = indexOf($scope.assigned[$scope.ci.type], 'id', assigned.id);
                    if (-1 != index)
                    {
                        $scope.assigned[$scope.ci.type].splice(index, 1);
                        assigned.state = 1;
                    }
                }
            };


            // --------------------------------
            // Pagination
            // --------------------------------
            $scope.ciReverse = false;
            $scope.ciCurrentPage = 1;
            $scope.ciPageSizes = {
                sizes: [{id: 10, name: '10'}, {id: 25, name: '25'}, {id: 50, name: '50'}],
                selectedOption: {id: 10, name: '10'}
            };
            $scope.ciCount = 0;

        }
    ])
;
