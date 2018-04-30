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
                $scope.selectedIndex = 1;

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