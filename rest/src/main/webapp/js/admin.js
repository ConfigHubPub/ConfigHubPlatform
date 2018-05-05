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

                            if (response.data.conf && response.data.conf.length > 0)
                            {
                                arr = response.data.conf;
                                for (i in arr)
                                {
                                    if (arr[i].key == 'ldap')
                                    {
                                        $scope.ldapForm = arr[i].value;
                                        break;
                                    }
                                }
                            }

                            if (!$scope.ldapForm)
                            {
                                $scope.ldapForm = {
                                    "system_username": "cn=read-only-admin,dc=example,dc=com",
                                    "system_password": "password",
                                    "ldap_uri": "ldap://ldap.forumsys.com:389",
                                    "use_start_tls": true,
                                    "trust_all_certificates": true,
                                    "active_directory": false,
                                    "search_base": "dc=example,dc=com",
                                    "search_pattern": "(&(objectClass=*)(uid={0}))",
                                    "principal": "riemann",
                                    "password": "password",
                                    "test_connect_only": false,
                                    "group_search_base": "",
                                    "group_id_attribute": "",
                                    "group_search_pattern": ""
                                };

                            }
                        });
                }

                $scope.enableLdapToggle = false;
                $scope.enableLdap = function() {

                };

                $scope.systemTestCompleted = false;
                $scope.testResponse;

                $scope.testLdap = function (connectionOnly) {

                    $scope.systemTestCompleted = false;

                    form = angular.copy($scope.ldapForm);
                    form.test_connect_only = connectionOnly;

                    $http({
                        method: 'POST',
                        url: '/rest/getLDAPConfig/testLdap',
                        data: $httpParamSerializer(form),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
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
                        data: $httpParamSerializer(form),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        console.log(response);
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