angular
    .module('configHub.admin', [])

    .controller('LdapController',
        ['$scope', '$rootScope', '$stateParams', '$http', '$timeout',
            function ($scope, $rootScope, $stateParams, $http, $timeout) {

                var form;
                $scope.ldap = {};
                $scope.ldapEnabled = false;
                $scope.isAdmin = false;
                initLDAP();

                function initLDAP()
                {
                    $http
                        .get("/rest/getLDAPConfig")
                        .then(function (response) {

                            $scope.ldapForm = response.data;
                            switch (response.status)
                            {
                                case 403:
                                    $scope.isAdmin = false;
                                    $scope.isAdmin = false;
                                    break;

                                case 500:
                                    $scope.error = "Failed to fetch system configuration.";

                                default:
                                    $scope.isAdmin = true;
                            }

                            $scope.ldapEnabled = $scope.ldapForm && $scope.ldapForm.ldapEnabled;
                        });
                }

                $scope.enableLdap = function() {
                    $scope.ldapForm.ldapEnabled = $scope.ldapEnabled;
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
                        if (!connectionOnly)
                        {
                            $scope.testResponseDetails = {
                                "nameAttribute": response.data.nameAttribute,
                                "emailAttribute": response.data.emailAttribute
                            };
                            $scope.ldapEntry = response.data.entry;
                        }
                        if (!response.data.success) {
                            $scope.testErrorResponse = response.data.errorMessage;
                        }
                        else {
                            $scope.testErrorResponse = null;
                        }
                    });
                };


                $scope.ldapSaveErrorMessage = null;
                $scope.saving = false;
                $scope.saveLdap = function () {
                    $scope.saving = true;
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

                        $timeout(function()
                        {
                            $scope.saving = false;
                        }, 1000);

                    });

                };

            }
        ])

    .controller('AdminController',
        ['$scope', '$rootScope', '$stateParams', '$http', '$filter', '$httpParamSerializer',
            function ($scope, $rootScope, $stateParams, $http, $filter, $httpParamSerializer) {

                var un;
                $scope.initialized = false;
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
                    $scope.initialized = false;

                    $http
                        .get("/rest/getSystemAdmins")
                        .then(function (response) {
                            $scope.admins = response.data.admins;
                            $scope.isAdmin = response.data.isAdmin;
                            $scope.initialized = true;
                        });
                }

                $scope.getUsers = function (val) {
                    return $http.get('/rest/userSearch', {
                        params: {t: val}
                    }).then(function (response) {
                        return response.data;
                    });
                };

                $scope.makeMeAnAdmin = function()
                {
                    return $http.post('/rest/makeMeAnAdmin')
                        .then(function (response) {
                            initAdmins();
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