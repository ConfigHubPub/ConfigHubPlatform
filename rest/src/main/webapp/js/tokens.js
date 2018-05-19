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
    .module('configHub.repository.tokens', [
        'monospaced.elastic',
        'ngAnimate',
        'puElasticInput',
        'chart.js'
    ])

    .controller('TokenController',
    ['$timeout', '$stateParams', '$scope', '$http', '$httpParamSerializer', 'secretService', '$rootScope', 'store', '$filter',
        function ($timeout, $stateParams, $scope, $http, $httpParamSerializer, secretService, $rootScope, store, $filter)
        {
            $scope.account = $stateParams.owner;
            $scope.repoName = $stateParams.name;
            $rootScope.selectedTab = 1;

            $scope.initialized = false;
            $scope.reverse = false;
            $scope.sortField = 'name';
            
            $scope.tokens = [[],[],[]];
            $scope.groups = [];
            $scope.tsFormat = tsFormat;
            $scope.accessControlEnabled = false;

            $scope.showAll = false;
            $scope.showAllToggle = function() {
                $scope.showAll = !$scope.showAll;
                $scope.fetchTokens();
            };

            var data,
                pst = store.get('pageSize'),
                token,
                tokenIndex = 0,
                fetchedTokens = [],
                filtered,
                sorted;

            $scope.fetchTokens = function()
            {
                $http({
                    method: 'GET',
                    url: '/rest/tokenFetchAll/' + $scope.account + "/" + $scope.repoName + "?all=" + $scope.showAll
                }).then(function(response) {

                    $scope.initialized = true;
                    data = response.data;
                    if (data.success)
                    {
                        $scope.isAdmin = data.isAdmin;
                        $scope.accessControlEnabled = data.accessControlEnabled;
                        $scope.teamMember = data.teamMember;
                        $scope.teams = data.teams;
                        $scope.groups = data.groups;
                        fetchedTokens = data.tokens;

                        organizeTokensForDisplay(fetchedTokens);
                    }
                    else
                    {
                        $scope.errorMessage = data.message;
                    }
                });
            };

            function organizeTokensForDisplay(data)
            {
                sorted = $filter('orderObjectBy')(data, 'name', false);

                tokenIndex = 0;
                $scope.tokens = [[],[],[]];
                if (data.length > 0)
                {
                    for (token in sorted)
                    {
                        $scope.tokens[tokenIndex++].push(sorted[token]);
                        if (tokenIndex >= 3) tokenIndex = 0;
                    }
                }
            }

            $scope.showToken = function(token)
            {
                token.show = !token.show;
            };


            $scope.searchTokens = function(searchQuery)
            {
                filtered = $filter('tokenFilter')(fetchedTokens, searchQuery);
                organizeTokensForDisplay(filtered);
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


            $scope.newToken = {};

            $scope.initNewToken = function()
            {
                $scope.newToken = {
                    edited: true,
                    isNew: true,
                    editable: true,
                    f: {
                        expires: null,
                        name: '',
                        active: true,
                        addSps: [],
                        removeSps: [],
                        managedBy: 'User'
                    },
                    dateTimeForm: false,
                    expires: null,
                    name: '',
                    active: true
                };
            };

            $scope.initNewToken();
            $scope.fetchTokens();

            $scope.newTokenEnabled = false;
            $scope.addToken = function() {
                $scope.newTokenEnabled = true;
            };

            $scope.cancelAddToken = function() {
                $scope.newTokenEnabled = false;
                $scope.errorMessage = '';
            };

        }
    ])

    .directive('token', function ()
    {
        return {
            restrict: 'A',
            templateUrl: 'repo/tokens/tokenForm.tpl.html',
            scope: true,
            controller: ['$scope', 'secretService', '$httpParamSerializer', '$http', 'toUtc', '$filter',
                function($scope, secretService, $httpParamSerializer, $http, toUtc, $filter)
                {
                    var spPass = {},
                        profile,
                        i, s, label, cipher,
                        needsAuth,
                        form,
                        date;

                    $scope.SGConfig = angular.copy(SGConfig);
                    delete $scope.SGConfig.maxItems;

                    $scope.SGConfig.plugins = ['remove_button'];
                    $scope.SGConfig.render = {
                        item: function(item, escape) {
                            return '<div>' + ('<span class="spnl">' + escape(item.name) + '</span>') + '</div>';
                        },
                        option: function(item, escape) {
                            label = item.name,
                                cipher = item.cipher ? item.cipher : null;

                            return '<div>' +
                                '<span class="spnl">' + escape(label) + '</span>' +
                                (cipher ? '<span class="spcl">| Encryption: ' + escape(cipher) + '</span>' : '<span class="spcl">| Encryption: disabled</span>') +
                                '</div>';
                        }
                    };


                    $scope.SGConfig.onInitialize = function(spsSelector) {
                        $scope.token.f.spsSelector = spsSelector;
                    };

                    $scope.SGConfig.onItemRemove = function(profile)
                    {
                        if (!$scope.token.f.spsSelector || $scope.token.f.processing) return;

                        needsAuth = $scope.token.sps ? $scope.token.sps.indexOf(profile) != -1 : true;

                        if (needsAuth)
                        {
                            $scope.token.f.processing = true;
                            secretService.authAndExec($scope, null, profile,
                                function()
                                {
                                    spPass[profile] = secretService.get(profile);
                                    $scope.token.f.removeSps.push(profile);

                                    i = $scope.token.f.addSps.indexOf(profile);
                                    if (-1 != i) $scope.token.f.addSps.splice(i, 1);

                                    $scope.token.f.processing = false;
                                },
                                function()
                                {
                                    $scope.token.f.spsSelector.addItem(profile, true);
                                    $scope.token.f.sps.push(profile);
                                    $scope.token.f.processing = false;
                                });
                        }
                        else
                        {
                            i = $scope.token.f.addSps.indexOf(profile);
                            if (-1 != i) $scope.token.f.addSps.splice(i, 1);

                            i = $scope.token.f.removeSps.indexOf(profile);
                            if (-1 != i) $scope.token.f.removeSps.splice(i, 1);

                        }

                    };

                    $scope.SGConfig.onItemAdd = function(profile)
                    {
                        if (!$scope.token.f.spsSelector || $scope.token.f.processing) return;

                        needsAuth = $scope.token.sps ? $scope.token.sps.indexOf(profile) == -1 : true;

                        if (needsAuth)
                        {
                            $scope.token.f.processing = true;
                            secretService.authAndExec($scope, null, profile,
                                function()
                                {
                                    spPass[profile] = secretService.get(profile);
                                    $scope.token.f.addSps.push(profile);

                                    i = $scope.token.f.removeSps.indexOf(profile);
                                    if (-1 != i) $scope.token.f.removeSps.splice(i, 1);

                                    $scope.token.f.processing = false;
                                },
                                function()
                                {
                                    $scope.token.f.spsSelector.removeItem(profile, true);
                                    $scope.token.f.processing = false;
                                });
                        }
                        else
                        {
                            i = $scope.token.f.addSps.indexOf(profile);
                            if (-1 != i) $scope.token.f.addSps.splice(i, 1);

                            i = $scope.token.f.removeSps.indexOf(profile);
                            if (-1 != i) $scope.token.f.removeSps.splice(i, 1);

                        }
                    };


s

                    $scope.toggleDateTimeForm = function() {
                        $scope.token.f.dateTimeForm = true;
                    };

                    $scope.clearDate = function() {
                        $scope.token.f.dateTimeForm = false;
                        $scope.token.f.expires = '';
                    };

                    $scope.getTokeDate = function()
                    {
                        if (null == $scope.token.expires)
                            return "Never";
                        else
                            return $filter('date')($scope.token.date, 'EEEE, MMMM d, y @ h:mm a');
                    };


                    $scope.setExpirationNever = function()
                    {
                        $scope.showCustomDate = false;
                        $scope.token.f.expires = null;
                    };

                    $scope.setExpirationDays = function(days)
                    {
                        $scope.showCustomDate = false;

                        date = new Date();
                        date.setDate(date.getDate() + days);
                        date.setMinutes(0);
                        date.setMinutes(0);
                        date.setSeconds(0);
                        date.setMilliseconds(0);
                        $scope.token.f.expires = date;
                    };

                    $scope.showCustomDate = false;
                    $scope.setCustomExpiration = function()
                    {
                        $scope.showCustomDate = true;
                    };

                    $scope.editToken = function() {

                        if ($scope.token.f) delete $scope.token.f;

                        $scope.token.f = angular.copy($scope.token);
                        $scope.token.f.addSps = [];
                        $scope.token.f.removeSps = [];

                        if ($scope.token.expires)
                            $scope.token.f.expires = new Date($scope.token.expires);

                        $scope.token.edited = true;
                    };

                    $scope.deleteToken = function()
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/deleteToken/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                tokenId: $scope.token.id
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                                $scope.fetchTokens();
                            else
                                $scope.errorMessage = response.data.message;
                        })
                    };


                    $scope.selectPushOverride = function(choice)
                    {
                        $scope.token.f.forceKeyPushEnabled = choice;
                    };

                    $scope.setAccessRulesTeam = function(choice)
                    {
                        $scope.token.f.rulesTeam = choice;
                    };


                    // Managed By
                    $scope.selectVisibleByAuthor = function()
                    {
                        $scope.token.f.managedBy = 'User';
                        $scope.token.f.user = $scope.user().username;
                        $scope.token.f.managingTeam = null;
                    };

                    $scope.selectVisibleByAdmins = function()
                    {
                        if (!$scope.isAdmin) return;
                        $scope.token.f.managedBy = 'Admins';
                        $scope.token.f.managingTeam = null;
                        $scope.token.f.user = null;
                    };

                    $scope.selectVisibleByEveryone = function()
                    {
                        if (!$scope.isAdmin) return;
                        $scope.token.f.managedBy = 'All';
                        $scope.token.f.managingTeam = null;
                        $scope.token.f.user = null;
                    };

                    $scope.selectVisibleByTeam = function(team)
                    {
                        if (!$scope.isAdmin && team != $scope.teamMember) return;

                        $scope.token.f.managedBy = 'Team';
                        $scope.token.f.managingTeam = team;
                        $scope.token.f.user = null;
                    };


                    $scope.save = function()
                    {
                        form = {
                            expires: $scope.token.f.expires ? toUtc.toMS($scope.token.f.expires) : null,
                            id: $scope.token.f.id ? $scope.token.f.id : null,
                            name: $scope.token.f.name,
                            active: $scope.token.f.active,
                            forceKeyPushEnabled: $scope.token.f.forceKeyPushEnabled,
                            addSps: $scope.token.f.addSps ? $scope.token.f.addSps.join(',') : '',
                            removeSps: $scope.token.f.removeSps ? $scope.token.f.removeSps.join(',') : '',
                            managedBy: $scope.token.f.managedBy,
                            managingTeam: $scope.token.f.managingTeam,
                            rulesTeam: $scope.token.f.rulesTeam,
                            newOwner: $scope.token.f.user
                        };

                        for (i in $scope.token.f.addSps)
                        {
                            profile = $scope.token.f.addSps[i];
                            secretService.authAndExec($scope, null, profile,
                                function() { form[profile] = secretService.get(profile); });
                        }

                        for (i in $scope.token.f.removeSps)
                        {
                            profile = $scope.token.f.removeSps[i];
                            secretService.authAndExec($scope, null, profile,
                                function() { form[profile] = secretService.get(profile); });
                        }

                        $http({
                            method: 'POST',
                            url: '/rest/saveOrUpdateToken/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer(form),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                $scope.token.edited = false;
                                $scope.cancelAddToken();

                                if ($scope.token.isNew)
                                    $scope.initNewToken();

                                $scope.fetchTokens();
                            }
                            else
                                $scope.errorMessage = response.data.message;
                        });
                    };

                    $scope.cancel = function()
                    {
                        $scope.token.edited = false;
                        $scope.errorMessage = '';
                    };

                }]
        }
    })

    ;