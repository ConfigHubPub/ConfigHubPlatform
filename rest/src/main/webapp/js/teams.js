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
    .module('configHub.repository.teams', [
        'ui.sortable'
    ])

    .controller('TeamsController',
    ['$scope', '$stateParams', '$http', 'contextService', '$filter', '$modal', 'store', '$state', '$httpParamSerializer', '$rootScope',
        function ($scope, $stateParams, $http, contextService, $filter, $modal, store, $state, $httpParamSerializer, $rootScope)
        {
            $scope.initialized = false;
            $scope.teams = [];
            $scope.aTeam = {};
            $scope.accessControlEnabled = false;

            $rootScope.selectedTab = 3;
            $scope.account = $stateParams.owner;
            $scope.repoName = $stateParams.name;

            // --------------------------------
            // Initialize and choose team
            // --------------------------------

            $scope.repoContext = { loaded: false };
            contextService.contextElements($scope.date, null, $scope.account, $scope.repoName).then(function(ctx) {
                $scope.repoContext = ctx;
                $scope.repoContext.loaded = true;
            });

            var orderBy = $filter('orderBy'),
                i,
                un,
                lastTeam,
                original,
                form,
                order = [];

            $scope.TeamSelectConfig = {
                create: false,
                valueField: 'name',
                labelField: 'name',
                searchField: ['name'],
                closeAfterSelect: false,
                openOnFocus: true,
                maxItems: 1
            };

            $scope.teamChanged = function()
            {
                $scope.findAndSelectTeam($scope.selectedTeam);
            };

            $http
                .get("/rest/getTeams/" + $scope.account + "/" + $scope.repoName)
                .then(function (response)
                {
                    // orderBy = $filter('orderBy');
                    if (response.data)
                    {
                        $scope.accessControlEnabled = response.data.accessControlEnabled;
                        $scope.teams = orderBy(response.data.teams, 'name');

                        i = -1;
                        if ($stateParams.team)
                            i = indexOf($scope.teams, 'name', $stateParams.team);

                        if (i>-1)
                            $scope.selectTeam($scope.teams[i]);
                        else
                            $scope.selectTeam($scope.teams[0]);
                    }

                    $scope.initialized = true;
                });

            $scope.findAndSelectTeam = function(teamName) {

                for (i = 0; i < $scope.teams.length; i++) {
                    if ($scope.teams[i].name === teamName) {
                        $scope.selectTeam($scope.teams[i]);
                        break;
                    }
                }
            };

            $scope.selectTeam = function(team)
            {
                $scope.allMembersToggle = false;

                if (!team) team = { newTeam: true };
                if ($scope.aTeam.selected && $scope.aTeam.name === team.name) return;

                if ($scope.aTeam)
                {
                    if ($scope.aTeam.d)
                        $scope.aTeam.d.sortingEnabled = false;
                    $scope.aTeam.selected = false;
                }

                $scope.aTeam = team;
                $scope.aTeam.selected = true;

                $scope.selected = null;
                $scope.selectedMember = {};
                $scope.memberMessage = '';

                if ($scope.aTeam.newTeam)
                    return;

                $http
                    .get('/rest/teamInfo/' + $scope.account + '/' + $scope.repoName + '/' + team.name)
                    .then(function(response)
                    {
                        if (response.data.success)
                        {
                            $scope.aTeam.d = response.data.team;

                            if ($scope.aTeam.d.accessRule)
                            {
                                $scope.aTeam.d.originalOrder = $scope.aTeam.d.accessRules.map(function(i) { return i.id; }).join(',');
                                $scope.aTeam.d.beforeSortItems = $scope.aTeam.d.accessRules.slice();
                                $scope.aTeam.d.reordered = false;
                                $scope.aTeam.d.sortingEnabled = false;
                            }

                            $state.go('repo.teams',
                                {owner: $scope.account, name: $scope.repoName, team: team.name},
                                {notify: false});
                        }
                        else
                        {
                            $scope.message = response.data.message;
                        }

                        $scope.initialized = true;
                    });
            };

            $scope.deleteTeamNow = function() {
                $http({
                    method: 'POST',
                    url: '/rest/deleteTeam/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({ team: $scope.aTeam.name }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        $scope.teams = response.data.teams;
                        // orderBy = $filter('orderBy');
                        $scope.teams = orderBy(response.data.teams, 'name');
                        $scope.selectTeam($scope.teams[0]);
                    }
                    else
                        $scope.aTeam.teamMessage = response.data.message;
                });
            };

            // --------------------------------
            // Users
            // --------------------------------
            $scope.allMembersToggle = false;
            $scope.allMembers = [];

            $scope.showAllMembers = function()
            {
                $http
                    .get("/rest/getAllMembers/" + $scope.account + "/" + $scope.repoName)
                    .then(function(response)
                    {
                        $scope.allMembers = response.data.members;
                        $scope.count = $scope.allMembers.length;
                        $scope.allMembersToggle = true;

                        if ($scope.aTeam)
                            $scope.aTeam.selected = false;
                    });
            };

            $scope.getUsers = function(val) {
                return $http.get('/rest/userSearch', {
                    params: { t: val }
                }).then(function(response){
                    return response.data;
                });
            };

            $scope.selected = '';
            $scope.selectedMember = {};
            $scope.selectMember = function(i, m, l)
            {
                $scope.selectedMember = m;
                $scope.memberMessage = '';
            };

            $scope.addMemberToTeam = function(f)
            {
                $scope.memberMessage = '';
                un = f.un ? f.un : f;
                $http({
                    method: 'POST',
                    url: '/rest/addMember/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({ un: un }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                    {
                        if (response.data.multiple)
                        {
                            $scope.selectedMember.memberOf = response.data.team;
                            $scope.selectedMember.un = un;
                            $scope.memberMessage = '';
                        }
                        else
                            $scope.memberMessage = response.data.message;
                    }
                    else
                    {
                        $scope.aTeam.d.members = response.data.members;
                        $scope.selected = null;
                        $scope.selectedMember = {};
                    }
                });
            };



            $scope.removeMember = function(member) {
                $http({
                    method: 'POST',
                    url: '/rest/removeMember/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer(member),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.memberMessage = response.data.message;
                    else
                        $scope.aTeam.d.members = response.data.members;
                });
            };

            $scope.moveToTeam = function(username, teamName) {
                $http({
                    method: 'POST',
                    url: '/rest/moveMemberToTeam/' +
                    $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        un: username,
                        all: $scope.allMembersToggle,
                        toTeam: teamName,
                        fromTeam: $scope.aTeam.name
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.memberMessage = response.data.message;
                    else
                    {
                        if ($scope.allMembersToggle)
                            $scope.allMembers = response.data.members;
                        else
                        {
                            $scope.selected = null;
                            $scope.selectedMember = {};
                            $scope.aTeam.d.members = response.data.members;
                        }
                    }
                });
            };

            // --------------------------------
            // Create / edit team
            // --------------------------------

            $scope.newTeam = function() {

                if ($scope.aTeam)
                    lastTeam = $scope.aTeam;

                $scope.selectTeam(
                    {
                        newName: '',
                        newTeam: true,
                        name: '',
                        members: [],
                        accessRules: []
                    })
            };

            $scope.cancel = function() {
                if (lastTeam)
                {
                    $scope.selectTeam(lastTeam);
                }
            };

            $scope.team = {};
            $scope.createTeam = function()
            {
                $http({
                    method: 'POST',
                    url: '/rest/createTeam/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        name: $scope.aTeam.newName
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        $scope.teams = response.data.teams;
                        for (i in $scope.teams)
                        {
                            if ($scope.teams[i].name === $scope.aTeam.newName)
                            {
                                $scope.selectTeam($scope.teams[i]);
                                break;
                            }
                        }
                    }
                    else
                        $scope.aTeam.teamMessage = response.data.message;
                });
            };

            $scope.renameTeam = function()
            {
                if (!$scope.aTeam.newName)
                    $scope.aTeam.newName = $scope.aTeam.name;

                $http({
                    method: 'POST',
                    url: '/rest/renameTeam/' + $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({newName: $scope.aTeam.newName}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        $scope.aTeam.name = $scope.aTeam.newName;
                        delete $scope.aTeam.newName;
                    }

                    $scope.aTeam.teamMessage = response.data.message;
                });
            };

            $scope.cancelRename = function() {
                if ($scope.aTeam.newTeam)
                    ;
                else
                    delete $scope.aTeam.newName;
            };

            // --------------------------------
            // Rules
            // --------------------------------
            $scope.genRuleProcessTypes = genRuleProcessTypes;
            $scope.editGenRuleStop = function() {
                $scope.aTeam.d.orgStopOnFirstMatch = $scope.aTeam.d.stopOnFirstMatch;
                $scope.aTeam.d.isEditingGenRuleStop = true;
            };
            $scope.cancelGenRuleStop = function() {
                $scope.aTeam.d.stopOnFirstMatch = $scope.aTeam.d.orgStopOnFirstMatch;
                $scope.aTeam.d.isEditingGenRuleStop = false;
            };
            $scope.updateGenRuleStop = function() {
                $http({
                    method: 'POST',
                    url: '/rest/updateStopOnFirstMatchedRule/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({stopOnFirstMatch: $scope.aTeam.d.stopOnFirstMatch}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                    {
                        $scope.aTeam.d.stopOnFirstMatch = response.data.stopOnFirstMatch;
                        $scope.aTeam.d.isEditingGenRuleStop = false;
                    }
                });
            };

            $scope.editGenUnprocessedMatch = function() {
                $scope.aTeam.d.orgUnmatchedEditable = $scope.aTeam.d.unmatchedEditable;
                $scope.aTeam.d.isEditingUnprocessedMatch = true;
            };
            $scope.cancelGenUnprocessedMatch = function() {
                $scope.aTeam.d.unmatchedEditable = $scope.aTeam.d.orgUnmatchedEditable;
                $scope.aTeam.d.isEditingUnprocessedMatch = false;
            };
            $scope.updateGenUnprocessedMatch = function() {
                $http({
                    method: 'POST',
                    url: '/rest/updateUnmatchedEditableRule/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({unmatchedEditable: $scope.aTeam.d.unmatchedEditable}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                    {
                        $scope.aTeam.d.unmatchedEditable = response.data.unmatchedEditable;
                        $scope.aTeam.d.isEditingUnprocessedMatch = false;
                    }
                });
            };

            $scope.types = ['Key', 'Value'];
            $scope.typeLabel = {
                'Key': 'Key',
                'Value': 'Context'
            };

            $scope.keyMatches = {
                Is: "Is",
                Contains: "Contains",
                StartsWith: "Starts with",
                EndsWith: "Ends with"
            };
            $scope.ctxMatches = {
                ContainsAny: "Contains any",
                ContainsAll: "Contains all",
                DoesNotContain: "Does not contain",
                Resolves: "Resolves",
                DoesNotResolve: "Does not resolve"
            };
            $scope.accessType = {
                ro: "Read-Only",
                rw: "Read/Write"
            };

            $scope.rule = {
                type: '',
                match: '',
                key: '',
                context: {},
                access: 'ro'
            };

            $scope.switchRuleType = function(type)
            {
                $scope.rule.type = type;

                switch (type) {
                    case $scope.types[0]:
                        $scope.rule.match = 'Contains';
                        break;
                    case $scope.types[1]:
                        $scope.rule.match = 'Resolves';
                        break;
                }
            };

            $scope.clearRuleForm = function() {
                $scope.newRuleFormVisible = false;
                $scope.rule = {
                    type: '',
                    match: '',
                    key: '',
                    context: {},
                    access: 'ro'
                };
            };

            $scope.editRule = function(rule)
            {
                original = {};
                angular.copy(rule, original);
                rule.original = original;
                rule.isEdited = true;
            };

            $scope.cancelRuleEdit = function(rule)
            {
                original = rule.original;
                angular.copy(original, rule);
                rule.isEdited = false;
            };

            $scope.newRuleFormVisible = false;
            $scope.enableNewRuleForm = function() { $scope.newRuleFormVisible = true; };

            $scope.contextSelectConfig = {
                create: false,
                plugins: ['remove_button'],
                valueField: 'name',
                labelField: 'name',
                searchField: ['name'],
                delimiter: ',',
                placeholder: '*',
                closeAfterSelect: false,
                openOnFocus: true,
                sortField: 'name'
            };

            $scope.createRule = function() {

                form = {};
                angular.copy($scope.rule, form);
                if (form.context)
                    form.context = contextParam(form.context);

                $http({
                    method: 'POST',
                    url: '/rest/createAccessRule/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer(form),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                    {
                        $scope.clearRuleForm();
                        $scope.aTeam.d.accessRules = response.data.accessRules;
                    }
                });
            };

            $scope.deleteRule = function(rule) {

                $http({
                    method: 'POST',
                    url: '/rest/deleteAccessRule/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({id: rule.id}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                    {
                        $scope.aTeam.d.accessRules = response.data.accessRules;
                    }
                });
            };

            $scope.updateRule = function(rule) {

                form = {};
                angular.copy(rule, form);
                if (form.context)
                    form.context = contextParam(form.context);

                $http({
                    method: 'POST',
                    url: '/rest/updateAccessRule/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer(form),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                        $scope.aTeam.d.accessRules = response.data.accessRules;
                });
            };

            // --------------------------------
            // Rules sorting
            // --------------------------------

            $scope.cancelSorting = function() {
                $scope.aTeam.d.sortingEnabled = false;
                $scope.aTeam.d.accessRules = $scope.aTeam.d.beforeSortItems;
            };

            // Reordering rules
            $scope.sortableOptions = {
                start: function() {
                    $scope.aTeam.d.beforeSortItems = $scope.aTeam.d.accessRules.slice();
                },
                update: function(e, ui) {},
                stop: function(e, ui) {
                    order = $scope.aTeam.d.accessRules.map(function(i) { return i.id; }).join(',');
                    $scope.aTeam.d.reordered = !angular.equals(order, $scope.aTeam.d.originalOrder)
                },
                items: "li:not(.not-sortable)"
            };

            $scope.saveOrder = function()
            {
                $http({
                    method: 'POST',
                    url: '/rest/reorderAccessRules/' +
                    $scope.account + "/" + $scope.repoName + "/" + $scope.aTeam.name,
                    data: $httpParamSerializer({order: order}),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (!response.data.success)
                        $scope.rulesMessage = response.data.message;
                    else
                    {
                        $scope.aTeam.d.accessRules = response.data.accessRules;
                        $scope.aTeam.d.sortingEnabled = false;
                        $scope.aTeam.d.originalOrder = $scope.aTeam.d.accessRules.map(function(i) { return i.id; }).join(',');
                        $scope.aTeam.d.beforeSortItems = $scope.aTeam.d.accessRules.slice();
                    }

                });
            };

            // --------------------------------
            // Pagination
            // --------------------------------
            $scope.currentPage = 1;
            if (store.get('userPageSize'))
                $scope.pageSize = store.get('userPageSize');
            else
                $scope.pageSize = 10;

            $scope.count = 0;
            $scope.pageSizeUpdate = function ()
            {
                store.set('userPageSize', $scope.pageSize);
            };
            $scope.showPagination = function ()
            {
                return count > 10;
            };
        }
    ])



    .filter('memberFilter', function()
    {
        return function(items, fields)
        {
            if (!fields || fields.length == 0)
                return items;

            var filtered = [],
                res = [],
                field,
                i,
                re;

            angular.forEach(fields, function (unescaped)
            {
                field = unescaped.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
                res.push(new RegExp(field, 'i'));
            });

            angular.forEach(items, function (item)
            {
                for (i in res)
                {
                    re = res[i];
                    if (re.test(item.un) || re.test(item.name) || re.test(item.team)) {
                        filtered.push(item);
                        break;
                    }
                }
            });

            return filtered;
        }
    })

    .directive('genRuleProcessUnmatched', function() {
        return {
            restrict: 'A',
            templateUrl: '/repo/team/genRuleProcessUnmatched.tpl.html',
            scope: true,
            controller: [function() { }]
        }
    })

    .directive('genRuleStop', function() {
        return {
            restrict: 'A',
            templateUrl: '/repo/team/genRuleStop.tpl.html',
            scope: true,
            controller: [function() { }]
        }
    })

    .directive('keyRuleEntry', function() {
        return {
            restrict: 'A',
            templateUrl: '/repo/team/keyRuleEntry.tpl.html',
            scope: true,
            controller: [function() { }]
        }
    })

    .directive('valueRuleEntry', function() {
        return {
            restrict: 'A',
            templateUrl: '/repo/team/valueRuleEntry.tpl.html',
            scope: true,
            controller: [function() { }]
        }
    })

;