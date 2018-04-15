angular
    .module('configHub.admin', [])

    .controller('AdminController',
        ['$scope', '$stateParams', '$http', '$filter', '$httpParamSerializer',
            function ($scope, $stateParams, $http, $filter, $httpParamSerializer) {

                var orderBy = $filter('orderBy'),
                    i,
                    un;

                $scope.admins = [];
                $scope.isAdmin = false;

                init();

                function init()
                {
                    initAdmins();
                }

                function initAdmins()
                {
                    $http
                        .get("/rest/getSystemAdmins")
                        .then(function (response) {
                            $scope.admins = response.data.admins;
                            $scope.isAdmin = response.data.isAdmin;
                        });
                }

                $scope.getUsers = function (val) {
                    return $http.get('/rest/userSearch', {
                        params: {t: val}
                    }).then(function (response) {
                        return response.data;
                    });
                };

                $scope.addAdmin = function (f) {
                    $scope.adminError = '';
                    un = f.un ? f.un : f;
                    $http({
                        method: 'POST',
                        url: '/rest/addSystemAdmin',
                        data: $httpParamSerializer({un: un}),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response) {
                        if (!response.data.success) {
                            $scope.adminError = response.data.message;
                        }
                        else {
                            $scope.adminError = '';
                            init();
                        }
                    });
                };

                $scope.removeAdmin = function (admin) {
                    $http({
                        method: 'POST',
                        url: '/rest/removeSystemAdmin',
                        data: $httpParamSerializer(admin),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response) {
                        if (!response.data.success) {
                            $scope.adminError = response.data.message;
                        }
                        else {
                            init();
                        }
                    });
                };

            }

        ])

;