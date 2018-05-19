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

    .module('configHub.dashboard', [])

    .run(['$rootScope', function($rootScope) {
        $rootScope.repository = {};
    }])

    .config(['$stateProvider', function ($stateProvider)
    {
        $stateProvider.state('dashboard', {
            url: '/home/',
            controller: 'DashboardCtrl',
            templateUrl: 'dashboard/dashboard.html',
            data: {
                requireLogin: true
            }
        });
    }])


    .controller('DashboardCtrl', ['$http', '$scope', '$filter', 'store',
        function ($http, $scope, $filter, store)
    {
        $scope.repositories = null;
        $scope.selectedRepo = null;
        $scope.initialized = false;

        $scope.account;
        $scope.repoName;

        $http({
            method: 'GET',
            url: '/rest/getDashboardElements'
        }).then(function successCallback(response) {

            $scope.sideBySide = store.get("splitView");
            if (!$scope.sideBySide)
                $scope.sideBySide = false;

            $scope.$on('sideBySide', function (a, sideBySide) {
                $scope.sideBySide = sideBySide;
            });
            $scope.repositories = response.data.repositories;

            if ($scope.repositories)
            {
                var orderBy = $filter('orderBy'),
                    index = 0,
                    lastRepo = store.get('lastRepo');

                $scope.repositories = orderBy($scope.repositories, 'name');

                if (lastRepo)
                {
                    index = indexOf($scope.repositories, 'id', lastRepo.id);
                    if (index < 0) index = 0;
                }

                $scope.selectRepo($scope.repositories[index]);
            }

            $scope.initialized = true;
        }, function errorCallback(response) {
            console.log("Caught error:");
            console.log(response);
        });



        $scope.auditRefreshCnt = 0;
        $scope.selectRepo = function(repo)
        {
            $scope.selectedRepo = repo;
            $scope.account = repo.account;
            $scope.repoName = repo.name;
            $scope.auditRefreshCnt++;
        };
    }])

    ;
