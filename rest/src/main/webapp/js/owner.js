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
    .module('configHub.account', [
    ])

    .controller('AccountController',
    ['$http', '$scope', '$stateParams', '$httpParamSerializer', '$alert', 'auth', '$state', '$rootScope',
        function ($http, $scope, $stateParams, $httpParamSerializer, $alert, auth, $state, $rootScope)
        {
            $scope.account = {};
            $scope.initialized = false;
            $scope.error = '';
            $scope.isPersonal = false;

            var i = 0;

            $http
                .get('/rest/getAccount/' + $stateParams.accountName)
                .then(function(response)
                {
                    if (response.data.success)
                    {
                        $scope.account = response.data;
                        $scope.initialized = true;

                        $scope.isPersonal = $scope.account.t === 'u';


                        for (i = 0; i < $scope.account.repos.length; i++)
                        {
                            switch ($scope.account.repos[i].ut) {
                                case 'owner': $scope.account.repos[i].ut = $rootScope.type.owner; break;
                                case 'admin': $scope.account.repos[i].ut = $rootScope.type.admin; break;
                                case 'member': $scope.account.repos[i].ut = $rootScope.type.member; break;
                                case 'nonMember': $scope.account.repos[i].ut = $rootScope.type.nonMember; break;
                            }
                            if ($scope.account.repos[i].demo && $scope.account.repos[i].ut < $rootScope.type.member)
                                $scope.account.repos[i].ut = $rootScope.type.demo;
                        }
                    }
                    else
                    {
                        $scope.error = response.data.message;
                    }
                });

            $scope.getUsers = function(val) {
                return $http.get('/rest/userSearch', {
                    params: { t: val }
                }).then(function(response){
                    return response.data;
                });
            };


            $scope.countries = countries;
            $scope.countryConfig = countryConfig;

            // ------------------------------------------------------------------------------
            // Public profile
            // ------------------------------------------------------------------------------
            $scope.profileMessageSuccess = false;

            $scope.updatePublicProfile = function()
            {
                $http({
                    method: 'POST',
                    url: '/rest/updatePublicProfile',
                    data: $httpParamSerializer({
                        account: $scope.account.un,
                        name: $scope.account.name,
                        company: $scope.account.company,
                        website: $scope.account.website,
                        city: $scope.account.city,
                        country: $scope.account.country
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        $scope.profileMessage = '';
                        $scope.profileMessageSuccess = true;

                        $alert({
                            title: 'Profile saved.',
                            content: '',
                            container: '#profileMessages',
                            type: 'success',
                            duration: 5,
                            show: true
                        });

                    }
                    else
                    {
                        $scope.profileMessage = response.data.message;
                        $scope.profileMessageSuccess = false;
                    }
                });
            };


            // ------------------------------------------------------------------------------
            // Account settings
            // ------------------------------------------------------------------------------
            $scope.nameMessage = '';
            $scope.changeUsername = function(newName)
            {
                auth.authAndExec($scope, function(password) {
                    $http({
                        method: 'POST',
                        url: '/rest/changeUsername',
                        data: $httpParamSerializer({
                            account: $scope.account.un,
                            newName: newName,
                            password: password
                        }),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        if (response.data.success)
                        {
                            $scope.account.un = newName;
                            if (response.data.token)
                            {
                                $scope.processLoginToken(response.token);
                                $state.go('owner',
                                    {accountName: $scope.account.un, s: 'unp'},
                                    {notify: false});
                                $scope.nameMessage = '';
                                $alert({
                                    title: 'Change saved.',
                                    container: '#nameSuccess',
                                    type: 'success',
                                    duration: 5,
                                    show: true
                                });
                            }
                        }
                        else
                        {
                            $scope.nameMessage = response.data.message;
                        }
                    });
                });
            };
        }])

    .directive('organizationAccount', function() {
        return {
            restrict: "A",
            templateUrl: '/public/orgAccount.html',
            scope: true,
            controller: ['$http', '$scope', '$stateParams', '$state', '$httpParamSerializer', 'auth', '$alert',
                function($http, $scope, $stateParams, $state, $httpParamSerializer, auth, $alert)
                {
                    $scope.selectedTabIndex = 0;
                    $scope.s = $stateParams.s;
                    if ($scope.s)
                    {
                        switch ($scope.s) {
                            case 'repo': $scope.selectedTabIndex = 0; break;
                            case 'mgr':  $scope.selectedTabIndex = 1; break;
                            case 'opp':  $scope.selectedTabIndex = 2; break;
                            case 'oac':  $scope.selectedTabIndex = 3; break;
                            default:     $scope.selectedTabIndex = 0;
                        }
                    }

                    function getL(s) {
                        switch (s) {
                            case 0: return 'repo';
                            case 1: return 'mgr';
                            case 2: return 'opp';
                            case 3: return 'oac';
                            default: return '';
                        }
                    }

                    $scope.upTb = function(s) {
                        $state.go('owner',
                            {accountName: $scope.account.un, s: getL(s)},
                            {notify: false});
                    };

                    $scope.showSettings = $scope.account.own && $scope.account.own === 'own';
                    if ($scope.showSettings)
                    {
                        $scope.accUserName = $scope.account.un;
                        $scope.email = $scope.account.email;
                    }

                    //-------------------------------------------------------------------------------------
                    // Management
                    //-------------------------------------------------------------------------------------

                    var un,
                        list,
                        i;

                    $scope.selected = '';
                    $scope.ownerError = '';
                    $scope.addAdminOrOwner = function(f, role)
                    {
                        $scope.ownerError = '';

                        un = f.un ? f.un : f;
                        $http({
                            method: 'POST',
                            url: '/rest/addAdminOrOwner/' + $scope.account.un,
                            data: $httpParamSerializer({un: un, role: role}),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                if (role === 'adm')
                                    $scope.account.admins.push(response.data.no);
                                else
                                    $scope.account.owners.push(response.data.no);
                                $scope.selected = '';
                            }
                            else
                            {
                                $scope.ownerError = response.data.message;
                            }
                        });
                    };

                    $scope.removeAdminOrOwner = function(un, role)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/removeAdminOrOwner/' + $scope.account.un,
                            data: $httpParamSerializer({un: un}),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                if (role === 'adm')
                                    list = $scope.account.admins;
                                else
                                    list = $scope.account.owners;

                                i = indexOf(list, 'un', un);
                                if (i != -1)
                                    list.splice(i, 1);

                                $scope.selected = '';
                            }
                            else
                            {
                                $scope.ownerError = response.data.message;
                            }

                        });
                    };


                    $scope.deleteOrganization = function()
                    {
                        auth.authAndExec($scope, function(password)
                        {
                            $http({
                                method: 'POST',
                                url: '/rest/deleteOrganization/' + $scope.account.un,
                                data: $httpParamSerializer({
                                    password: password
                                }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function (response)
                            {
                                console.log(response);
                                if (response.data.success) {

                                }
                                else {
                                    $scope.orgDeleteError = response.data.message;
                                }

                            });
                        });
                    };
                }
            ]
        }
    })

    .directive('personalAccount', function() {
        return {
            restrict: "A",
            templateUrl: '/public/userAccount.html',
            scope: true,
            controller: ['$http', '$scope', '$stateParams', '$state', '$httpParamSerializer', 'auth', '$alert',
                function($http, $scope, $stateParams, $state, $httpParamSerializer, auth, $alert)
                {
                    $scope.selectedTabIndex = 0;
                    $scope.s = $stateParams.s;
                    if ($scope.s)
                    {
                        switch ($scope.s) {
                            case 'repo': $scope.selectedTabIndex = 0; break;
                            case 'org':  $scope.selectedTabIndex = 1; break;
                            case 'upp':  $scope.selectedTabIndex = 2; break;
                            case 'uac':  $scope.selectedTabIndex = 3; break;
                            default:     $scope.selectedTabIndex = 0;
                        }
                    }

                    function getL(s) {
                        switch (s) {
                            case 0: return 'repo';
                            case 1: return 'org';
                            case 2: return 'upp';
                            case 3: return 'uac';
                            default: return '';
                        }
                    }

                    $scope.upTb = function(s) {
                        $state.go('owner',
                            {accountName: $scope.account.un, s: getL(s)},
                            {notify: false});
                    };

                    $scope.showSettings = $scope.account.own;
                    if ($scope.showSettings)
                    {
                        $scope.accUserName = $scope.account.un;
                        $scope.email = $scope.account.email;
                    }



                    // ------------------------------------------------------------------------------
                    // Account settings
                    // ------------------------------------------------------------------------------


                    $scope.passMessage = '';
                    $scope.oldPass = $scope.newPass = $scope.newPass2 = '';
                    $scope.changePassword = function(oldPass, newPass, newPass2)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/changePassword',
                            data: $httpParamSerializer({
                                password: oldPass,
                                newPassword1: newPass,
                                newPassword2: newPass2
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                $alert({
                                    title: 'Password updated.',
                                    container: '#passSuccess',
                                    type: 'success',
                                    duration: 5,
                                    show: true
                                });

                                $scope.passMessage = '';
                                $scope.oldPass, $scope.newPass, $scope.newPass2 = '';
                            }
                            else
                            {
                                $scope.passMessage = response.data.message;
                            }

                        });
                    };


                    // ------------------------------------------------------------------------------
                    // Email
                    // ------------------------------------------------------------------------------

                    $scope.changeEmail = function(email)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/changeEmail',
                            data: $httpParamSerializer({
                                email: email
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                $scope.processLoginToken(response.data.token);
                                $state.go('owner',
                                    {accountName: $scope.account.un, s: 'em'},
                                    {notify: false});

                                $scope.emailMessage = '';
                                $alert({
                                    title: 'Email updated.',
                                    container: '#emailSuccess',
                                    type: 'success',
                                    duration: 5,
                                    show: true
                                });
                            }
                            else
                            {
                                $scope.emailMessage = response.data.message;
                            }
                        });
                    };

                    $scope.emailRepo = function()
                    {
                        editEmailNotification("repo", $scope.account.repoSub);
                    };

                    $scope.emailBlog = function()
                    {
                        editEmailNotification("blog", $scope.account.blogSub);
                    };

                    function editEmailNotification(what, isTrue)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/updateEmailPrefField',
                            data: $httpParamSerializer({
                                field: what,
                                val: isTrue
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        });
                    }


                    // ------------------------------------------------------------------------------
                    // Repositories
                    // ------------------------------------------------------------------------------

                    $scope.reposMessage = '';
                    $scope.leaveRepository = function(repo)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/leaveRepository',
                            data: $httpParamSerializer({
                                account: repo.owner,
                                repositoryName: repo.name
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                if (response.data.removed)
                                {
                                    var i = 0;
                                    for (; i < $scope.account.repos.length; i++)
                                    {
                                        if ($scope.account.repos[i].name === repo.name &&
                                            $scope.account.repos[i].owner === repo.owner)
                                        {
                                            $scope.account.repos.splice(i, 1);
                                            break;
                                        }
                                    }
                                }
                                $scope.reposMessage = '';
                            }
                            else
                            {
                                $scope.reposMessage = response.data.message;
                            }
                        });
                    }
                }
            ]
        }
    })
;
