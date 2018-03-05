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