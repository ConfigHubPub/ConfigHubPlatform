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