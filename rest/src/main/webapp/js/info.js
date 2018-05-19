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
    .module('configHub.info', [])

    .controller('ConfigHubInfoController', ['$scope', '$rootScope', '$http', '$state', '$stateParams',
        function ($scope, $rootScope, $http, $state, $stateParams)
        {

            $scope.settings = [
                {
                    name: 'Repositories',
                    s: 'repositories'
                },
                {
                    name: 'LDAP / Active Directory',
                    s: 'ldap'
                },
                {
                    name: 'Administrators',
                    s: 'admins'
                },
                {
                    name: 'Upgrade',
                    s: 'upgrade'
                }
            ];

            if ($stateParams.s)
                $scope.selectedIndex = indexOf($scope.settings, 's', $stateParams.s);
            else
                $scope.selectedIndex = 0;

            function selectSection()
            {
                $scope.settings[$scope.selectedIndex].selected = true;
                $scope.selectSetting = function (index) {
                    $scope.settings[$scope.selectedIndex].selected = false;
                    $scope.settings[index].selected = true;
                    $scope.selectedIndex = index;

                    $state.go('system.settings',
                        {s: $scope.settings[index].s},
                        {notify: false});
                };
            }

            selectSection();

            $scope.initialized = false;
            $scope.errorMessage = '';
            $scope.data = [];

            // Repositories
            $http
                .get("/rest/info/all")
                .then(function(response) {
                    $scope.data = response.data;
                    $scope.initialized = true;
                }, function(response) {
                    $scope.errorMessage = "Something went wrong";
                    $scope.initialized = true;
                });


            // Version
            $http
                .get("/rest/info/system")
                .then(function(response) {
                    $scope.system = response.data;
                }, function(response) {
                    $scope.errorMessage = "Something went wrong";
                });

        }])
;