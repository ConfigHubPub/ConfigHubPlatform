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
    .module('configHub.signup', [])

    .controller('SignupCtrl', ['$http', '$state', '$scope', 'store', 'jwtHelper', '$rootScope', '$httpParamSerializer',
        function ($http, $state, $scope, store, jwtHelper, $rootScope, $httpParamSerializer)
    {
        $scope.initialized = false;
        $scope.accountsEnabled = false;

        $http
            .get("/rest/localAccountsAvailable")
            .then(function (response) {
                $scope.accountsEnabled = response.data.enabled;
                $scope.initialized = true;
            });


        $scope.name = '';
        $scope.email = '';
        $scope.username = '';
        $scope.password = '';

        $scope.signup = function()
        {
            var postData = {
                email: $scope.email,
                username: $scope.username,
                password: $scope.password
            };

            $http({
                method: 'POST',
                url: '/rest/signup',
                data: $httpParamSerializer(postData),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(function (response)
            {
                if (!response.data.success)
                {
                    // if not successful, bind errors to error variables
                    // $scope.errorName = response.data.error_name;
                    $scope.errorEmail = response.data.error_email;
                    $scope.errorUsername = response.data.error_username;
                    $scope.errorPassword = response.data.error_password;
                    $scope.message = response.data.message;
                } else
                {
                    $scope.processLoginToken(response.data.token);
                    if (!$rootScope.goToLastLocation())
                        $state.go('dashboard');
                }
            })
;
        };
    }]);