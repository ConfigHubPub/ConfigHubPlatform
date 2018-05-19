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
    .module('configHub.organization.create', [])

    .controller('CreateOrganizationsController',
    ['$scope', '$rootScope', '$stateParams', '$http', '$window', '$state', 'accountNameCheckService',
        function ($scope, $rootScope, $stateParams, $http, $window, $state, accountNameCheckService)
        {
            $scope.organization = {};
            $scope.errorName = "";
            var lastRequest;

            $scope.validateNameChange = function ()
            {
                if (!$scope.organization.name)
                    $scope.errorName = "";

                else {
                    accountNameCheckService.cancel(lastRequest);
                    lastRequest = accountNameCheckService.isNameTaken($scope.organization.name);
                    lastRequest.then(
                        function handleCollaboratorsResolve(ret)
                        {
                            switch (ret) {
                                case 1: $scope.errorName = ""; break;
                                case 2: $scope.errorName = nameError; break;
                                case 3: $scope.errorName = "Name is already taken"; break;

                                default: $scope.errorName = ""; break;
                            }
                        }
                    );
                    return ( lastRequest );
                }
            };

            $scope.createOrganization = function ()
            {
                $http({
                    method: 'POST',
                    url: '/rest/createOrganization/' + $scope.organization.name,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success) {
                        $scope.message = response.data.message;
                    }
                    else {
                        $scope.organization = response.data.organization;
                        $state.go('owner', {accountName: $scope.organization.un });
                    }
                });
            };
        }])

;