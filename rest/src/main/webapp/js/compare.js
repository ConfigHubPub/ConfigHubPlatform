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
    .module('configHub.repository.compare', [
        'angularUtils.directives.dirPagination',
        'configHub.repository.contextService',
        'selectize',
        'monospaced.elastic',
        'ngAnimate',
        'ui.keypress',
        'puElasticInput'
    ])

    .service('diffService', ['toUtc', '$http', '$q', '$stateParams', '$httpParamSerializer',
        function (toUtc, $http, $q, $stateParams, $httpParamSerializer)
        {
            // Return the public API.
            return ({
                cancel: cancel,
                getDiff: getDiff,
                getJSONDiff: getJSONDiff
            });

            function cancel(promise)
            {
                if (promise &&
                    promise._httpTimeout &&
                    promise._httpTimeout.resolve) {
                    promise._httpTimeout.resolve();
                }
            }

            function getDiff(allKeys, diffOnly, A, B, key, allValues, aPass, bPass)
            {
                var httpTimeout = $q.defer(),
                    params = {
                        aContext: contextParam(A.chosenContext),
                        aTag: A.selectedTag,
                        aTs: toUtc.toMS(A.date),
                        bContext: contextParam(B.chosenContext),
                        bTag: B.selectedTag,
                        bTs: toUtc.toMS(B.date),
                        allKeys: allKeys,
                        diffOnly: diffOnly,
                        allValues: allValues,
                        key: key,
                        aPass: aPass,
                        bPass: bPass
                    },

                    request = $http({
                        method: 'GET',
                        url: '/rest/compareResolver/' + $stateParams.owner + "/" + $stateParams.name,
                        params: params,
                        timeout: httpTimeout.promise
                    }),

                    promise = request.then(function (response) {return response.data; });
                promise._httpTimeout = httpTimeout;

                return ( promise );
            }

            function getJSONDiff(A, B)
            {
                var httpTimeout = $q.defer(),
                    params = {
                        aContext: contextParam(A.chosenContext),
                        aTag: A.selectedTag,
                        aTs: toUtc.toMS(A.date),

                        bContext: contextParam(B.chosenContext),
                        bTag: B.selectedTag,
                        bTs: toUtc.toMS(B.date)
                    },

                    request = $http({
                        method: 'POST',
                        url: '/rest/getJSONDiff/' + $stateParams.owner + "/" + $stateParams.name,
                        data: $httpParamSerializer(params),
                        timeout: httpTimeout.promise,
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }),

                    promise = request.then(function (response) {return response.data; });
                promise._httpTimeout = httpTimeout;

                return ( promise );
            }
        }])

    .controller('CompareController',
        ['$timeout', '$stateParams', '$scope', '$rootScope', '$http', 'store', 'diffService',
            'contextService', '$filter', '$state', 'toUtc', 'secretService',
            function ($timeout, $stateParams, $scope, $rootScope, $http, store, diffService,
                      contextService, $filter, $state, toUtc, secretService)
            {
                $scope.comparisonView = true;
                $scope.tsFormat = tsFormat;
                $scope.loading = false;

                $scope.repoName = $stateParams.name;
                $scope.account = $stateParams.owner;
                $rootScope.selectedTab = 0;

                $scope.ctxStoreName = 'c_' + $stateParams.owner + "/" + $stateParams.name;
                $scope.hdcStoreName = 'hcd_' + $stateParams.owner + "/" + $stateParams.name;


                $scope.config = {};
                $scope.updateJSONDiff = 0;

                var diffOnlyStoreName = 'dfo_' + $scope.account + "/" + $scope.repoName,
                    lastRequest,
                    lastInitializedRequest,
                    watchersInitialized = false,
                    orderBy = $filter('orderBy'),
                    i, j,
                    score,
                    Side,
                    oa = {},
                    ob = {},
                    t = store.get($scope.ctxStoreName),
                    split = false,
                    index,
                    aPass, bPass,
                    newEntry,
                    tag,
                    ts,
                    ctxParsed,
                    resetAttempts = 0,
                    pst = store.get('pageSize');


                function processJSONDiff()
                {
                    $scope.initialized = false;

                    if (lastInitializedRequest)
                        $timeout.cancel(lastInitializedRequest);

                    lastInitializedRequest =
                        $timeout(function ()
                        {
                            diffService.cancel(lastRequest);
                            $scope.loading = true;

                            lastRequest = diffService.getJSONDiff($scope.A, $scope.B);

                            lastRequest.then(
                                function handleDiffResolve(response)
                                {
                                    if (response.error) {
                                        if (resetAttempts == 0 && response.resetContext) {
                                            resetAttempts = 1;
                                            $scope.left = {};
                                            $scope.right = {};
                                            store.set($scope.ctxStoreName, JSON.stringify($scope.A.chosenContext));
                                            processJSONDiff();
                                        }
                                        else
                                        {
                                            $scope.error = response.error;
                                            $scope.left = {};
                                            $scope.right = {};
                                        }
                                    }
                                    else
                                    {
                                        $scope.left = response.left;
                                        $scope.right = response.right;
                                        $scope.initialized = true;
                                        watchersInitialized = true;
                                        $scope.propertiesLoaded = true;
                                        $scope.loading = false;
                                        $scope.error = '';
                                        $scope.updateJSONDiff++;
                                    }

                                    $scope.isJSONDiff = true;
                                }
                            );

                            return (lastRequest);
                        }, lastInitializedRequest ? 500 : 0);
                }

                function processDiff()
                {
                    $scope.initialized = false;

                    if (lastInitializedRequest)
                        $timeout.cancel(lastInitializedRequest);

                    lastInitializedRequest =
                        $timeout(function ()
                        {
                            diffService.cancel(lastRequest);
                            $scope.loading = true;

                                lastRequest = diffService.getDiff($scope.allKeys, $scope.diffOnly, $scope.A, $scope.B);

                                lastRequest.then(
                                    function handleDiffResolve(response)
                                    {
                                        if (response.error)
                                        {
                                            if (resetAttempts == 0 && response.resetContext)
                                            {
                                                resetAttempts = 1;
                                                $scope.A.chosenContext = {};
                                                $scope.B.chosenContext = {};
                                                store.set($scope.ctxStoreName, JSON.stringify($scope.A.chosenContext));
                                                processDiff();
                                            }
                                            else
                                            {
                                                $scope.error = response.error;
                                            }
                                        }
                                        else
                                        {
                                            $scope.config = response.diff;
                                            $scope.dateCmp = response.date;
                                            $scope.initialized = true;
                                            watchersInitialized = true;
                                            $scope.propertiesLoaded = true;
                                            $scope.loading = false;
                                            $scope.error = '';
                                        }

                                        $scope.isJSONDiff = false;
                                    }
                                );


                            return (lastRequest);
                        }, lastInitializedRequest ? 500 : 0);
                }

                $scope.aContextSelectConfig = angular.copy(contextSelectConfig);
                $scope.bContextSelectConfig = angular.copy(contextSelectConfig);

                $scope.aTagSelectConfig = angular.copy(tagSelectConfig);
                $scope.bTagSelectConfig = angular.copy(tagSelectConfig);

                $scope.allKeys = false;
                $scope.allKeyToggle = function ()
                {
                    $scope.allKeys = !$scope.allKeys;
                    processDiff();
                };

                $scope.diffOnly = store.get(diffOnlyStoreName);
                $scope.diffOnlyToggle = function ()
                {
                    $scope.diffOnly = !$scope.diffOnly;
                    store.set(diffOnlyStoreName, $scope.diffOnly);
                    processDiff();
                    $scope.allKeys = false;
                };

                $scope.now = Date.now();

                $scope.ctxEq = {};

                function updateContextDiff()
                {
                    for (i in scores) {
                        score = scores[i];
                        $scope.ctxEq[score] = sameArrays($scope.A.chosenContext[score],
                            $scope.B.chosenContext[score],
                            orderBy);
                    }

                    j = true;
                    for (i in $scope.A.repoContext.depthScores)
                    {
                        if (!$scope.A.chosenContext[$scope.A.repoContext.depthScores[i]]
                            || $scope.A.chosenContext[$scope.A.repoContext.depthScores[i]].length != 1)
                        {
                            j = false;
                            break;
                        }
                    }
                    if (j)
                    {
                        for (i in $scope.B.repoContext.depthScores)
                        {
                            if (!$scope.B.chosenContext[$scope.B.repoContext.depthScores[i]]
                                || $scope.B.chosenContext[$scope.B.repoContext.depthScores[i]].length != 1)
                            {
                                j = false;
                                break;
                            }
                        }
                    }
                    $scope.fullContext = j;
                }

                Side = function (id)
                {
                    this.id = id;
                    this.selectedTag = null;
                    this.repoContext = {loaded: false};
                    this.chosenContext = {};

                    this.tagLabel = null;
                    this.timeToSet = null;
                    this.timeLabel = null;

                    var tsl = 'A' === this.id ? 'ats' : 'bts',
                        tl = 'A' === this.id ? 'at' : 'bt',
                        i,
                        tag;

                    this.tagLabel = this.selectedTag = $stateParams[tl];

                    if ($stateParams[tsl]) {
                        this.date = new Date(JSON.parse($stateParams[tsl]));
                        this.timeToSet = this.timeLabel = this.date;
                    }
                    else {
                        this.date = null;
                    }

                    this.getRepoTs = function ()
                    {
                        if (null === this.date)
                            return $scope.now;
                        return this.date;
                    };

                    this.goLive = function ()
                    {
                        this.selectedTag = null;
                        this.timeLabel = null;
                        this.tagLabel = null;
                        this.timeToSet = null;

                        if ('A' === this.id)
                            $state.go('repo.compare',
                                {owner: $scope.account, name: $scope.repoName, at: '', ats: ''},
                                {notify: false});
                        else
                            $state.go('repo.compare',
                                {owner: $scope.account, name: $scope.repoName, bt: '', bts: ''},
                                {notify: false});

                        if (null != this.date) {
                            this.date = null;
                            $scope.now = Date.now();

                            getContextItems(this);
                            if ($scope.isJSONDiff)
                                processJSONDiff();
                            else
                                processDiff();
                        }
                    };

                    this.changeDate = function (timeToSet)
                    {
                        this.selectedTag = null;
                        this.timeToSet = timeToSet;
                    };

                    this.tagChange = function (selectedTag)
                    {
                        i = indexOf(this.repoContext.tags, 'name', selectedTag)
                        if (-1 == i) {
                            getContextItems(this);
                            if ($scope.isJSONDiff)
                                processJSONDiff();
                            else
                                processDiff();
                            return;
                        }

                        tag = this.repoContext.tags[i];
                        this.timeToSet = new Date(tag.ts);
                        this.selectedTag = selectedTag;
                    };

                    this.setDayTime = function ()
                    {
                        if ('A' === this.id)
                            $state.go('repo.compare',
                                {
                                    owner: $scope.account,
                                    name: $scope.repoName,
                                    at: this.selectedTag,
                                    ats: toUtc.toMS(this.timeToSet)
                                },
                                {notify: false});
                        else
                            $state.go('repo.compare',
                                {
                                    owner: $scope.account,
                                    name: $scope.repoName,
                                    bt: this.selectedTag,
                                    bts: toUtc.toMS(this.timeToSet)
                                },
                                {notify: false});

                        this.tagLabel = this.selectedTag;
                        this.date = this.timeLabel = this.timeToSet;

                        getContextItems(this);
                        if ($scope.isJSONDiff)
                            processJSONDiff();
                        else
                            processDiff();
                    };

                    this.getDate = function (side)
                    {
                        return this.date;
                    };

                    this.isLive = function ()
                    {
                        return null == this.date && null == this.selectedTag;
                    };
                };

                /**
                 * API
                 *
                 * Value is editable iff repository is Live and use has access to the value
                 * @param value
                 * @returns {*|boolean}
                 */
                $scope.isValueEditable = function (value, side)
                {
                    if ($scope.ut == $scope.type.demo) return true;
                    if ($scope.ut < $scope.type.demo) return false;

                    if (!value.editable) return false;
                    if (0 == side)
                        return $scope.A.date == null;
                    if (2 == side)
                        return $scope.B.date == null;

                    return false
                };

                $scope.A = new Side('A');
                $scope.B = new Side('B');

                //------------------------------------------------------------------------------

                $scope.now = Date.now();

                $scope.getDate = function (side)
                {
                    if (0 == side)
                        return $scope.A.date;
                    if (2 == side)
                        return $scope.B.date;
                    return null;
                };
                // --------------------------------
                // Repo resolution
                // --------------------------------

                if (t) {
                    ctxParsed = JSON.parse(t);
                    for (i in ctxParsed) {
                        if (ctxParsed[i] && ctxParsed[i].length > 0) {
                            if (ctxParsed[i].length == 2) {
                                oa[i] = [ ctxParsed[i][0] ];
                                ob[i] = [ ctxParsed[i][1] ];
                            }
                            else {
                                split = true;
                                oa[i] = ctxParsed[i];
                                ob[i] = angular.copy(ctxParsed[i]);
                            }
                        }
                    }
                }

                $scope.A.chosenContext = oa;
                $scope.B.chosenContext = ob;
                //------------------------------------------------------------------------------

                $scope.$watch($scope.A.id + '.chosenContext', function (newVal, oldVal)
                {
                    if (!$scope.A.repoContext.loaded) return;
                    if (watchersInitialized) {
                        updateContextDiff();
                        if ($scope.isJSONDiff)
                            processJSONDiff();
                        else
                            processDiff();
                    }
                }, true);

                $scope.$watch($scope.B.id + '.chosenContext', function (newVal, oldVal)
                {
                    if (!$scope.B.repoContext.loaded) return;
                    if (watchersInitialized) {
                        updateContextDiff();
                        if ($scope.isJSONDiff)
                            processJSONDiff();
                        else
                            processDiff();
                    }
                }, true);

                $scope.propertiesLoaded = false;

                /**
                 * API
                 *
                 */
                $scope.isLive = function (side)
                {
                    if (0 == side)
                        return $scope.A.date == null && null == $scope.A.selectedTag;
                    if (2 == side)
                        return $scope.B.date == null && null == $scope.B.selectedTag;

                    return false;
                };

                $scope.lineupContext = false;
                $scope.toggleContextLineup = function ()
                {
                    $scope.lineupContext = !$scope.lineupContext;
                };

                /**
                 * API
                 *
                 * @param key1
                 * @param key2
                 * @param allValues
                 * @param entry
                 */
                $scope.resolveEntries = function (key1, key2, allValues, entry)
                {
                    console.log("compare.js :: resolveEntries");
                };

                /**
                 * API
                 *
                 * @param key
                 * @param entry
                 * @param allValues
                 */
                $scope.getAllValuesForDetachedEntry = function (key, entry, allValues)
                {
                    // this is called when a new property is getting created
                    console.log("compare.js :: getAllValuesForDetachedEntry " + key);
                };

                /**
                 * API
                 *
                 * Refresh specific entry
                 * @param entry
                 */
                $scope.resolveEntry = function (key, allValues, password, callback)
                {
                    if ($scope.isJSONDiff)
                        processJSONDiff();
                    else
                    {
                        index = indexOfEntry($scope.config, key);

                        if ($scope.config[index][0].encrypted && password)
                            aPass = secretService.get($scope.config[index][0].spName, $scope.A.date);

                        if ($scope.config[index][2].encrypted && password)
                            bPass = secretService.get($scope.config[index][2].spName, $scope.B.date);

                        lastRequest = diffService.getDiff($scope.allKeys,
                            $scope.diffOnly,
                            $scope.A,
                            $scope.B,
                            key,
                            allValues,
                            aPass ? aPass : password,
                            bPass ? bPass : password);

                        lastRequest.then(
                            function handleDiffResolve(response)
                            {
                                if (response.error) {
                                    if (resetAttempts == 0 && response.resetContext) {
                                        resetAttempts = 1;
                                        $scope.A.chosenContext = {};
                                        $scope.B.chosenContext = {};
                                        store.set($scope.ctxStoreName, JSON.stringify($scope.A.chosenContext));
                                        processDiff();
                                    }
                                    else {
                                        $scope.error = response.error;
                                    }
                                }
                                else {
                                    // Nothing to display, remove it if its already there
                                    if (!response.diff || !response.diff[0]) {
                                        // remove entry
                                        if (!key) return;

                                        index = indexOfEntry($scope.config, key);
                                        if (index > -1)
                                            $scope.config.splice(index, 1);

                                        return;
                                    }

                                    newEntry = response.diff[0];
                                    index = indexOfEntry($scope.config, newEntry.key);

                                    $scope.config[index].f.k[0].decrypted = response.aDecrypted;
                                    $scope.config[index].f.k[2].decrypted = response.bDecrypted;

                                    // Currently there is no such entry
                                    if (index < 0) {
                                        $scope.config.push(newEntry);
                                        return;
                                    }

                                    // add keys
                                    if (!newEntry[0])
                                        delete $scope.config[index][0];
                                    else if (!$scope.config[index][0])
                                        $scope.config[index][0] = newEntry[0];
                                    else {
                                        $scope.config[index][0].readme = newEntry[0].readme;
                                        $scope.config[index][0].deprecated = newEntry[0].deprecated;
                                        $scope.config[index][0].uses = newEntry[0].uses;
                                        $scope.config[index][0].spName = newEntry[0].spName;
                                        $scope.config[index][0].spCipher = newEntry[0].spCipher;
                                        $scope.config[index][0].encrypted = newEntry[0].encrypted;
                                        $scope.config[index][0].vdt = newEntry[0].vdt;
                                    }

                                    if (!newEntry[2])
                                        delete $scope.config[index][2];
                                    else if (!$scope.config[index][2])
                                        $scope.config[index][2] = newEntry[2];
                                    else {
                                        $scope.config[index][2].readme = newEntry[2].readme;
                                        $scope.config[index][2].deprecated = newEntry[2].deprecated;
                                        $scope.config[index][2].uses = newEntry[2].uses;
                                        $scope.config[index][2].spName = newEntry[2].spName;
                                        $scope.config[index][2].spCipher = newEntry[2].spCipher;
                                        $scope.config[index][2].encrypted = newEntry[2].encrypted;
                                        $scope.config[index][2].vdt = newEntry[2].vdt;
                                    }

                                    // So far good

                                    // ToDo: handle opened values
                                    //for (var rpi in newEntry.properties)
                                    //{
                                    //    var pi = indexOfCmpPropertyA($scope.config[index].properties, newEntry.properties[rpi]);
                                    //    if (pi >= 0)
                                    //    {
                                    //        var pair = $scope.config[index].properties[pi];
                                    //
                                    //        if (pair.a && pair.a.isEdited)
                                    //        {
                                    //            console.log("found pair:");
                                    //            console.log(pair);
                                    //            console.log("new: ");
                                    //            console.log(newEntry.properties[rpi].a);
                                    //
                                    //            pair.a.type = newEntry.properties[rpi].a.type;
                                    //            pair.a.score = newEntry.properties[rpi].a.score;
                                    //            pair.a.attr = newEntry.properties[rpi].a.attr;
                                    //
                                    //            newEntry.properties[rpi].a = pair.a;
                                    //
                                    //            console.log("new pair: ");
                                    //            console.log(newEntry.properties[rpi]);
                                    //        }
                                    //    }
                                    //
                                    //    var pi = indexOfCmpPropertyB($scope.config[index].properties, newEntry.properties[rpi]);
                                    //    if (pi >= 0)
                                    //    {
                                    //        var pair = $scope.config[index].properties[pi];
                                    //        if (pair.b && pair.b.isEdited)
                                    //        {
                                    //            pair.b.type = newEntry.properties[rpi].b.type;
                                    //            pair.b.score = newEntry.properties[rpi].b.score;
                                    //            pair.b.attr = newEntry.properties[rpi].b.attr;
                                    //            newEntry.properties[rpi].b = pair.b;
                                    //        }
                                    //    }
                                    //
                                    //}

                                    $scope.config[index].properties = newEntry.properties;
                                    $scope.config[index].allValues = allValues;

                                    if (callback) callback();
                                }
                            }
                        );
                    }
                };

                /**
                 * API
                 *
                 * After new property, or a value update has been created, update entity
                 * @param value
                 */
                $scope.postValueModification = function (key, allValues, callback)
                {
                    $scope.resolveEntry(key, allValues, null, callback);
                };

                lastRequest = processDiff();
                getContextItems($scope.A);
                getContextItems($scope.B);
                updateContextDiff();

                // --------------------------------
                // Repo context
                // --------------------------------
                $scope.getRepoContext = function (property)
                {
                    if ('A' === property.side)
                        return $scope.A.repoContext;
                    return $scope.B.repoContext;
                };

                function getContextItems(side)
                {
                    side.repoContext.loaded = false;
                    contextService
                        .contextElements(side.date, side.selectedTag, $scope.account, $scope.repoName)
                        .then(function (ctx)
                        {
                            side.repoContext = ctx;

                            if (side.selectedTag) {
                                i = indexOf(side.repoContext.tags, 'name', side.selectedTag);
                                if (i < 0)
                                    side.selectedTag = '';
                                else {
                                    tag = side.repoContext.tags[i];
                                    ts = tag.ts;
                                    side.timeLabel = side.timeToSet = new Date(tag.ts);
                                    side.date = new Date(JSON.parse(ts));
                                }
                            }

                            side.repoContext.loaded = true;
                        });
                }

                $scope.isJSONDiff = false;

                $scope.sideBySide = store.get("splitView");
                if (!$scope.sideBySide)
                    $scope.sideBySide = false;

                $scope.jsonDiff = function(showJSON, sideBySide)
                {
                    if (showJSON) {
                        $scope.sideBySide = sideBySide;
                        store.set("splitView", $scope.sideBySide);

                        console.log($scope.sideBySide)
                    }

                    if (showJSON)
                        processJSONDiff();
                    else
                        processDiff();
                };

                $scope.single = function ()
                {
                    $state.go('repo.editor', {
                        owner: $scope.account,
                        name: $scope.repoName
                    });
                };

                // --------------------------------
                // Pagination
                // --------------------------------
                $scope.currentPage = 1;


                $scope.pageSize = pst ? pst : 10;
                $scope.pageSizes = {
                    sizes: [{id: 10, name: '10'}, {id: 25, name: '25'}, {id: 50, name: '50'}, {id: 150, name: '150'}],
                    selectedOption: {id: $scope.pageSize, name: $scope.pageSize}
                };
                $scope.pageSizeUpdate = function ()
                {
                    store.set('pageSize', $scope.pageSizes.selectedOption.name);
                };

                $scope.reverse = false;
                $scope.toggleReverse = function () { $scope.reverse = !$scope.reverse; };

                // --------------------------------
                // New property
                // --------------------------------
                $scope.enableNewPropertyForm = false;
                $scope.showNewPropertyForm = function () { $scope.enableNewPropertyForm = true; };
                $scope.hideNewPropertyForm = function () { $scope.enableNewPropertyForm = false; };

                // --------------------------------
                // Value-Data-Type
                // --------------------------------
                $scope.dts = dts;
                $scope.DTConfig = DTConfig;

                // --------------------------------
                // Security
                // --------------------------------
                $scope.encryptionProfiles = [];
                $scope.SGConfig = SGConfig;
                $scope.CipherConfig = CipherConfig;
                $scope.ciphers = [];
                $scope.newEncriptionProfile = false;

                $http({
                    method: 'GET',
                    url: "/rest/getSecurityProfiles/" + $stateParams.owner + "/" + $stateParams.name
                }).then(function successCallback(response) {
                    $scope.encryptionProfiles = response.data.groups;
                    $scope.ciphers = response.data.ciphers;

                    if (!$scope.encryptionProfiles || $scope.encryptionProfiles.length == 0)
                        $scope.newEncriptionProfile = true;
                });

            }])

    .directive('jsonDiff', ['$compile', function ($compile)
    {
        return {
            restrict: 'A',
            replace: true,
            link: function ($scope, element)
            {
                $scope.$watch('updateJSONDiff', function() { update(); });
                var previousContent = false;

                function update()
                {
                    if (previousContent) {
                        element.empty();
                    }

                    var dd = difflib.unifiedDiff((angular.toJson($scope.left, true)).split('\n'),
                        (angular.toJson($scope.right, true)).split('\n'), {
                            fromfile: 'file.json',
                            tofile: 'file.json',
                            lineterm: ''
                        });

                    var diff2htmlUi = new Diff2HtmlUI({ diff: dd.join("\n") });
                    diff2htmlUi.draw(element, {
                        outputFormat: $scope.sideBySide ? 'side-by-side' : 'line-by-line',
                        showFiles: false,
                        matching: 'none'
                    });
                    diff2htmlUi.highlightCode(element);
                    previousContent = true;
                }
            }
        }
    }])
;
