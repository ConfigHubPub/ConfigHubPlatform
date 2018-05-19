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
    .module('configHub.repository.editor', [
        'configHub.repository.entry',
        'configHub.repository.contextService'
    ])

    .service('editorInit', ['resolverService', 'store',
        function (resolverService, store)
        {
            return ({
                setState: setState,
                getState: getState,
                initialize: initialize,
                isLiteral: isLiteral,
                isKeyOnly: isKeyOnly,
                isSpOnly: isSpOnly,
                isFCOnly: isFCOnly,
                setContext: setContext,
                getContext: getContext,
                setKeys: setKeys,
                getKeys: getKeys,
                setConfig: setConfig,
                setContextOptions: setContextOptions,
                updateTime: updateTime
            });

            var state,
                editorScope,
                literal = false,
                o = {},
                c, t, i,
                sp,
                els,
                el,
                depth,
                ctxParsed,
                d = {};

            function updateTime(date, tag)
            {
                editorScope.setDate(date);
                editorScope.setTag(tag);
                editorScope.postTimeChange();
            }

            function setState(s)
            {
                state = s;
                literal = state == 'key' || state == 'contextItem';
            }

            function getState() { return state; }

            function setConfig(config, date) {
                editorScope.config = config;
                editorScope.date = date;
            }

            function isLiteral()
            {
                return literal;
            }

            function isKeyOnly()
            {
                return state == 'key';
            }

            function isSpOnly()
            {
                return state == 'securityProfile';
            }

            function isFCOnly()
            {
                return state == 'fileConf';
            }

            function setContext(context)
            {
                editorScope.chosenContext = context;
            }

            function getContext()
            {
                return editorScope.chosenContext;
            }

            function setKeys(keys)
            {
                editorScope.keys = keys;
            }

            function getKeys()
            {
                return editorScope.keys;
            }

            function setContextOptions(contextFilter)
            {
                for (depth in contextFilter)
                {
                    if (!contextFilter[depth])
                        continue;

                    editorScope.chosenContext[depth] = [contextFilter[depth]];
                }
            }

            function initialize($scope, isEditor, date, ctx)
            {
                if (isEditor)
                    editorScope = $scope;

                if (state == 'editor' || 'fileConf' == state) {
                    o = {};
                    if (ctx)
                    {
                        c = ctx.split(";");
                        for (t in c)
                        {
                            sp = c[t].split(":");
                            if (!sp) continue;
                            i = parseInt(sp[0]);
                            o[i] = [];
                            els = sp[1].split(",");
                            for (el in els)
                                o[i].push(els[el])
                        }
                    }
                    else
                    {
                        t = store.get($scope.ctxStoreName);
                        if (t) {
                            ctxParsed = JSON.parse(t);
                            for (i in ctxParsed) {
                                if (ctxParsed[i] && ctxParsed[i].length > 0)
                                    o[i] = ctxParsed[i];
                            }
                        }
                    }

                    $scope.chosenContext = o;
                }
                else if ('key' == state) {
                    $scope.chosenContext = {};
                }
                else if ('contextItem' == state)
                {
                    d = {};
                    d[$scope.ci.placement] = [];
                    d[$scope.ci.placement].push($scope.ci.name);
                    for (i in $scope.ci.assignments)
                        d[$scope.ci.placement].push($scope.ci.assignments[i].name);

                    $scope.chosenContext = d;
                }
                else if ('securityProfile' == state) {
                    $scope.chosenContext = {};
                }
            }

        }])

    .service('resolverService', ['$http', '$q', '$stateParams', 'toUtc',
        function ($http, $q, $stateParams, toUtc)
        {
            // Return the public API.
            return ({
                cancel: cancel,
                resolveProperties: resolveProperties,
                resolveEntry: resolveEntry,
                resolveSp: resolveSp,
                resolveEntries: resolveEntries,
                search: search,
                resolveConfigFile: resolveConfigFile
            });

            var httpTimeout,
                request,
                promise;

            function cancel(promise)
            {
                if (promise &&
                    promise._httpTimeout &&
                    promise._httpTimeout.resolve) {
                    promise._httpTimeout.resolve();
                }
            }

            function resolveEntries(key1, key2, allValues, chosenContext, date, account, repoName)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'GET',
                    url: '/rest/mergedKeyValues/' + account + "/" + repoName + '/' + key1 + "," + key2,
                    params: {
                        context: contextParam(chosenContext),
                        includeSiblings: allValues,
                        ts: toUtc.toMS(date)
                    },
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function resolveEntry(key, allValues, keyView, chosenContext, date, tag, sk, literal, account, repoName)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'GET',
                    url: '/rest/keyProperties/' + account + "/" + repoName,
                    params: {
                        key: key,
                        context: contextParam(chosenContext),
                        sk: sk,
                        allValues: allValues,
                        keyView: keyView,
                        ts: toUtc.toMS(date),
                        tag: tag,
                        literal: literal
                    },
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function resolveSp(profile, allKeys, account, repoName)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'GET',
                    url: '/rest/securityProfileAssignments/' + account + "/" + repoName,
                    params: {
                        profile: profile,
                        allKeys: allKeys
                    },
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function resolveConfigFile(keys, chosenContext, account, repoName, date, tag)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'POST',
                    url: '/rest/getFileKeys/' + account + "/" + repoName,
                    params: {
                        keys: keys,
                        context: contextParam(chosenContext),
                        ts: toUtc.toMS(date),
                        tag: tag
                    },
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function resolveProperties(allKeys, chosenContext, date, tag, literal, account, repoName)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'GET',
                    url: '/rest/editorResolver/' + account + "/" + repoName,
                    params: {
                        context: contextParam(chosenContext),
                        ts: toUtc.toMS(date),
                        allKeys: allKeys,
                        tag: tag,
                        literal: literal
                    },
                    timeout: httpTimeout.promise
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function search(date, tag, account, repoName, searchTerm)
            {
                httpTimeout = $q.defer();
                request = $http({
                    method: 'GET',
                    url: '/rest/searchRepo/' + account + "/" + repoName,
                    params: {
                        ts: toUtc.toMS(date),
                        tag: tag,
                        searchTerm: searchTerm
                    },
                    timeout: httpTimeout.promise
                });

                promise = request.then(unwrapResolve);
                promise._httpTimeout = httpTimeout;
                return ( promise );
            }

            function unwrapResolve(response)
            {
                return (response.data);
            }
        }])

    .controller('EditorInitController', ['$rootScope', 'editorInit', function($rootScope, editorInit) {
        editorInit.setState('editor');
        $rootScope.selectedTab = 0;
    }])

    .controller('EditorController',
    [   '$rootScope', '$scope', '$http', '$stateParams', 'store', '$filter', '$timeout',
        'contextService', 'resolverService', 'editorInit', '$state', 'toUtc', 'focus',
        function($rootScope, $scope, $http, $stateParams, store, $filter, $timeout,
                 contextService, resolverService, editorInit, $state, toUtc, focus)
        {
            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;

            $scope.ctxStoreName = 'c_' + $scope.account + "/" + $scope.repoName;
            $scope.tsFormat = tsFormat;
            $scope.loading = false;

            // -------------------------------------------------------------------
            // Date / Tag
            // -------------------------------------------------------------------
            $scope.tagLabel = $scope.selectedTag = $stateParams.tag;
            $scope.hdcStoreName = 'hcd_' + $scope.account + "/" + $scope.repoName;
            $scope.tagStoreName = 'tag_' + $scope.account + "/" + $scope.repoName;

            var d,
                i,
                depthScore,
                label,
                tag,
                ts,
                k,
                index,
                keys,
                ks = [],
                newEntry,
                rpi,
                pi,
                removeEntry,
                depth,
                tmp,
                resetAttempts = 0,
                pst = store.get('pageSize');

            if ($stateParams.ts)
            {
                $scope.date = new Date(JSON.parse($stateParams.ts));
                $scope.timeToSet = $scope.timeLabel = $scope.date;
            }
            else
            {
                d = store.get($scope.hdcStoreName);
                if (d)
                {
                    $scope.date = new Date(JSON.parse(d));
                    $scope.timeToSet = $scope.timeLabel = $scope.date;
                    $state.go('repo.editor', {
                        owner: $scope.account,
                        name: $scope.repoName,
                        tag: '',
                        ts: toUtc.toMS($scope.date)
                    }, {notify: false});
                }
                else
                    $scope.date = null;

                $scope.now = Date.now();
            }

            $scope.setDate = function(date) { $scope.date = date; };
            $scope.setTag = function(tag) {
                $scope.selectedTag = tag;
            };

            $scope.updateUrl = function() {
                $state.go('repo.editor', {
                    owner: $scope.account,
                    name: $scope.repoName,
                    tag: '',
                    ts: toUtc.toMS($scope.date)
                }, {notify: false});
            };
            $scope.postTimeChange = function() {

                $scope.updateUrl();
                initRepoContext();

                if (!(!$scope.localSearch && $scope.searchQuery && $scope.searchQuery.length > 0))
                    resolve();
                else
                    $scope.searchRepo($scope.searchQuery);
            };

            $scope.isLive = function()
            {
                return null == $scope.date && null == $scope.selectedTag;
            };

            $scope.getDate = function() {
                return $scope.date;
            };

            // ----------------------------------------------------------------
            // Context scope selection - start
            // ----------------------------------------------------------------
            editorInit.initialize($scope, true, $scope.date, $stateParams.ctx);

            // ----------------------------------------------------------------
            // Searching
            // ----------------------------------------------------------------
            $scope.localSearch = false;
            $scope.setRepoSearchMode = function(local, searchTerm)
            {
                if (searchTerm)
                    $scope.allKeys = false;

                if ($scope.localSearch === local)
                {
                    focus('searchTerm');
                    return;
                }

                $scope.localSearch = local;

                if (searchTerm && searchTerm.length > 0)
                {
                    if ($scope.localSearch)
                        resolve();
                    else
                        $scope.searchRepo(searchTerm);
                }

                focus('searchTerm');
            };

            $scope.searchRepo = function(searchTerm)
            {
                if ($scope.localSearch) return;

                if (!searchTerm || searchTerm.length == 0)
                {
                    resolve(false, true);
                }
                else
                {
                    $scope.loading = true;
                    $scope.lastRequest = resolverService.search(
                        $scope.date,
                        $scope.selectedTag,
                        $scope.account,
                        $scope.repoName,
                        searchTerm);

                    $scope.lastRequest.then(
                        function handlePropertiesResolve(properties)
                        {
                            $scope.config = properties.config;
                            $scope.propertiesLoaded = true;
                            $scope.loading = false;
                        }
                    );
                }
            };


            // --------------------------------
            // Repo context
            // --------------------------------
            $scope.contextSelectConfig = contextSelectConfig;

            $scope.repoContext = {
                loaded: false
            };

            $scope.keySorters = [
                {lbl: 'Context relevance', asc: true, srt: 'score' },
                {lbl: 'Value', asc: false, srt: 'value' },
                {lbl: '' }
            ];

            function initRepoContext() {
                $scope.repoContext.loaded = false;

                contextService
                    .contextElements($scope.date, $scope.selectedTag, $scope.account, $scope.repoName)
                    .then(function(ctx)
                    {
                        $scope.repoContext = ctx;
                        for (i in $scope.repoContext.depthScores)
                        {
                            depthScore = $scope.repoContext.depthScores[i];
                            label = $scope.repoContext.depths[depthScore].label;
                            $scope.keySorters.push({ lbl : label, asc: false,  srt: 'levels[' + i + '].n' });
                        }

                        if ($scope.selectedTag)
                        {
                            i = indexOf($scope.repoContext.tags, 'name', $scope.selectedTag);
                            if (i < 0)
                                $scope.selectedTag = '';
                            else
                            {
                                tag = $scope.repoContext.tags[i];
                                ts = tag.ts;
                                $scope.timeLabel = $scope.timeToSet = new Date(tag.ts);
                                $scope.date = new Date(JSON.parse(ts));
                            }
                        }

                        $scope.repoContext.loaded = true;
                        if (editorInit.isFCOnly())
                            setFullContext();
                    });
            }

            $scope.getRepoContext = function() {
                return $scope.repoContext;
            };

            $scope.allKeys = false;
            $scope.allKeyToggle = function() {
                $scope.allKeys = !$scope.allKeys;
                if ($scope.allKeys)
                    $scope.searchQuery = '';
                resolve();
            };

            $scope.lineupContext = false;
            $scope.toggleContextLineup = function ()
            {
                $scope.lineupContext = !$scope.lineupContext;
            };

            $scope.propertiesLoaded = false;
            $scope.lastRequest;

            $rootScope.removeKeys = function(keys)
            {
                for (i in keys) {
                    k = keys[i].replace(/\s/g, '');
                    index = indexOfEntry($scope.config, k);
                    if (-1 != index)
                        $scope.config.splice(index, 1);
                }
            };


            /**
             * User has changed context element
             */
            $scope.$watch('chosenContext', function(newVal, oldVal)
            {
                if (!editorInit.isLiteral() && $scope.repoContext.loaded)
                {
                    if ($scope.disabledContextSelector)
                    {
                        d = store.get($scope.ctxStoreName);
                        if (d) tmp = JSON.parse(d);
                        else   tmp = {};

                        for (i in $scope.repoContext.depthScores)
                        {
                            depth = $scope.repoContext.depthScores[i];
                            if (!$scope.disabledContextSelector[depth])
                                tmp[depth] = $scope.chosenContext[depth];
                        }

                        store.set($scope.ctxStoreName, JSON.stringify(tmp));
                    }
                    else
                        store.set($scope.ctxStoreName, JSON.stringify($scope.chosenContext));
                }


                $scope.now = Date.now();
                if (!(!$scope.localSearch && $scope.searchQuery && $scope.searchQuery.length > 0))
                {
                    resolve(true);
                }

                if (editorInit.isFCOnly() && $scope.repoContext.loaded)
                    setFullContext();

            }, true);

            function setFullContext()
            {
                tmp = true;
                for (i in $scope.repoContext.depthScores)
                {
                    if (!$scope.chosenContext[$scope.repoContext.depthScores[i]]
                        || $scope.chosenContext[$scope.repoContext.depthScores[i]].length != 1)
                    {
                        tmp = false;
                        break;
                    }
                }
                $scope.fullContext = tmp;
            }

            $scope.$on('fileChanged', function(e) {
                resolve(false);
            });


            // ---------------------------------------------------------------------------
            // Resolve
            // ---------------------------------------------------------------------------
            function resolve(perContextChange, reset)
            {
                resolverService.cancel($scope.lastRequest);

                if (editorInit.isKeyOnly())
                {
                    $scope.propertiesLoaded = false;
                    $scope.lastRequest = resolverService.resolveEntry($stateParams.key,
                                                                      true,
                                                                      editorInit.isKeyOnly(),
                                                                      {},
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      true,
                                                                      $scope.account,
                                                                      $scope.repoName);

                    $scope.lastRequest.then(
                        function handlePropertiesResolve(entry)
                        {
                            entry.singleKey = true;
                            $scope.config = [];
                            $scope.config.push(entry);
                            $scope.initialized = true;
                            $scope.propertiesLoaded = true;
                        }
                    );
                }
                else if (editorInit.isSpOnly())
                {
                    $scope.propertiesLoaded = false;
                    $scope.lastRequest = resolverService.resolveSp($stateParams.profile, $scope.allKeys, $scope.account, $scope.repoName);

                    $scope.lastRequest.then(
                        function handlePropertiesResolve(properties)
                        {
                            $scope.config = properties.config;
                            $scope.propertiesLoaded = true;
                        }
                    );
                }
                else if (editorInit.isFCOnly())
                {
                    if (!$scope.localSearch && $scope.searchQuery && $scope.searchQuery.length > 0)
                    {
                        $scope.searchRepo($scope.searchQuery);
                        return;
                    }

                    if (!$scope.initialized)
                    {
                        $scope.propertiesLoaded = true;
                        return;
                    }

                    if (reset)
                    {
                        $scope.config = [];
                        ks = $scope.$parent.getCurrentTokens();
                        keys = ks.join();
                    }
                    else if (perContextChange)
                    {
                        ks = [];

                        if ($scope.config)
                            for (i in $scope.config) ks.push($scope.config[i].key);
                        keys = ks.join();
                    }
                    else
                    {
                        keys = editorInit.getKeys();
                    }

                    if (perContextChange)
                        $scope.propertiesLoaded = false;

                    $scope.lastRequest = resolverService.resolveConfigFile(
                        keys,
                        $scope.chosenContext,
                        $scope.account,
                        $scope.repoName,
                        $scope.date,
                        $scope.selectedTag
                    );

                    $scope.lastRequest.then(
                        function handlePropertiesResolve(response)
                        {
                            if (!response.success)
                            {
                                if (resetAttempts == 0 && response.resetContext)
                                {
                                    resetAttempts = 1;
                                    $scope.chosenContext = {};
                                    store.set($scope.ctxStoreName, JSON.stringify($scope.chosenContext));
                                    resolve(perContextChange, reset);
                                }
                                else
                                {
                                    $scope.error = response.message;
                                }
                            }
                            else
                            {
                                $scope.canManageContext = response.canManageContext;

                                if (perContextChange)
                                {
                                    $scope.config = response.config;
                                    $scope.propertiesLoaded = true;
                                }
                                else
                                {
                                    if (!$scope.config)
                                        $scope.config = [];

                                    for (i in response.config)
                                    {
                                        newEntry = response.config[i];
                                        index = indexOfEntry($scope.config, newEntry.key);
                                        if (-1 != index)
                                        {
                                            $scope.config.splice(index, 1);
                                        }

                                        newEntry.f = { k: { 0: {}, 1: {}, 2: {} } };
                                        $scope.config.push(newEntry);
                                    }
                                }
                            }

                        }
                    );
                }
                else
                {
                    $scope.loading = true;
                    $scope.lastRequest = resolverService.resolveProperties($scope.allKeys,
                        $scope.chosenContext,
                        $scope.date,
                        $scope.selectedTag,
                        editorInit.isLiteral(),
                        $scope.account,
                        $scope.repoName);

                    $scope.lastRequest.then(
                        function handlePropertiesResolve(response)
                        {
                            if (response.error)
                            {
                                if (resetAttempts == 0 && response.resetContext)
                                {
                                    resetAttempts = 1;
                                    $scope.chosenContext = {};
                                    store.set($scope.ctxStoreName, JSON.stringify($scope.chosenContext));
                                    resolve(perContextChange, reset);
                                }
                                else
                                {
                                    $scope.error = response.error;
                                }
                            }
                            else
                            {
                                $scope.canManageContext = response.canManageContext;
                                $scope.config = response.config;
                                resetAttempts = 0;
                            }
                            $scope.propertiesLoaded = true;
                            $scope.loading = false;
                        }
                    );
                }
                return ($scope.lastRequest);
            }


            /**
             * API
             *
             * @param key1
             * @param key2
             * @param allValues
             * @param entry
             */
            $scope.resolveEntries = function(key1, key2, allValues, entry)
            {
                resolverService.cancel(lastRequest);
                lastRequest = resolverService.resolveEntries(key1, key2, allValues, $scope.chosenContext, $scope.date,
                                                             $scope.account, $scope.repoName);

                lastRequest.then(
                    function handlePropertiesResolve(newEntry)
                    {
                        for (rpi in newEntry.properties)
                        {
                            pi = indexOfProperty(entry.properties, newEntry.properties[rpi]);
                            if (pi >= 0)
                            {
                                if (entry.properties[pi].isEdited)
                                {
                                    entry.properties[pi].type = newEntry.properties[rpi].type;
                                    entry.properties[pi].score = newEntry.properties[rpi].score;
                                    entry.properties[pi].attr = newEntry.properties[rpi].attr;
                                    newEntry.properties[rpi] = entry.properties[pi];
                                }
                            }
                        }

                        entry.properties = newEntry.properties;
                        entry.allValues = allValues;
                    }
                );
            };

            /**
             * API
             *
             * @param key
             * @param entry
             * @param allValues
             */
            $scope.getAllValuesForDetachedEntry = function(key, entry, allValues)
            {
                if (!allValues)
                {
                    i = entry.properties.length;
                    while (i--)
                    {
                        if (!entry.properties[i].stickyForm)
                            entry.properties.splice(i, 1);
                    }

                    entry.allValues = allValues;
                    return;
                }

                resolverService.cancel($scope.lastRequest);
                $scope.lastRequest = resolverService.resolveEntry(key,
                                                                  true,
                                                                  editorInit.isKeyOnly(),
                                                                  $scope.chosenContext,
                                                                  $scope.date,
                                                                  $scope.selectedTag,
                                                                  null,
                                                                  editorInit.isLiteral(),
                                                                  $scope.account,
                                                                  $scope.repoName);

                $scope.lastRequest.then(
                    function handlePropertiesResolve(newEntry)
                    {
                        entry.uses = newEntry.uses;

                        for (rpi in newEntry.properties)
                        {
                            pi = indexOfProperty(entry.properties, newEntry.properties[rpi]);
                            if (pi >= 0)
                            {
                                if (entry.properties[pi].isEdited)
                                {
                                    entry.properties[pi].type = newEntry.properties[rpi].type;
                                    entry.properties[pi].score = newEntry.properties[rpi].score;
                                    entry.properties[pi].attr = newEntry.properties[rpi].attr;
                                    newEntry.properties[rpi] = entry.properties[pi];
                                }
                            }
                        }

                        i = entry.properties.length;
                        while (i--)
                        {
                            if (!entry.properties[i].stickyForm)
                                entry.properties.splice(i, 1);
                        }

                        if (!newEntry.key) return;
                        for (i in newEntry.properties)
                            entry.properties.push(newEntry.properties[i]);

                        entry.allValues = allValues;
                    }
                );
            };

            /**
             * API
             *
             * Refresh specific entry
             * @param entry
             */
            $scope.resolveEntry = function(key, allValues, encryptSecret, callback)
            {
                $scope.lastRequest = resolverService.resolveEntry(key,
                    allValues || (!$scope.localSearch && $scope.searchQuery && $scope.searchQuery.length > 0),
                                                                  editorInit.isKeyOnly(),
                                                                  $scope.chosenContext,
                                                                  $scope.date,
                                                                  $scope.selectedTag,
                                                                  encryptSecret,
                                                                  editorInit.isLiteral(),
                                                                  $scope.account,
                                                                  $scope.repoName);

                $scope.lastRequest.then(
                    function handlePropertiesResolve(newEntry)
                    {
                        $timeout(function () {
                            removeEntry = newEntry.key &&
                                              newEntry.properties.length > 0 ? false : !$scope.allKeys;

                            if (newEntry.no_key)
                                removeEntry = true;

                            if (removeEntry && editorInit.isSpOnly())
                                removeEntry = newEntry[1].spName != $stateParams.profile;

                            if (removeEntry && editorInit.isFCOnly())
                            {
                                index = indexOfEntry($scope.config, key);
                                if (index > -1)
                                {
                                    $scope.config[index].properties = [];
                                    $scope.config[index][1].uses = newEntry.no_key ? 0 : newEntry[1].uses;
                                    if (!newEntry.no_key) {
                                        $scope.config[index][1].readme = newEntry[1].readme;
                                        $scope.config[index][1].deprecated = newEntry[1].deprecated;
                                        $scope.config[index][1].spName = newEntry[1].spName;
                                        $scope.config[index][1].spCipher = newEntry[1].spCipher;
                                        $scope.config[index][1].encrypted = newEntry[1].encrypted;
                                        $scope.config[index][1].vdt = newEntry[1].vdt;
                                        $scope.config[index][1].pushEnabled = newEntry[1].pushEnabled;
                                    }
                                    else
                                        $scope.config.splice(index, 1);
                                }
                                else {
                                    $scope.config.push(newEntry);
                                }

                                return;
                            }

                            if (removeEntry)
                            {
                                if (!key)
                                    return;

                                index = indexOfEntry($scope.config, key);
                                if (index > -1)
                                    $scope.config.splice(index, 1);

                                return;
                            }

                            index = indexOfEntry($scope.config, newEntry.key);
                            if (index < 0)
                            {
                                newEntry.f = { k: { 0: {}, 1: {}, 2: {} } };
                                $scope.config.push(newEntry);
                                return;
                            }

                            $scope.config[index][1].uses = newEntry[1].uses;
                            $scope.config[index][1].readme = newEntry[1].readme;
                            $scope.config[index][1].deprecated = newEntry[1].deprecated;
                            $scope.config[index][1].spName = newEntry[1].spName;
                            $scope.config[index][1].spCipher = newEntry[1].spCipher;
                            $scope.config[index][1].encrypted = newEntry[1].encrypted;
                            $scope.config[index][1].vdt = newEntry[1].vdt;
                            $scope.config[index][1].pushEnabled = newEntry[1].pushEnabled;

                            for (rpi in newEntry.properties)
                            {
                                pi = indexOfProperty($scope.config[index].properties, newEntry.properties[rpi]);
                                if (pi >= 0)
                                {
                                    if ($scope.config[index].properties[pi].isEdited)
                                    {
                                        $scope.config[index].properties[pi].type = newEntry.properties[rpi].type;
                                        $scope.config[index].properties[pi].score = newEntry.properties[rpi].score;
                                        $scope.config[index].properties[pi].attr = newEntry.properties[rpi].attr;
                                        newEntry.properties[rpi] = $scope.config[index].properties[pi];
                                    }
                                }
                            }

                            $scope.config[index].properties = newEntry.properties;
                            $scope.config[index].allValues = allValues;

                            if (callback) callback();
                        }, slideTime + 20);
                    }
                );
            };

            /**
             * API
             *
             * Value is editable iff repository is Live and use has access to the value
             * @param value
             * @returns {*|boolean}
             */
            $scope.isValueEditable = function (value)
            {
                if ($scope.ut == $scope.type.demo) return true;
                if ($scope.ut < $scope.type.demo) return false;
                return value.editable && $scope.date == null && null == $scope.selectedTag;
            };

            /**
             * API
             *
             * @param key
             * @param allValues
             */
            $scope.postValueModification = function(key, allValues, callback) {
                $scope.resolveEntry(key, allValues, null, callback);
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
            $scope.pageSizeUpdate = function()
            {
                store.set('pageSize', $scope.pageSizes.selectedOption.name );
            };

            $scope.reverse = false;
            $scope.toggleReverse = function() { $scope.reverse = !$scope.reverse; };

            // --------------------------------
            // New property
            // --------------------------------
            $scope.enableNewPropertyForm = false;
            $scope.showNewPropertyForm = function() { $scope.enableNewPropertyForm = true; };
            $scope.hideNewPropertyForm = function() { $scope.enableNewPropertyForm = false; };

            // --------------------------------
            // Initialization
            // --------------------------------
            initRepoContext();

            // When the scope is destroyed, we want to clean up the controller,
            // including canceling any outstanding request tociphers gather friends.
            $scope.$on(
                "$destroy",
                function handleDestroyEvent()
                {
                    resolverService.cancel($scope.lastRequest);
                    $scope.propertiesLoaded = false;
                }
            );

            $scope.compare = function() {
                $state.go('repo.compare', {
                    owner: $scope.account,
                    name: $scope.repoName
                });
            };

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
                url: "/rest/getSecurityProfiles/" + $scope.account + "/" + $scope.repoName
            }).then(function successCallback(response) {
                $scope.encryptionProfiles = response.data.groups;
                $scope.ciphers = response.data.ciphers;

                if (!$scope.encryptionProfiles || $scope.encryptionProfiles.length == 0)
                    $scope.newEncriptionProfile = true;
            });

        }])

    ;