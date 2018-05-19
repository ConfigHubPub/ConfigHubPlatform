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
    .module('configHub.repository.profiles', [])

    .controller('SecureGroupsController',
        ['$scope', '$http', '$stateParams', '$filter', 'secretService', '$rootScope', 'store', '$state',
            function ($scope, $http, $stateParams, $filter, secretService, $rootScope, store, $state)
            {
                $scope.account = $stateParams.owner;
                $scope.repoName = $stateParams.name;

                $scope.initialized = false;
                $scope.message = '';
                $scope.profiles = [];

                $rootScope.selectedTab = 2;

                var pst = store.get('pageSize');

                $scope.newSecurityProfile = function() {
                    $state.go('repo.new-security-group',
                        {owner: $scope.account, name: $scope.repoName});
                };


                $http
                    .get("/rest/getSecurityProfiles/" + $scope.account + "/" + $scope.repoName)
                    .then(function (response)
                    {
                        $scope.initialized = true;
                        if (response.data.success) {
                            $scope.profiles = response.data.groups;

                        } else {
                            $scope.message = response.data.message;
                        }
                    });

                $scope.getProfile = function(profile) {
                    $state.go('repo.security-profile', {
                        owner: $scope.account,
                        name: $scope.repoName,
                        profile: profile.name
                    });
                };

                // --------------------------------
                // Pagination
                // --------------------------------
                $scope.reverse = false;
                $scope.currentPage = 1;
                $scope.pageSize = pst ? pst : 10;
                $scope.pageSizes = {
                    sizes: [{id: 10, name: '10'}, {id: 25, name: '25'}, {id: 50, name: '50'}],
                    selectedOption: {id: $scope.pageSize, name: $scope.pageSize}
                };
                $scope.pageSizeUpdate = function()
                {
                    store.set('pageSize', $scope.pageSizes.selectedOption.name );
                };
            }
        ])

    .controller('NewSecureGroupController', ['$scope', '$rootScope', '$http', '$httpParamSerializer', '$stateParams', 'secretService', '$state',
    function($scope, $rootScope, $http, $httpParamSerializer, $stateParams, secretService, $state) {

        $scope.account = $stateParams.owner;
        $scope.repoName = $stateParams.name;
        $rootScope.selectedTab = 2;

        $scope.CipherConfig = {
            create: false,
            valueField: 'cipher',
            labelField: 'title',
            searchField: ['title'],
            closeAfterSelect: false,
            openOnFocus: true,
            maxItems: 1
        };

        $scope.ciphers = ciphers;
        $scope.aProfile = {};
        $scope.aProfile.cipher = $scope.ciphers[0].cipher;

        $scope.saveSecurityProfile = function()
        {
            $http({
                method: 'POST',
                url: '/rest/createSecurityProfile/' + $scope.account + "/" + $scope.repoName,
                data: $httpParamSerializer({
                    name: $scope.aProfile.name,
                    password: $scope.aProfile.password,
                    password2: $scope.aProfile.password2,
                    cipher: $scope.aProfile.cipher ? $scope.aProfile.cipher : ''
                }),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).then(function (response)
            {
                if (response.data.success) {
                    secretService.cache($scope.aProfile.name, $scope.aProfile.password);
                    $state.go('repo.security-profiles',
                        {owner: $scope.account, name: $scope.repoName});
                }
                else
                    $scope.message = response.data.message;
            });
        };

        $scope.cancelNewSp = function()
        {
            $state.go('repo.security-profiles',
                {owner: $scope.account, name: $scope.repoName});
        };

    }])

    .controller('SecureGroupController',
        ['$scope', '$http', '$stateParams', '$filter', 'secretService', '$rootScope', '$state', '$httpParamSerializer', 'editorInit',
            function ($scope, $http, $stateParams, $filter, secretService, $rootScope, $state, $httpParamSerializer, editorInit)
            {
                $scope.account = $stateParams.owner;
                $scope.repoName = $stateParams.name;
                $scope.profileName = $stateParams.profile;

                $scope.tsFormat = tsFormat;
                $scope.initialized = false;
                $scope.message = '';
                $scope.profiles = [];
                $scope.aProfile = {};
                $scope.passive = true;

                $rootScope.selectedTab = 2;

                $scope.ciphers = ciphers;

                $scope.CipherConfig = {
                    create: false,
                    valueField: 'cipher',
                    labelField: 'title',
                    searchField: ['title'],
                    closeAfterSelect: false,
                    openOnFocus: true,
                    maxItems: 1
                };

                $scope.files = [];
                $scope.tokens = [];
                $scope.hideTokenEdit = true;
                // --------------------------------
                // Initialize and choose profile
                // --------------------------------

                $scope.isLive = function () { return true; };
                editorInit.setState('securityProfile');



                var form;

                $http({
                    method: 'POST',
                    url: '/rest/getSecurityGroup/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        profile: $scope.profileName
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success) {

                        if (!$scope.aProfile.cipher)
                            $scope.aProfile.cipher = ' ';

                        $scope.aProfile = response.data.profile;
                        $scope.isPrivAccount = response.data.privAcc;
                        $scope.aProfile.f = {
                            name: $scope.aProfile.name,
                            cipher: $scope.aProfile.cipher
                        };
                        $scope.files = response.data.files;
                        $scope.tokens = response.data.tokens;
                        $scope.count = $scope.aProfile.keys ? $scope.aProfile.keys.length : 0;

                        editorInit.initialize($scope);
                    }
                    else {
                        $scope.message = response.data.message;
                    }

                    $scope.initialized = true;
                });

                // --------------------------------
                // Edit profile
                // --------------------------------

                $scope.editProfile = function ()
                {
                    if ($scope.aProfile.edit) return;

                    $scope.aProfile.f = {
                        name: $scope.aProfile.name,
                        cipher: $scope.aProfile.cipher
                    };

                    $scope.aProfile.edit = true;
                };

                $scope.cancelEdit = function ()
                {
                    $state.go('repo.security-profiles',
                        {owner: $scope.account, name: $scope.repoName});
                };

                $scope.updateProfile = function ()
                {
                    if ($scope.aProfile.showChangePasswordForm && $scope.aProfile.f.currPassword) {
                        update($scope.aProfile.f.currPassword);
                    }
                    else {
                        secretService.authAndExec($scope, null, $scope.aProfile.f.name,
                            function () { update(secretService.get($scope.aProfile.f.name)); }
                        );
                    }
                };

                $scope.withOldPass = true;
                function update(password)
                {
                    form = {
                        profile: $scope.aProfile.f.name,
                        newName: $scope.aProfile.name,
                        oldPass: password,
                        cipher: $scope.aProfile.cipher ? $scope.aProfile.cipher : ''
                    };

                    $http({
                        method: 'POST',
                        url: '/rest/updateSecurityProfile/' + $scope.account + "/" + $scope.repoName,
                        data: $httpParamSerializer(form),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        if (response.data.success) {
                            $scope.cancelEdit();
                        }
                        else {
                            $scope.aProfile.message = response.data.message;
                        }
                    });
                }
                $scope.changePassword = function ()
                {
                    secretService.updateSPPassword($scope, $scope.aProfile.name, function ()
                    {
                        $scope.cancelEdit();
                    }).show();
                };


                // --------------------------------
                // Delete profile
                // --------------------------------

                $scope.deleteProfile = function ()
                {
                    secretService.getSKModal($scope, null, $scope.aProfile.name, function ()
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/deleteSecurityProfile/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                name: $scope.aProfile.name,
                                password: secretService.get($scope.aProfile.name)
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            console.log(response);

                            if (response.data.success) {
                                secretService.clear($scope.aProfile.name);
                                $state.go('repo.security-profiles',
                                    {owner: $scope.account, name: $scope.repoName});
                            }
                            else {
                                $scope.aProfile.message = response.data.message;
                            }
                        });
                    }).show();
                };

                // --------------------------------
                // Files
                // --------------------------------
                $scope.getFile = function(file) {
                    $state.go('repo.file', {
                        owner: $scope.account,
                        name: $scope.repoName,
                        id: file.id,
                        fullPath: file.fullPath,
                        sp: file.sp
                    });
                };

                // --------------------------------
                // Pagination
                // --------------------------------
                $scope.reverse = false;
                $scope.currentPage = 1;
                $scope.pageSizes = {
                    sizes: [{id: 10, name: '10'}, {id: 25, name: '25'}, {id: 50, name: '50'}],
                    selectedOption: {id: 10, name: '10'}
                };
                $scope.count = 0;
            }
        ])

    .filter('securityKeyFilter', function ()
    {
        return function (items, fields)
        {
            if (!fields || fields.length == 0)
                return items;

            var res = [],
                filtered = [],
                added = false,
                fi,
                field,
                it,
                item,
                i,
                re;

            for (fi = 0; fi < fields.length; fi++) {
                field = fields[fi].replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
                res.push(new RegExp(field, 'i'));
            }

            for (it = 0; it < items.length; it++) {
                item = items[it];
                added = false;

                for (i in res) {
                    re = res[i];
                    if (re.test(item.key) || re.test(item[1].readme) || re.test(item[1].spName)) {
                        filtered.push(item);
                        added = true;
                        break;
                    }
                }
            }

            return filtered;
        }
    })
;