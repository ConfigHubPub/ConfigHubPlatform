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
    .module('configHub.login', [])

    .controller('LoginController', ['$http', '$state', '$scope', '$httpParamSerializer',
        function ($http, $state, $scope, $httpParamSerializer)
        {
            $scope.email;
            $scope.password;

            $scope.login = function()
            {
                $http({
                    method: 'POST',
                    url: '/rest/login',
                    data: $httpParamSerializer({
                        email: $scope.email,
                        password: $scope.password
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {

                        $scope.processLoginToken(response.data.token);
                        $state.go('dashboard');

                    } else {
                        $scope.message = response.data.message;
                    }
                })
            };
            
        }])

    .controller('PassResetController', ['$http', '$state', '$scope', '$httpParamSerializer',
        function ($http, $state, $scope, $httpParamSerializer)
        {
            $scope.accountEmail;
            $scope.message = '';
            $scope.sent = false;

            $scope.resetPassword = function ()
            {
                $http({
                    method: 'POST',
                    url: '/rest/resetLoginPassword',
                    data: $httpParamSerializer({
                        email: $scope.accountEmail
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {
                        $scope.message = '';
                        $scope.sent = true;
                    } else {
                        $scope.message = response.data.message;
                        $scope.sent = false;
                    }
                })
            };

        }])

    .controller('PassChangeController', ['$http', '$state', '$scope', '$httpParamSerializer', '$stateParams',
        function ($http, $state, $scope, $httpParamSerializer, $stateParams)
        {
            $scope.password = '';
            $scope.password2 = '';
            $scope.message = '';
            $scope.disableBtns = false;

            var token = $stateParams.t;

            $http({
                method: 'POST',
                url: '/rest/validatePassChangeToken',
                data: $httpParamSerializer({
                    t: token
                }),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).then(function (response)
            {
                if (!response.data.success) {
                    $scope.message = response.data.message;
                    $scope.disableBtns = true;
                }
            });


            $scope.savePassword = function()
            {
                $http({
                    method: 'POST',
                    url: '/rest/updateLoginPassword',
                    data: $httpParamSerializer({
                        password: $scope.password,
                        password2: $scope.password2,
                        t: token
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {
                        $scope.message = '';
                        $scope.processLoginToken(response.data.token);
                        $state.go('dashboard');
                    } else {
                        $scope.message = response.data.message;
                        $scope.password = '';
                        $scope.password2 = '';
                    }
                })
            };
            
        }])

;
