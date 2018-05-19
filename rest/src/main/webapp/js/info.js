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

    .controller('ConfigHubInfoController', ['$scope', '$http',
        function ($scope, $http)
        {
            $scope.initialized = false;
            $scope.errorMessage = '';
            $scope.data = [];

            $http
                .get("/rest/info/all")
                .then(function(response) {
                    $scope.data = response.data;
                    $scope.initialized = true;
                }, function(response) {
                    $scope.errorMessage = "Something went wrong";
                    $scope.initialized = true;
                });


            $http
                .get("/rest/info/system")
                .then(function(response) {
                    $scope.system = response.data;
                }, function(response) {
                    $scope.errorMessage = "Something went wrong";
                });

        }])
;