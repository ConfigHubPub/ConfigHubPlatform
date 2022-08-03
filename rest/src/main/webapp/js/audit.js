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
    .module('configHub.repository.audit', [])

    .controller('AuditController',
        ['$scope', '$stateParams', '$rootScope',
            function ($scope, $stateParams, $rootScope)
            {

                $scope.repoName = $stateParams.name;
                $scope.account = $stateParams.owner;
                $rootScope.selectedTab = 6;
            }])

    .controller('RepoAuditController',
        ['$scope', '$stateParams',
            function($scope, $stateParams) {

                $scope.repoName = $stateParams.name;
                $scope.account = $stateParams.owner;
            }])
    .directive('propertyAudit', ['store', function(store)
    {
        return {
            restrict: 'A',
            templateUrl: 'repo/audit/propertyAudit.tpl.html',
            scope: true,
            controller:
                ['$scope', '$http', '$stateParams', '$httpParamSerializer', '$filter', 'store', 'contextService',
                    function($scope, $http, $stateParams, $httpParamSerializer, $filter, store, contextService)
                    {
                        var t = store.get("repoAuditTypes"),
                            firstId = 0,
                            lastId = 0,
                            orderBy = $filter('orderBy'),
                            v = store.get("splitView");

                        $scope.sideBySide = v ? true : false;
                        $scope.toggleSideBySide = function ()
                        {
                            $scope.sideBySide = !$scope.sideBySide;
                            $scope.$emit('sideBySide', $scope.sideBySide);
                            store.set("splitView", $scope.sideBySide);
                            getRepoAudit(false);
                        };

                        $scope.repoContext = { loaded: false };
                        contextService.contextElements($scope.date, null, $scope.account, $scope.repoName).then(function(ctx) {
                            $scope.repoContext = ctx;
                            $scope.repoContext.loaded = true;
                        });

                        $scope.tsFormat = tsFormat;

                        if (!$scope.mode)
                            $scope.mode = 'all';

                        $scope.$watch('auditRefreshCnt', function(newVal, oldVal)
                        {
                            $scope.goToLatest();
                        }, true);

                        $scope.labels = {};
                        $scope.attention = false;

                        $scope.toggleAttention = function() {
                            $scope.attention = !$scope.attention;
                            $scope.goToLatest();
                        };



                        if (t) {
                            $scope.selectedTypes = JSON.parse(t);
                        }
                        else
                        {
                            $scope.selectedTypes = ["Config"];
                            store.set("repoAuditTypes", JSON.stringify($scope.selectedTypes));
                        }

                        $scope.recordTypes = [
                            { "value": "Config", "label": "<i class='dlbl props'></i> Properties" },
                            { "value": "Files", "label": "<i class='dlbl files'></i> Files" },
                            { "value": "Tokens", "label": "<i class='dlbl token'></i> Tokens" },
                            { "value": "Security", "label": "<i class='dlbl security'></i> Security" },
                            { "value": "Tags", "label": "<i class='dlbl tags'></i> Tags" },
                            { "value": "Teams", "label": "<i class='dlbl team'></i> Teams" },
                            { "value": "RepoSettings", "label": "<i class='dlbl repo'></i> Settings" }
                        ];

                        $scope.goToLatest = function()
                        {
                            firstId = 0;
                            lastId = 0;
                            getRepoAudit();
                        };

                        $scope.move = function(forward)
                        {
                            getRepoAudit(forward);
                        };

                        $scope.updateSearch = function()
                        {
                            store.set("repoAuditTypes", JSON.stringify($scope.selectedTypes));
                            firstId = 0;
                            lastId = 0;
                            getRepoAudit()
                        };

                        $scope.audit = [];
                        $scope.lastCommitNo = -1;

                        $scope.loadMore = function(commit)
                        {
                            t = Math.floor(commit.count / 10);
                            if (t > 500) t = 500;
                            if (t < 100) t = 100;
                            commit.limit += t;
                        };

                        $scope.getCommitModifications = function(commit)
                        {
                            commit.loading = true;

                            $http({
                                method: 'GET',
                                url: '/rest/getCommit/' + $scope.account + "/" + $scope.repoName,
                                params: { rev: commit.rev },
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function successCallback(response) {
                                commit.records = response.data.audit[0].records;
                                commit.overloaded = false;
                            });
                        };

                        function getRepoAudit(forward)
                        {
                            if (!$scope.account || !$scope.repoName)
                                return;

                            $http({
                                method: 'POST',
                                url: '/rest/getPropertyAudit/' + $scope.account + "/" + $scope.repoName + "/" + $scope.property.id,
                                data: $httpParamSerializer({
                                    recordTypes: $scope.selectedTypes.join(','),
                                    max: 10,
                                    starting: null == forward ? firstId : forward ? lastId : firstId,
                                    direction: null == forward ? 0 : forward ? 1 : -1,
                                    attention: $scope.attention
                                }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function successCallback(response)
                            {

                                if (response.data.success)
                                {
                                    $scope.labels = response.data.labels;
                                    $scope.audit = orderBy(response.data.audit, '-ts');

                                    if ($scope.audit && $scope.audit.length > 0) {
                                        firstId = $scope.audit[0].rev;
                                        lastId = $scope.audit[$scope.audit.length-1].rev;
                                    }
                                }
                            });
                        }

                        $scope.getLabel = function(placement)
                        {
                            if ($scope.labels[placement])
                                return $scope.labels[placement];

                            return "Label not found";
                        };

                        $scope.editComment = function(commit, comment)
                        {
                            $http({
                                method: 'POST',
                                url: '/rest/editCommitComment/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer({
                                    commitId: commit.rev,
                                    comment: comment
                                }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function successCallback(response)
                            {
                                if (response.data.success)
                                {
                                    commit.comment = comment;
                                    commit.editComment = false;
                                }
                                else
                                    commit.error = response.data.message;
                            });
                        }

                    }]
        }
    }])
    .directive('repoAudit', ['store', function(store)
    {
        return {
            restrict: 'A',
            templateUrl: 'repo/audit/repositoryAudit.tpl.html',
            scope: true,
            controller:
                ['$scope', '$http', '$stateParams', '$httpParamSerializer', '$filter', 'store', 'contextService',
                    function($scope, $http, $stateParams, $httpParamSerializer, $filter, store, contextService)
                    {
                        var t = store.get("repoAuditTypes"),
                            firstId = 0,
                            lastId = 0,
                            orderBy = $filter('orderBy'),
                            v = store.get("splitView");

                        $scope.sideBySide = v ? true : false;
                        $scope.toggleSideBySide = function ()
                        {
                            $scope.sideBySide = !$scope.sideBySide;
                            $scope.$emit('sideBySide', $scope.sideBySide);
                            store.set("splitView", $scope.sideBySide);
                            getRepoAudit(false);
                        };

                        $scope.repoContext = { loaded: false };
                        contextService.contextElements($scope.date, null, $scope.account, $scope.repoName).then(function(ctx) {
                            $scope.repoContext = ctx;
                            $scope.repoContext.loaded = true;
                        });

                        $scope.tsFormat = tsFormat;

                        if (!$scope.mode)
                            $scope.mode = 'all';

                        $scope.$watch('auditRefreshCnt', function(newVal, oldVal)
                        {
                            $scope.goToLatest();
                        }, true);

                        $scope.labels = {};
                        $scope.attention = false;

                        $scope.toggleAttention = function() {
                            $scope.attention = !$scope.attention;
                            $scope.goToLatest();
                        };



                        if (t) {
                            $scope.selectedTypes = JSON.parse(t);
                        }
                        else
                        {
                            $scope.selectedTypes = ["Config"];
                            store.set("repoAuditTypes", JSON.stringify($scope.selectedTypes));
                        }

                        $scope.recordTypes = [
                            { "value": "Config", "label": "<i class='dlbl props'></i> Properties" },
                            { "value": "Files", "label": "<i class='dlbl files'></i> Files" },
                            { "value": "Tokens", "label": "<i class='dlbl token'></i> Tokens" },
                            { "value": "Security", "label": "<i class='dlbl security'></i> Security" },
                            { "value": "Tags", "label": "<i class='dlbl tags'></i> Tags" },
                            { "value": "Teams", "label": "<i class='dlbl team'></i> Teams" },
                            { "value": "RepoSettings", "label": "<i class='dlbl repo'></i> Settings" }
                        ];

                        $scope.goToLatest = function()
                        {
                            firstId = 0;
                            lastId = 0;
                            getRepoAudit();
                        };

                        $scope.move = function(forward)
                        {
                            getRepoAudit(forward);
                        };

                        $scope.updateSearch = function()
                        {
                            store.set("repoAuditTypes", JSON.stringify($scope.selectedTypes));
                            firstId = 0;
                            lastId = 0;
                            getRepoAudit()
                        };

                        $scope.audit = [];
                        $scope.lastCommitNo = -1;

                        $scope.loadMore = function(commit)
                        {
                            t = Math.floor(commit.count / 10);
                            if (t > 500) t = 500;
                            if (t < 100) t = 100;
                            commit.limit += t;
                        };

                        $scope.getCommitModifications = function(commit)
                        {
                            commit.loading = true;

                            $http({
                                method: 'GET',
                                url: '/rest/getCommit/' + $scope.account + "/" + $scope.repoName,
                                params: { rev: commit.rev },
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function successCallback(response) {
                                commit.records = response.data.audit[0].records;
                                commit.overloaded = false;
                            });
                        };

                        function getRepoAudit(forward)
                        {
                            if (!$scope.account || !$scope.repoName)
                                return;

                            switch ($scope.mode)
                            {
                                case 'all':

                                    $http({
                                        method: 'POST',
                                        url: '/rest/getRepositoryAudit/' + $scope.account + "/" + $scope.repoName,
                                        data: $httpParamSerializer({
                                            recordTypes: $scope.selectedTypes.join(','),
                                            max: 10,
                                            starting: null == forward ? firstId : forward ? lastId : firstId,
                                            direction: null == forward ? 0 : forward ? 1 : -1,
                                            attention: $scope.attention
                                        }),
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                    }).then(function successCallback(response)
                                    {

                                        if (response.data.success)
                                        {
                                            $scope.labels = response.data.labels;
                                            $scope.audit = orderBy(response.data.audit, '-ts');

                                            if ($scope.audit && $scope.audit.length > 0) {
                                                firstId = $scope.audit[0].rev;
                                                lastId = $scope.audit[$scope.audit.length-1].rev;
                                            }
                                        }
                                    });

                                    break;

                                case 'key':
                                    $http({
                                        method: 'POST',
                                        url: '/rest/getKeyAudit/' + $scope.account + "/" + $scope.repoName,
                                        data: $httpParamSerializer({
                                            key: $scope.key,
                                            max: 10,
                                            starting: null == forward ? firstId : forward ? lastId : firstId,
                                            direction: null == forward ? 0 : forward ? 1 : -1
                                        }),
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                    }).then(function successCallback(response)
                                    {
                                        if (response.data.success)
                                        {
                                            $scope.labels = response.data.labels;
                                            $scope.audit = orderBy(response.data.audit, '-ts');

                                            if ($scope.audit && $scope.audit.length > 0) {
                                                firstId = $scope.audit[0].rev;
                                                lastId = $scope.audit[$scope.audit.length-1].rev;
                                            }
                                        }
                                    });

                                    break;
                            }
                        }


                        $scope.getLabel = function(placement)
                        {
                            if ($scope.labels[placement])
                                return $scope.labels[placement];

                            return "Label not found";
                        };

                        $scope.editComment = function(commit, comment)
                        {
                            $http({
                                method: 'POST',
                                url: '/rest/editCommitComment/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer({
                                    commitId: commit.rev,
                                    comment: comment
                                }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function successCallback(response)
                            {
                                if (response.data.success)
                                {
                                    commit.comment = comment;
                                    commit.editComment = false;
                                }
                                else
                                    commit.error = response.data.message;
                            });
                        }

                    }]
        }
    }])

    .directive('tagDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/tagDiff.tpl.html',
            scope: true,
            controller: ['$scope', '$filter',
                function ($scope, $filter)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    var readmeDiffSet,
                        amDateFormat = $filter('amDateFormat');

                    if ($scope.record.revType === 'Modify')
                    {
                        readmeDiffSet = $scope.objKeyPresent($scope.diff, 'readme');
                        $scope.showReadme = readmeDiffSet || $scope.entry.readme;
                        $scope.readmeDiff = readmeDiffSet && $scope.diff.readme != $scope.entry.readme;

                        $scope.nameChange = $scope.objKeyPresent($scope.diff, 'name');

                        $scope.tsChange = $scope.objKeyPresent($scope.diff, 'ts');
                        if ($scope.tsChange)
                        {
                            $scope.ot = amDateFormat($scope.diff.ts, tsFormat) + " (Local time)";
                            $scope.ct = amDateFormat($scope.entry.ts, tsFormat) + " (Local time)";
                        }
                    }
                }]
        }
    })


    .directive('differ', [function ()
    {
        return {
            restrict: 'A',
            replace: true,
            link: function ($scope, element)
            {
                var old = $scope.oldContent,
                    curr = $scope.currContent;

                var dd = difflib.unifiedDiff(angular.isArray(old) ? old : old.split('\n'),
                    angular.isArray(curr) ? curr : curr.split('\n'), {
                        fromfile: 'Original',
                        tofile: 'Current',
                        lineterm: ''
                    });

                $scope.contentModified = dd.length > 0;

                if ($scope.contentModified)
                {
                    var diff2htmlUi = new Diff2HtmlUI({ diff: dd.join("\n") });
                    diff2htmlUi.draw(element, {
                        outputFormat: $scope.sideBySide ? 'side-by-side' : 'line-by-line',
                        showFiles: false,
                        matching: 'none'
                    });

                    if (null == $scope.noHighlight || !$scope.noHighlight)
                        diff2htmlUi.highlightCode(element);
                }
                else
                {
                    $scope.currContent = angular.isArray(curr) ? curr : curr.split("\n");
                }
            }
        }
    }])


    .directive('dirDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/dirDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                }]
        }
    })

    .directive('fileDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/fileDiff.tpl.html',
            scope: true,
            controller: ['$scope', '$http', '$httpParamSerializer', 'secretService',
                function ($scope, $http, $httpParamSerializer, secretService)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    $scope.oldContent = '';
                    $scope.currContent = '';

                    $scope.oldSpName = '';
                    $scope.currSpName = '';

                    var oldName, p, cIndex, i;
                    $scope.mod = false;

                    if ($scope.record.revType === 'Modify')
                    {
                        $scope.mod = true;

                        $scope.pathChange = $scope.objKeyPresent($scope.diff, 'absPath');
                        $scope.nameChange = $scope.objKeyPresent($scope.diff, 'fileName');
                        $scope.contentModified = true;
                        // $scope.contentChange = $scope.objKeyPresent($scope.diff, 'content');

                        $scope.oldContext = $scope.diff ? $scope.diff.context : null;
                        $scope.currContext = $scope.entry.levels;
                        $scope.currActive = $scope.entry.active;

                        $scope.spNameMatch = $scope.diff.spName === $scope.entry.spName;

                        if ($scope.spNameMatch)
                        {
                            $scope.currSpName = $scope.oldSpName = $scope.entry.spName;
                            $scope.encrypted = $scope.entry.encryptionState == 1;
                        }
                        else
                        {
                            $scope.currSpName = $scope.entry.spName;
                            $scope.oldSpName = $scope.diff.spName;

                            $scope.encrypted = $scope.diff.encrypted || $scope.entry.encryptionState == 1;
                        }

                        if (!$scope.encrypted)
                        {
                            if ($scope.diff.content)
                                $scope.oldContent = $scope.diff.content;
                            else
                                $scope.oldContent = $scope.entry.content;

                            $scope.currContent = $scope.entry.content;
                        }

                        for (i in $scope.currContext)
                        {
                            oldName = $scope.currContext[i].n ? $scope.currContext[i].n : '';
                            if ($scope.oldContext)
                            {
                                p = $scope.currContext[i].p;
                                cIndex = indexOf($scope.oldContext, 'p', p);
                                if (-1 != cIndex) {
                                    oldName = $scope.oldContext[cIndex].n ? $scope.oldContext[cIndex].n : '';
                                }
                            }
                            $scope.currContext[i].on = oldName;
                            if (!$scope.currContext[i].n) $scope.currContext[i].n = '';
                        }
                    }
                    else if ($scope.record.revType == 'Delete')
                    {
                        $scope.encrypted = $scope.entry.encryptionState == 1;
                        $scope.currSpName = $scope.oldSpName = $scope.entry.spName;
                        $scope.oldContent = $scope.entry.content.split("\n");
                        $scope.currContext = $scope.entry.levels;
                        $scope.currActive = $scope.entry.active;
                    }
                    else
                    {
                        $scope.currContext = $scope.entry.levels;
                        $scope.encrypted = $scope.entry.encryptionState == 1;
                        $scope.currSpName = $scope.oldSpName = $scope.entry.spName;
                        $scope.currContent = $scope.entry.content.split("\n");
                        $scope.currActive = $scope.entry.active;
                    }

                    var form;

                    $scope.decrypt = function()
                    {
                        if (!$scope.diff)
                            $scope.diff = {"name": $scope.entry.spName};
                        else
                        if (!$scope.diff.name)
                            $scope.diff.name = $scope.entry.spName;

                        if ($scope.spNameMatch || ((!$scope.diff || !$scope.diff.spName) && $scope.entry.spName))
                        {
                            secretService
                                .authAndExecAudit($scope,
                                    $scope.commit.ts,
                                    $scope.entry.spName,
                                    function (password)
                                    {
                                        form = {
                                            id: $scope.entry.id,
                                            revId: $scope.commit.rev,
                                            password: password,
                                            oldSpName: $scope.diff.spName,
                                            ts: $scope.commit.ts
                                        };

                                        $http({
                                            method: 'POST',
                                            url: '/rest/decryptAuditFile/' + $scope.account + "/" + $scope.repoName,
                                            data: $httpParamSerializer(form),
                                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                        }).then(function successCallback(response)
                                        {
                                            if (response.data.success) {
                                                if ($scope.record.revType == 'Delete')
                                                    $scope.oldContent = response.data.content.split("\n");

                                                else if ($scope.record.revType == 'Add')
                                                    $scope.currContent = response.data.content.split("\n");

                                                else {
                                                    $scope.oldContent = response.data.old;
                                                    $scope.currContent = response.data.content;
                                                }
                                                $scope.encrypted = false;
                                            }
                                        });
                                    }
                                );
                        }
                        else if ($scope.diff.spName && $scope.entry.spName)
                        {
                            secretService
                                .authSwitchAndExecAudit($scope,
                                    $scope.currSpName,
                                    $scope.oldSpName,
                                    $scope.commit.ts,
                                    function ()
                                    {
                                        form = {
                                            id: $scope.entry.id,
                                            revId: $scope.commit.rev,
                                            password: secretService.get($scope.currSpName, $scope.commit.ts),
                                            oldPass: secretService.get($scope.oldSpName, $scope.commit.ts),
                                            oldSpName: $scope.diff.spName,
                                            ts: $scope.commit.ts
                                        };

                                        $http({
                                            method: 'POST',
                                            url: '/rest/decryptAuditFile/' + $scope.account + "/" + $scope.repoName,
                                            data: $httpParamSerializer(form),
                                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                        }).then(function successCallback(response)
                                        {
                                            if (response.data.success) {
                                                if ($scope.record.revType == 'Delete')
                                                    $scope.oldContent = response.data.content;

                                                else if ($scope.record.revType == 'Add')
                                                    $scope.currContent = response.data.content.split("\n");

                                                else {
                                                    $scope.oldContent = response.data.old;
                                                    $scope.currContent = response.data.content;
                                                }
                                                $scope.encrypted = false;
                                            }
                                        });
                                    }
                                );
                        }
                        else if ($scope.diff.spName && !$scope.entry.spName)
                        {
                            secretService
                                .authAndExecAudit($scope,
                                    $scope.commit.ts,
                                    $scope.diff.spName,
                                    function (password)
                                    {
                                        form = {
                                            id: $scope.entry.id,
                                            revId: $scope.commit.rev,
                                            oldPass: password,
                                            oldSpName: $scope.diff.spName,
                                            ts: $scope.commit.ts
                                        };

                                        $http({
                                            method: 'POST',
                                            url: '/rest/decryptAuditFile/' + $scope.account + "/" + $scope.repoName,
                                            data: $httpParamSerializer(form),
                                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                        }).then(function successCallback(response)
                                        {
                                            if (response.data.success) {
                                                if ($scope.record.revType == 'Delete')
                                                    $scope.oldContent = response.data.content;

                                                else if ($scope.record.revType == 'Add')
                                                    $scope.currContent = response.data.content.split("\n");

                                                else {
                                                    $scope.oldContent = response.data.old;
                                                    $scope.currContent = response.data.content;
                                                }
                                                $scope.encrypted = false;
                                            }
                                        });
                                    }
                                );
                        }

                    };
                }]
        }
    })

    .directive('repositoryDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/repositoryDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;
                    $scope.oldCtx = '';
                    $scope.newCtx = '';
                    $scope.depthScores = depthScores;

                    $scope.contextScopeChanged = $scope.objKeyPresent($scope.diff, 'depth');
                    if ($scope.objKeyPresent($scope.diff, 'labels'))
                    {
                        var b = false,
                            i,
                            depthScore;


                        for (i in $scope.depthScores)
                        {
                            depthScore = $scope.depthScores[i];
                            if ($scope.diff.labels[depthScore])
                            {
                                if (b) $scope.oldCtx += " > ";
                                $scope.oldCtx += $scope.diff.labels[depthScore];
                                b = true;
                            }
                        }

                        b = false;
                        for (i in $scope.depthScores)
                        {
                            depthScore = $scope.depthScores[i];
                            if ($scope.entry.labels[depthScore])
                            {
                                if (b) $scope.newCtx += " > ";
                                $scope.newCtx += $scope.entry.labels[depthScore];
                                b = true;
                            }
                        }
                    }



                }]
        }
    })

    .directive('securityProfileDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/securityProfileDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    if ($scope.record.revType === 'Modify')
                    {
                        $scope.nameDiffSet = $scope.objKeyPresent($scope.diff, 'name');
                        $scope.encDiffSet = $scope.objKeyPresent($scope.diff, 'encrypted');
                        $scope.cipherDiffSet = $scope.objKeyPresent($scope.diff, 'cipher');
                    }
                }]
        }
    })

    .directive('keyDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/keyDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    var readmeDiffSet,
                        vdtDiffSet,
                        spDiffSet;

                    if ($scope.record.revType === 'Modify')
                    {
                        readmeDiffSet = $scope.objKeyPresent($scope.diff, 'readme');
                        $scope.showReadme = readmeDiffSet || $scope.entry[1].readme;
                        $scope.readmeDiff = readmeDiffSet && $scope.diff.readme != $scope.entry[1].readme;

                        vdtDiffSet = $scope.objKeyPresent($scope.diff, 'vdt');
                        $scope.showVdt = vdtDiffSet || ($scope.entry[1].vdt && $scope.entry[1].vdt != 'Text');
                        $scope.vdtDiff = vdtDiffSet && $scope.diff.vdt != $scope.entry[1].vdt;

                        spDiffSet = $scope.objKeyPresent($scope.diff, 'spName');
                        $scope.showSp = spDiffSet || $scope.entry[1].spName;
                        $scope.spDiff = spDiffSet && $scope.diff.spName != $scope.entry[1].spName;

                        $scope.keyDiffSet = $scope.objKeyPresent($scope.diff, 'key');
                    }
                }]
        }
    })

    .directive('contextItemDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/contextItemDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    $scope.assignmentDiff = $scope.objKeyPresent($scope.diff, 'assignments');
                    if ($scope.assignmentDiff)
                    {
                        $scope.diffAssignments = [];

                        var cur = $scope.entry.assignments ? $scope.entry.assignments : [],
                            old = $scope.diff.assignments ? $scope.diff.assignments : [],
                            i = 0,
                            index;

                        for (; i < cur.length; i++)
                        {
                            index = old.indexOf(cur[i]);
                            if (index >= 0)
                            {
                                $scope.diffAssignments.push({m: 0, n: old[index]});
                                old.splice(index,1);
                            }
                        }

                        for (i=0; i<$scope.diffAssignments.length; i++)
                            cur.splice(cur.indexOf($scope.diffAssignments[i].n), 1);

                        for (i=0; i < cur.length; i++)
                            $scope.diffAssignments.push({m: 1, n: cur[i]});

                        for (i=0; i < old.length; i++)
                            $scope.diffAssignments.push({m: -1, n: old[i]});

                    }

                }]
        }
    })

    .directive('propertyDiff', function() {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/valueDiff.tpl.html',
            scope: true,
            controller: ['$scope', '$attrs', '$http', '$httpParamSerializer', 'secretService',
                function($scope, $attrs, $http, $httpParamSerializer, secretService) {

                    var entry = $scope.record.entry,
                        diff = entry.diff,
                        revType = $scope.record.revType,
                        i,
                        oldName,
                        p,
                        cIndex;

                    $scope.oldContent = '';
                    $scope.currContent = '';

                    $scope.oldContext = diff ? diff.context : null;
                    $scope.currContext = entry.levels;

                    $scope.commit.hasKey = false;
                    $scope.shouldDisplayCommitData = function()
                    {
                        return $attrs.hasOwnProperty('shouldDisplayConcisely');
                    };

                    $scope.showKey = function()
                    {
                        if ($scope.commit.hasKey || $attrs.hasOwnProperty('shouldDisplayConcisely'))
                        {
                            return false;
                        }

                        for (i in $scope.commit.records)
                        {
                            if ($scope.commit.records[i].type === 'propertyKey')
                            {
                                $scope.commit.hasKey = true;
                                return false;
                            }
                        }

                        return true;
                    };

                    $scope.mod = false;
                    $scope.encryptionState = entry.encryptionState;

                    if (revType == 'Modify')
                    {
                        $scope.mod = true;

                        if (diff.hasOwnProperty('active'))
                            $scope.oldActive = diff.active;
                        else
                            $scope.oldActive = entry.active;
                        $scope.currActive = entry.active;

                        for (i in $scope.currContext)
                        {
                            oldName = $scope.currContext[i].n ? $scope.currContext[i].n : '';
                            if ($scope.oldContext)
                            {
                                p = $scope.currContext[i].p;
                                cIndex = indexOf($scope.oldContext, 'p', p);
                                if (-1 != cIndex) {
                                    oldName = $scope.oldContext[cIndex].n ? $scope.oldContext[cIndex].n : '';
                                }
                            }
                            $scope.currContext[i].on = oldName;
                            if (!$scope.currContext[i].n) $scope.currContext[i].n = '';
                        }

                        if (diff.value) {
                            if (angular.isObject(diff.value) || angular.isArray(diff.value))
                                $scope.oldContent = JSON.stringify(diff.value, null, 4).split("\n");
                            else
                                $scope.oldContent = diff.value.split("\n");
                        }
                        else {
                            if (angular.isObject(entry.value) || angular.isArray(entry.value))
                                $scope.oldContent = JSON.stringify(entry.value, null, 4);
                            else
                                $scope.oldContent = entry.value.split("\n");
                        }

                        if (angular.isObject(entry.value) || angular.isArray(entry.value))
                            $scope.currContent = JSON.stringify(entry.value, null, 4).split("\n");
                        else
                            $scope.currContent = entry.value ? entry.value.split("\n") : '';
                    }
                    else if (revType == 'Delete')
                    {
                        $scope.oldContext = entry.levels;
                        if (angular.isObject(entry.value) || angular.isArray(entry.value))
                            $scope.oldContent = JSON.stringify(entry.value, null, 4).split("\n");
                        else
                            $scope.oldContent = entry.value.split("\n");

                        $scope.currActive = entry.active;
                    }
                    else
                    {
                        $scope.currContext = entry.levels;
                        if (angular.isObject(entry.value) || angular.isArray(entry.value))
                            $scope.currContent = JSON.stringify(entry.value, null, 4).split("\n");
                        else
                            $scope.currContent = entry.value.split("\n");
                        $scope.currActive = entry.active;
                    }

                    $scope.decrypt = function()
                    {
                        secretService
                            .authAndExecAudit($scope,
                                $scope.commit.ts,
                                entry.spName,
                                function (password) {

                                    $http({
                                        method: 'POST',
                                        url: '/rest/decryptAuditValue/' + $scope.account + "/" + $scope.repoName,
                                        data: $httpParamSerializer({
                                            id: entry.id,
                                            revId: $scope.commit.rev,
                                            password: password
                                        }),
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                    }).then(function successCallback(response)
                                    {
                                        if (response.data.success) {
                                            if (revType == 'Delete')
                                                $scope.oldContent = response.data.value;
                                            else {
                                                $scope.oldContent = response.data.old;
                                                $scope.currContent = response.data.value;
                                            }
                                            $scope.encryptionState = 0;
                                        }
                                    });
                                }
                            );



                    };

                }]
        }
    })

    .directive('tokenDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/tokenDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    $scope.diff_active = $scope.diff && $scope.diff.hasOwnProperty('active') && $scope.diff.active != $scope.entry.active;
                    $scope.diff_forceKeyPushEnabled = $scope.diff && $scope.diff.hasOwnProperty('forceKeyPushEnabled') && $scope.diff.forceKeyPushEnabled != $scope.entry.forceKeyPushEnabled;
                    $scope.diff_rulesTeam = $scope.diff && $scope.diff.hasOwnProperty('rulesTeam') && $scope.diff.rulesTeam != $scope.entry.rulesTeam;
                    $scope.diff_managingTeam = $scope.diff && $scope.diff.hasOwnProperty('managingTeam') && $scope.diff.managingTeam != $scope.entry.managingTeam;
                    $scope.diff_user = $scope.diff && $scope.diff.hasOwnProperty('user') && $scope.diff.user != $scope.entry.user;
                    $scope.diff_managedBy = $scope.diff_user || ($scope.diff && $scope.diff.hasOwnProperty('managedBy') && $scope.diff.managedBy != $scope.entry.managedBy);

                    $scope.usedByWas = $scope.usedByIs = "";

                    if ($scope.diff_managedBy) {
                        switch ($scope.diff.managedBy ? $scope.diff.managedBy : $scope.entry.managedBy) {
                            case 'User': $scope.usedByWas = $scope.diff.user; break;
                            case 'Admins': $scope.usedByWas = 'Admins / Owners'; break;
                            case 'Team': $scope.usedByWas = $scope.diff.managingTeam; break;
                            case 'All': $scope.usedByWas = 'Everyone'; break;
                        }
                    }

                    switch ($scope.entry.managedBy) {
                        case 'User': $scope.usedByIs = $scope.entry.user; break;
                        case 'Admins': $scope.usedByIs = 'Admins / Owners'; break;
                        case 'Team': $scope.usedByIs = $scope.entry.managingTeam; break;
                        case 'All': $scope.usedByIs = 'Everyone'; break;
                    }

                    $scope.spsDiff = $scope.objKeyPresent($scope.diff, 'sps');
                    if ($scope.spsDiff)
                    {
                        $scope.diffSps = [];

                        var cur = $scope.entry.sps ? $scope.entry.sps : [],
                            old = $scope.diff.sps ? $scope.diff.sps : [],
                            i = 0,
                            index;

                        for (; i < cur.length; i++)
                        {
                            index = old.indexOf(cur[i]);
                            if (index >= 0)
                            {
                                $scope.diffSps.push({m: 0, n: old[index]});
                                old.splice(index,1);
                            }
                        }

                        for (i=0; i<$scope.diffSps.length; i++)
                            cur.splice(cur.indexOf($scope.diffSps[i].n), 1);

                        for (i=0; i < cur.length; i++)
                            $scope.diffSps.push({m: 1, n: cur[i]});

                        for (i=0; i < old.length; i++)
                            $scope.diffSps.push({m: -1, n: old[i]});

                    }

                }
            ]
        }
    })


    .directive('teamDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/teamDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;

                    $scope.genRuleProcessTypes = genRuleProcessTypes;
                }
            ]
        }
    })

    .directive('ruleDiff', function()
    {
        return {
            restrict: "A",
            templateUrl: 'repo/audit/ruleDiff.tpl.html',
            scope: true,
            controller: ['$scope',
                function ($scope)
                {
                    $scope.entry = $scope.record.entry;
                    $scope.diff = $scope.entry.diff;
                    $scope.oldContent = '';
                    $scope.currContent = '';

                    var o, n, score, tvar;


                    o = "#" + $scope.entry.priority + ". ";
                    o += ("rw" === $scope.entry.access ? "Read/Write " : "Read-Only ") + "when ";
                    if ("Key" === $scope.entry.type)
                    {
                        o += "key ";
                        switch($scope.entry.match)
                        {
                            case 'Is': o += "is "; break;
                            case 'StartsWith': o += "starts with "; break;
                            case 'EndsWith': o += "ends with "; break;
                            case 'Contains': o += "contains "; break;
                        }

                        o += "'" + $scope.entry.key + "'";
                    }
                    else
                    {
                        o += "context ";
                        switch($scope.entry.match)
                        {
                            case 'Resolves': o += "resolves "; break;
                            case 'DoesNotResolve': o += "does not resolve "; break;
                            case 'ContainsAny': o += "contains any "; break;
                            case 'ContainsAll': o += "contains all "; break;
                            case 'DoesNotContain': o += "does not contain "; break;
                        }

                        for (score in $scope.repoContext.depthScores)
                        {
                            o += "[ ";
                            if ($scope.entry.context[$scope.repoContext.depthScores[score]])
                                o += $scope.entry.context[$scope.repoContext.depthScores[score]];
                            else
                                o += "*";
                            o += " ]";

                            if ($scope.repoContext.depthScores.length -1 > parseInt(score))
                                o += " > ";
                        }
                    }

                    $scope.currContent = o;

                    if (!$scope.diff)
                        $scope.oldContent = o;
                    else
                    {
                        tvar = $scope.objKeyPresent($scope.diff, "priority") ? $scope.diff.priority : $scope.entry.priority;
                        n = "#" + tvar + ". ";

                        tvar = $scope.objKeyPresent($scope.diff, "access") ? $scope.diff.access : $scope.entry.access;
                        n += ("rw" === tvar ? "Read/Write " : "Read-Only ") + "when ";

                        tvar = $scope.objKeyPresent($scope.diff, "type") ? $scope.diff.type : $scope.entry.type;
                        if ("Key" === tvar)
                        {
                            n += "key ";
                            tvar = $scope.objKeyPresent($scope.diff, "match") ? $scope.diff.match : $scope.entry.match;
                            switch(tvar)
                            {
                                case 'Is': n += "is "; break;
                                case 'StartsWith': n += "starts with "; break;
                                case 'EndsWith': n += "ends with "; break;
                                case 'Contains': n += "contains "; break;
                            }

                            tvar = $scope.objKeyPresent($scope.diff, "key") ? $scope.diff.key : $scope.entry.key;
                            n += "'" + tvar + "'";
                        }
                        else
                        {
                            n += "context ";
                            tvar = $scope.objKeyPresent($scope.diff, "match") ? $scope.diff.match : $scope.entry.match;
                            switch(tvar)
                            {
                                case 'Resolves': n += "resolves "; break;
                                case 'DoesNotResolve': n += "does not resolve "; break;
                                case 'ContainsAny': n += "contains any "; break;
                                case 'ContainsAll': n += "contains all "; break;
                                case 'DoesNotContain': n += "does not contain "; break;
                            }

                            tvar = $scope.diff.context ? $scope.diff.context : $scope.entry.context;

                            for (score in $scope.repoContext.depthScores)
                            {
                                n += "[ ";
                                if (tvar[$scope.repoContext.depthScores[score]])
                                    n += tvar[$scope.repoContext.depthScores[score]];
                                else
                                    n += "*";
                                n += " ]";

                                if ($scope.repoContext.depthScores.length -1 > parseInt(score))
                                    n += " > ";
                            }
                        }

                        $scope.oldContent = n;
                    }

                    $scope.noHighlight = true;
                    $scope.genRuleProcessTypes = genRuleProcessTypes;
                }
            ]
        }
    })

    ;
