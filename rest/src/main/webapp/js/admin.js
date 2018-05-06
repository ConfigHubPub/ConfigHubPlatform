angular
    .module('configHub.admin', [])

    .controller('LdapController',
        ['$scope', '$rootScope', '$stateParams', '$http', '$filter', '$httpParamSerializer',
            function ($scope, $rootScope, $stateParams, $http, $filter, $httpParamSerializer) {

                var form, arr, i;
                $scope.ldap = {};

                initLDAP();

                function initLDAP()
                {
                    $http
                        .get("/rest/getLDAPConfig")
                        .then(function (response) {
                            $scope.ldapForm = response.data;
                        });
                }

                $scope.enableLdapToggle = false;
                $scope.enableLdap = function() {

                };

                $scope.runningTest = false;
                $scope.systemTestCompleted = false;
                $scope.testResponse;
                $scope.connectionOnlyTest = false;

                $scope.testLdap = function (connectionOnly) {

                    $scope.systemTestCompleted = false;
                    $scope.connectionOnlyTest = connectionOnly;
                    $scope.runningTest = true;

                    form = angular.copy($scope.ldapForm);

                    form.testConnectionOnly = connectionOnly;
                    form.principal = $scope.principal;
                    form.password = $scope.password;

                    $http({
                        method: 'POST',
                        url: '/rest/getLDAPConfig/testLdap',
                        data: form,
                        headers: {'Content-Type': 'application/json'}
                    }).then(function (response)
                    {
                        $scope.runningTest = false;
                        $scope.systemTestCompleted = true;

                        $scope.testResponse = response.data.success;
                        if (!response.data.success) {
                            $scope.testErrorResponse = response.data.errorMessage;
                        }
                        else {
                            $scope.testErrorResponse = null;
                        }
                    });

                };


                $scope.ldapSaveErrorMessage = null;
                $scope.saveLdap = function () {

                    form = angular.copy($scope.ldapForm);

                    $http({
                        method: 'POST',
                        url: '/rest/saveLDAPConfig',
                        data: form,
                        headers: {'Content-Type': 'application/json'}
                    }).then(function (response)
                    {
                        if (!response.data.success) {
                            $scope.ldapSaveErrorMessage = response.data.errorMessage;
                        }
                        else {
                            $scope.ldapSaveErrorMessage = null;
                        }
                    });

                };

            }
        ])

    .controller('AdminController',
        ['$scope', '$rootScope', '$stateParams', '$http', '$filter', '$httpParamSerializer',
            function ($scope, $rootScope, $stateParams, $http, $filter, $httpParamSerializer) {

                var orderBy = $filter('orderBy'),
                    form,
                    i,
                    un;

                $scope.admins = [];
                $scope.isAdmin = false;

                init();

                function init()
                {
                    initAdmins();
                }

                /**
                 * Administrator access
                 */
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