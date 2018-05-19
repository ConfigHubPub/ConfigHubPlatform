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
    .module('configHub.repository.settings', [
        'puElasticInput'
    ])

    .controller('CleanupController',
        ['$rootScope', '$http', '$scope', '$stateParams', '$state', '$httpParamSerializer', 'auth', '$timeout', '$alert',
            function ($rootScope, $http, $scope, $stateParams, $state, $httpParamSerializer, auth, $timeout, $alert) {
                $scope.account = $stateParams.owner;
                $scope.repoName = $stateParams.name;
                $rootScope.selectedTab = 5;

                $scope.initialized = false;
                getUnusedKeys();

                function getUnusedKeys()
                {
                    $http
                        .get("/rest/cleanup/" + $scope.account + "/" + $scope.repoName + "/keys")
                        .then(function (response)
                        {
                            $scope.unusedKeys = response.data.keys;
                            $scope.initialized = true;
                        });
                }

                $scope.allKeys = false;
                $scope.allKeyToggle = function() {
                    $scope.allKeys = !$scope.allKeys;
                    angular.forEach($scope.unusedKeys, function(entry) { entry.selected = $scope.allKeys; } );
                };


                $scope.deleteSelectedKeys = function()
                {
                    var selectedKeys = [];
                    angular.forEach($scope.unusedKeys, function(entry) {
                        if (entry.selected)
                            selectedKeys.push(entry.key);
                    });

                    $http({
                        method: 'POST',
                        url: '/rest/deleteUnusedKeys/' + $stateParams.owner + "/" + $stateParams.name,
                        data: $httpParamSerializer({keys: selectedKeys.join(',')}),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        if (response.data.success) {
                            getUnusedKeys();
                            $scope.errorMessage = null;
                        }
                        else {
                            $scope.errorMessage = response.data.message;
                        }
                    });
                };
            }
            ])

    .controller('SettingsController',
        ['$rootScope', '$http', '$scope', '$stateParams', '$state', '$httpParamSerializer', 'auth', '$timeout', '$alert',
            function ($rootScope, $http, $scope, $stateParams, $state, $httpParamSerializer, auth, $timeout, $alert)
            {
                $scope.account = $stateParams.owner;
                $scope.repoName = $stateParams.name;
                $rootScope.selectedTab = 5;

                $scope.settings = [
                    {
                        name: 'Name & Description',
                        s: 'name'
                    },
                    {
                        name: 'Configuration',
                        s: 'features'
                    },
                    {
                        name: 'Context Scope',
                        s: 'context'
                    },
                    {
                        name: 'Cleanup',
                        s: 'cleanup'
                    },
                    {
                        name: 'Visibility',
                        s: 'visibility'
                    },
                    {
                        name: 'Transfer Ownership',
                        s: 'transfer'
                    },
                    {
                        name: 'Delete Repository',
                        s: 'delete'
                    }
                ];

                var i, form;

                $scope.selectedIndex = 0;
                $scope.selectSetting = function (index)
                {
                    $scope.settings[$scope.selectedIndex].selected = false;
                    $scope.settings[index].selected = true;
                    $scope.selectedIndex = index;

                    $state.go('repo.settings',
                        {owner: $scope.account, name: $scope.repoName, s: $scope.settings[index].s},
                        {notify: false});
                };

                $scope.repo = {};
                $scope.initialized = false;
                function initialize()
                {
                    $http
                        .get("/rest/repositoryInfo/" + $scope.account + "/" + $scope.repoName)
                        .then(function (response)
                        {
                            i = 0;
                            if ($stateParams.s)
                                i = indexOf($scope.settings, 's', $stateParams.s);

                            $scope.repo = response.data;

                            $scope.features = {
                                accessControlEnabled: $scope.repo.accessControlEnabled,
                                securityProfilesEnabled: $scope.repo.securityProfilesEnabled,
                                valueTypeEnabled: $scope.repo.valueTypeEnabled,
                                contextClustersEnabled: $scope.repo.contextClustersEnabled,
                                isPersonal: $scope.repo.isPersonal,
                                adminContextControlled: $scope.repo.adminContextControlled,
                                tokenlessAPIPull: $scope.repo.tokenlessAPIPull,
                                tokenlessAPIPush: $scope.repo.tokenlessAPIPush
                            };
                            $scope.isPrivate = $scope.repo.isPrivate;

                            $scope.initialized = true;

                            $scope.selectSetting(i == -1 ? 0 : i);
                        });
                }

                initialize();

                //---------------------------------------------------------------------------------------
                // Name and description
                //---------------------------------------------------------------------------------------
                $scope.nameMessage = '';

                $scope.updateNameAndDescription = function (name, desc)
                {
                    $http({
                        method: 'POST',
                        url: '/rest/updateRepository/' + $stateParams.owner + "/" + $stateParams.name,
                        data: $httpParamSerializer({name: name, description: desc}),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        if (response.data.success) {
                            $rootScope.repository = response.data.repository;
                            $scope.nameMessage = '';
                            $state.go('repo.settings', {
                                owner: response.data.repository.owner,
                                name: response.data.repository.name,
                                s: 'name'
                            });
                        }
                        else {
                            $scope.nameMessage = response.data.message;
                        }
                    });
                };

                //---------------------------------------------------------------------------------------
                // Features
                //---------------------------------------------------------------------------------------

                $scope.togglesDisabled = false;

                $scope.saveFeatures = function()
                {
                    auth.authAndExec($scope, function (password)
                    {
                        form = {
                            accessControl: $scope.features.accessControlEnabled,
                            securityProfiles: $scope.features.securityProfilesEnabled,
                            valueType: $scope.features.valueTypeEnabled,
                            contextClusters: $scope.features.contextClustersEnabled,
                            adminContextControlled: $scope.features.adminContextControlled,
                            tokenlessAPIPull: $scope.features.tokenlessAPIPull,
                            tokenlessAPIPush: $scope.features.tokenlessAPIPush,
                            password: password
                        };

                        $http({
                            method: 'POST',
                            url: '/rest/updateRepositoryFeatures/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer(form),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success) {
                                $rootScope.repository = response.data.repository;
                            }
                            else {
                                $scope.featureMessage = response.data.message;
                                $scope.cancelFeatures();
                                if (response.data.err)
                                {
                                    for (i in response.data.err)
                                    {
                                        switch (response.data.err[i].type)
                                        {
                                            case 'valueType':
                                                $scope.vdtMessage = response.data.err[i].message;
                                                $scope.keys = response.data.err[i].keys;
                                                break;

                                            case 'contextClusters':
                                                $scope.ccMessage = response.data.err[i].message;
                                                break;

                                            case 'adminContextControlled':
                                                $scope.ccMessage = response.data.err[i].message;
                                                break;

                                        }
                                    }
                                }
                            }
                        });
                    });
                };

                $scope.cancelFeatures = function()
                {
                    $scope.features = {
                        accessControlEnabled: $scope.repo.accessControlEnabled,
                        securityProfilesEnabled: $scope.repo.securityProfilesEnabled,
                        valueTypeEnabled: $scope.repo.valueTypeEnabled,
                        contextClustersEnabled: $scope.repo.contextClustersEnabled,
                        isPersonal: $scope.repo.isPersonal,
                        adminContextControlled: $scope.repo.adminContextControlled
                    };
                };


                //---------------------------------------------------------------------------------------
                // Transfer
                //---------------------------------------------------------------------------------------

                $scope.transferMessage = "";
                $scope.transferOwnership = function (toAccount)
                {
                    auth.authAndExec($scope, function (password)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/transferOwnership/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                password: password,
                                toAccount: toAccount
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success) {
                                if (response.data.hasOwnProperty('repository')) {
                                    $rootScope.repository = response.data.repository;
                                    $state.go('repo.settings', {
                                        owner: response.data.repository.owner,
                                        name: response.data.repository.name
                                    });
                                }
                                else {
                                    $rootScope.repository = {};
                                    $state.go('home');
                                }
                            }
                            else {
                                $scope.transferMessage = response.data.message;
                            }
                        });
                    });
                };

                //---------------------------------------------------------------------------------------
                // Visibility
                //---------------------------------------------------------------------------------------

                $scope.visibilityMessage = "";
                $scope.togglePrivacy = function(isPrivate)
                {
                    auth.authAndExec($scope, function (password)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/togglePrivacy/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                isPrivate: isPrivate
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success) {
                                $rootScope.repository = response.data.repository;
                            }
                            else {
                                $scope.visibilityMessage = response.data.message;
                            }
                        });
                    });
                };

                //---------------------------------------------------------------------------------------
                // Delete
                //---------------------------------------------------------------------------------------

                $scope.deleteRepository = function()
                {
                    $scope.deleteError = '';

                    auth.authAndExec($scope, function(password)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/deleteRepository/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                password: password
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                                $state.go('dashboard');
                            else
                                $scope.deleteError = response.data.message;

                        });
                    });
                };
            }])

    .controller('SetupContextController',
        ['$rootScope', '$http', '$scope', '$stateParams', '$state', '$httpParamSerializer', 'auth', '$timeout', '$alert',
            function ($rootScope, $http, $scope, $stateParams, $state, $httpParamSerializer, auth, $timeout, $alert)
            {
                //---------------------------------------------------------------------------------------
                // Context
                //---------------------------------------------------------------------------------------

                var ordered = [],
                    s,
                    tk;

                $scope.getContextSize = function ()
                {
                    return Object.size($scope.repo.labels);
                };

                $scope.updateContextLabels = function ()
                {
                    ordered = [];
                    for (s in depthScores) {
                        if ($scope.repo.labels[depthScores[s]])
                            ordered.push($scope.repo.labels[depthScores[s]]);
                    }

                    $http({
                        method: 'POST',
                        url: '/rest/updateContextLabels/' + $stateParams.owner + "/" + $stateParams.name,
                        data: $httpParamSerializer({
                            labels: ordered.join()
                        }),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response)
                    {
                        if (!response.data.success) {
                            $scope.errorMessage = response.data.message;
                        }
                        else {
                            $scope.repo = response.data.repo;

                            $alert({
                                title: 'Context labels updated.',
                                container: '#labelSuccess',
                                type: 'success',
                                duration: 5,
                                show: true
                            });
                        }
                    });
                };

                $scope.scopeLimit = scopeLimit;
                $scope.getContextExpansionMap = function()
                {
                    if ($scope.repo.scopeSize == scopeLimit)
                        return [];

                    tk = [];
                    tk.push(null);

                    for (s in depthScores) {
                        if ($scope.repo.labels[depthScores[s]]) {
                            tk.push($scope.repo.labels[depthScores[s]]);
                            tk.push(null);
                        }
                    }

                    return tk;
                };

                $scope.insertLevel = -1;
                $scope.insertContext = function (i)
                {
                    $scope.insertLevel = i;
                };

                $scope.cancelCtxExpand = function () { $scope.insertLevel = -1; };
                $scope.inserting = false;

                $scope.expandContext = function (newContextLabel)
                {
                    $scope.inserting = true;

                    auth.authAndExec($scope, function (password)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/addContextLevel/' + $stateParams.owner + "/" + $stateParams.name,
                            data: $httpParamSerializer({
                                label: newContextLabel,
                                index: $scope.insertLevel,
                                password: password
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (!response.data.success) {
                                $scope.errorMessage = response.data.message;
                            }
                            else {
                                $scope.repo = response.data.repo;
                                $scope.cancelCtxExpand();

                                $alert({
                                    title: 'Context scope updated.',
                                    container: '#contextSuccess',
                                    type: 'success',
                                    duration: 5,
                                    show: true
                                });
                            }

                            $scope.inserting = false;
                        });
                    });
                };

                $scope.removing = [];
                $scope.shrinkMessage = '';
                $scope.removeContextRank = function (p)
                {
                    $scope.removing[p] = true;

                    auth.authAndExec($scope, function (password)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/removeContextLevel/' + $stateParams.owner + "/" + $stateParams.name,
                            data: $httpParamSerializer({
                                rank: p,
                                password: password
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (!response.data.success) {
                                $scope.shrinkMessage = response.data.message;
                            }
                            else {
                                $scope.shrinkMessage = '';
                                $scope.repo = response.data.repo;

                                $alert({
                                    title: 'Context scope updated.',
                                    container: '#contextSuccess',
                                    type: 'success',
                                    duration: 5,
                                    show: true
                                });
                            }

                            $scope.removing[p] = false;
                        });
                    });
                };

            }])

;