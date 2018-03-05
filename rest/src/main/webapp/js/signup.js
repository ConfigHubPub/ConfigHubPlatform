angular
    .module('configHub.signup', [])

    .controller('SignupCtrl', ['$http', '$state', '$scope', 'store', 'jwtHelper', '$rootScope', '$httpParamSerializer',
        function ($http, $state, $scope, store, jwtHelper, $rootScope, $httpParamSerializer)
    {
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