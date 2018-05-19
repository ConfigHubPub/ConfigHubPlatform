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
    .module('configHub.repository.contextItems', [])

    .controller('ContextItemsController', ['$stateParams', '$scope', '$http', '$rootScope',
        function ($stateParams, $scope, $http, $rootScope)
        {
            $scope.depthData = {};
            $scope.depthScores = [];
            $scope.initialized = false;
            $rootScope.selectedTab = 0;

            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;

            $http
                .get('/rest/contextItems/' + $stateParams.owner + "/" + $stateParams.name)
                .then(function successCallback(response)
                {
                    $scope.depthData = response.data.depthData;
                    $scope.depthScores = response.data.depthScores;
                    $scope.canManageContext = response.data.canManageContext;
                    $scope.initialized = true;
                });

            $scope.loadingIsDone = function() { return $scope.loadingIsDone; }
            $scope.repoName = $stateParams.name;
        }])

    // filter all levels on name only
    .filter('levelFilter', function ()
    {
        return function (items, field, reverse)
        {
            if (!field)
                return items;

            var filtered = [],
                re = new RegExp(field, 'i'),
                ki = 0,
                item;

            for (; ki < items.length; ki++) {
                item = items[ki];
                if (re.test(item.name)) {
                    filtered.push(item);
                }
            }

            return filtered;
        }
    })

    ;
