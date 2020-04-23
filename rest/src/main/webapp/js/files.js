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

{
    angular
        .module('fileEditor', [ 'ui.ace', 'ngFileUpload' ])

        .controller('FilesController', [
            '$http', '$scope', '$stateParams', '$httpParamSerializer', '$rootScope', '$window', 'focus',
            '$state', 'store', 'toUtc', 'Upload', 'secretService', 'contextService', 'editorInit',
            function($http, $scope, $stateParams, $httpParamSerializer, $rootScope, $window, focus,
                     $state, store, toUtc, Upload, secretService, contextService, editorInit)
            {

                $scope.repoName = $stateParams.name;
                $scope.account = $stateParams.owner;
                $scope.searchResult = false;
                $scope.ctxStoreName = 'c_' + $scope.account + "/" + $scope.repoName;
                $rootScope.selectedTab = 0;
                $scope.propContextSelectConfig = propContextSelectConfig;

                var d,
                    fileName,
                    i,
                    uploads,
                    c, j,
                    file,
                    pst = store.get('pageSize'),
                    f,
                    depthScore,
                    hasSecurityProfiles = false,
                    resetAttempts = 0,
                    roots = [], all = {}, folder, parent, p,
                    addComment,
                    storePath = $scope.account + "_" + $scope.repoName + '_hiddenDirs';

                $scope.importInto = function(dir) {
                    $scope.toggleImport();
                    $scope.prefix = dir;
                };

                $scope.allFiles = false;
                $scope.allFilesToggle = function() {
                    $scope.allFiles = !$scope.allFiles;
                    getFiles($scope.searchQuery);
                };

                $scope.enableDirEditor = function(directory) {
                    directory.isEdited = true;
                    directory.renaming = true;
                };

                $scope.moveContent = function(directory) {
                    directory.isEdited = true;
                    directory.moveTo = true;
                };

                $scope.gotoCi = function (ci)
                {
                    $state.go('repo.contextItem',
                        {
                            owner: $scope.account,
                            name: $scope.repoName,
                            depthLabel: $scope.repoContext.depths[ci.p].label,
                            contextItem: ci.n
                        });
                };

                // ----------------------------------------------------------------
                // Searching
                // ----------------------------------------------------------------
                $scope.searchResolved = false;
                $scope.setRepoSearchMode = function(searchResolved, searchTerm)
                {
                    if ($scope.searchResolved === searchResolved)
                    {
                        focus('searchTerm');
                        return;
                    }

                    $scope.searchResolved = searchResolved;

                    if (searchTerm && searchTerm.length > 0)
                        getFiles(searchTerm);

                    focus('searchTerm');
                };

                $scope.localSearch = false;
                $scope.searchQuery = '';
                $scope.searchFiles = function (searchQuery)
                {
                    if (!$scope.initialized) return;
                    getFiles(searchQuery);
                };



                $scope.getDirectories = function(val) {
                    return $http.get('/rest/directorySearch/' + $scope.account + "/" + $scope.repoName, {
                        params: { t: val }
                    }).then(function(response){
                        return response.data;
                    });
                };


                $scope.initialized = false;
                $scope.reverse = false;
                $scope.showImport = false;
                $scope.toggleImport = function()
                {
                    $scope.showNewDirForm = false;
                    $scope.prefix = ($scope.currentPath && $scope.currentPath.length > 0)
                        ? $scope.currentPath.slice(0, $scope.currentPath.length+1).join("/") + '/'
                        : '';
                    $scope.showImport = !$scope.showImport;
                };

                $scope.encryptionProfiles = [];
                $scope.SGConfig = SGConfig;
                $scope.CipherConfig = CipherConfig;
                $scope.ciphers = [];
                $scope.newEncriptionProfile = false;

                $http({
                    method: 'GET',
                    url: "/rest/getSecurityProfiles/" + $stateParams.owner + "/" + $stateParams.name
                }).then(function successCallback(response) {
                    hasSecurityProfiles = true;
                    $scope.encryptionProfiles = response.groups;
                    $scope.ciphers = response.ciphers;

                    if (!$scope.encryptionProfiles || $scope.encryptionProfiles.length == 0)
                        $scope.newEncriptionProfile = true;
                });

                $scope.cancelImport = function() {
                    $scope.showImport = false;
                    $scope.status = {};
                    $scope.prefix = '';
                    $scope.securityGroup = '';
                    $scope.selectedFiles = [];
                };




                // -------------------------------------------------------------------
                // Date / Tag
                // -------------------------------------------------------------------
                $scope.tagLabel = $scope.selectedTag = $stateParams.tag;
                $scope.hdcStoreName = 'hcd_' + $scope.account + "/" + $scope.repoName;
                $scope.tagStoreName = 'tag_' + $scope.account + "/" + $scope.repoName;

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
                        $state.go('repo.files', {
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
                $scope.setTag = function(tag) { $scope.selectedTag = tag; };

                $scope.updateUrl = function() {
                    $state.go('repo.files',
                        {   owner: $scope.account,
                            name: $scope.repoName,
                            tag: $scope.selectedTag,
                            ts: toUtc.toMS($scope.date),
                            path: $scope.path
                        }, {notify: false});
                };
                $scope.postTimeChange = function() {
                    getFiles();
                    $scope.updateUrl();
                };

                // --------------------------------
                // Repo context
                // --------------------------------

                editorInit.setState('editor');
                editorInit.initialize($scope, false, $scope.date, $stateParams.ctx);

                $scope.contextSelectConfig = contextSelectConfig;

                $scope.repoContext = {
                    loaded: false
                };

                function initRepoContext() {
                    $scope.repoContext.loaded = false;

                    contextService
                        .contextElements($scope.date, $scope.selectedTag, $scope.account, $scope.repoName)
                        .then(function(ctx)
                        {
                            $scope.repoContext = ctx;
                            for (i in $scope.repoContext.depthScores)
                                depthScore = $scope.repoContext.depthScores[i];

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
                        });
                }

                $scope.getRepoContext = function() {
                    return $scope.repoContext;
                };

                // --------------------------------
                // Initialization
                // --------------------------------
                initRepoContext();

                $scope.refresh = function() {
                    getFiles();
                };

                // -------------------------------------------------------------------

                function toHierarchy(flat)
                {
                    if (!flat) return;
                    roots = [];
                    all = {};

                    flat.forEach(function(folder)
                    {
                        if (!folder.path)
                            $scope.rootFiles = folder.files;
                        else
                        {
                            folder.hidden = $scope.hidden.hasOwnProperty(folder.path);
                            all[folder.path] = folder;
                        }
                    });

                    Object.keys(all).forEach(function(path)
                    {
                        folder = all[path];
                        d = folder.path.lastIndexOf('/');
                        parent = -1 == d ? '' : folder.path.substring(0, d);

                        if (parent === "") {
                            roots.push(folder)
                        } else if (parent in all) {
                            p = all[parent];
                            if (!('subs' in p)) {
                                p.subs = [];
                            }
                            p.subs.push(folder);
                        }
                    });
                    return roots;
                }


                d = store.get(storePath);
                $scope.hidden = {};
                if (d) { $scope.hidden = JSON.parse(d); }

                function getFiles(searchQuery)
                {
                    $scope.rootFiles = [];
                    $scope.initialized = false;
                    $scope.directories = [];

                    $http({
                        method: 'GET',
                        url: "/rest/getRepoFiles/" + $scope.account + "/" + $scope.repoName,
                        params: {
                            context: contextParam($scope.chosenContext),
                            all: $scope.allFiles,
                            ts: toUtc.toMS($scope.date),
                            tag: $scope.selectedTag,
                            searchTerm: searchQuery,
                            searchResolved: $scope.searchResolved
                        }
                    }).then(function successCallback(response) {

                        if (response.data.success)
                            $scope.directories = toHierarchy(response.data.data);
                        else
                        {
                            if (resetAttempts == 0 && response.data.resetContext)
                            {
                                resetAttempts = 1;
                                $scope.chosenContext = {};
                                store.set($scope.ctxStoreName, JSON.stringify($scope.chosenContext));
                                getFiles();
                            }
                            else
                            {
                                $scope.message = response.data.message;
                            }

                        }

                        $scope.initialized = true;
                    });

                }

                /**
                 * User has changed context element
                 */
                $scope.$watch('chosenContext', function(newVal, oldVal)
                {
                    store.set($scope.ctxStoreName, JSON.stringify($scope.chosenContext));
                    getFiles();
                }, true);


                $scope.toggleDir = function(directory)
                {
                    directory.hidden = !directory.hidden;
                    if (!directory.hidden)
                        delete $scope.hidden[directory.path];
                    else
                        $scope.hidden[directory.path] = true;

                    store.set(storePath, JSON.stringify($scope.hidden));
                };


                $scope.newFile = function(path) {
                    $state.go('repo.file', {
                        owner: $scope.account,
                        name: $scope.repoName,
                        id: '0',
                        path: path
                    });
                };

                $scope.getFile = function(file) {
                    $window.getSelection().removeAllRanges();
                    $state.go('repo.file', {
                        owner: $scope.account,
                        name: $scope.repoName,
                        id: file.id,
                        fullPath: file.fullPath,
                        ts: toUtc.toMS($scope.date),
                        sp: file.sp
                    });
                };

                $scope.gotoSp = function(spName)
                {
                    $state.go('repo.security-profiles', {owner: $scope.account, name: $scope.repoName, profile: spName });
                };



                // --------------------------------
                // Upload
                // --------------------------------
                $scope.status = {};
                $scope.prefix = '';
                $scope.securityGroup = '';

                $scope.uploadFiles = function (files, context)
                {
                    addComment = angular.element(document.querySelector('#changeCommentField')).length > 0;

                    $scope.context = context;
                    $scope.message = '';
                    $scope.status = {};

                    if ($scope.securityGroup)
                    {
                        secretService
                            .authAndExec($scope,
                                null,
                                $scope.securityGroup,
                                function () {
                                    processFilesUpload(files, secretService.get($scope.securityGroup));
                                });
                    }
                    else
                        processFilesUpload(files);
                };

                function processFilesUpload(files, password)
                {
                    for (i in files)
                    {
                        fileName = files[i].name;
                        $scope.status[fileName] = {
                            percent: 0,
                            message: ''
                        };
                    }

                    f = $scope.prefix;
                    uploads = [];
                    j = 0;

                    upload(j, files, password);
                }


                function upload(pos, files, password)
                {
                    file = files[pos];
                    uploads[pos] = Upload.upload({
                        url: "/rest/uploadFiles/" + $scope.account + "/" + $scope.repoName,
                        data: { file: file },
                        headers: {
                            fileName: file.name,
                            path: $scope.prefix,
                            password: password,
                            context: contextParam($scope.context),
                            changeComment: addComment ? $scope.changeComment : '',
                            sg: $scope.securityGroup
                        }
                    });

                    uploads[pos].then(function (response)
                    {
                        if (!response.data)
                        {
                            $scope.status[response.config.data.file.name].message = 'Upload failed.  File size limit 2MB.';
                        }
                        else if (response.data.success) { }
                        else
                        {
                            $scope.status[response.config.data.file.name].message = response.data.message;
                        }

                    }, function (resp) {}, function (evt) {
                        $scope.status[evt.config.data.file.name].percent = parseInt(100.0 * evt.loaded / evt.total);
                    });

                    uploads[pos]['finally'](
                        function ()
                        {
                            pos++;
                            if (pos < files.length)
                                upload(pos, files, password)
                            else
                            {
                                c = 0;
                                $scope.uploading = false;
                                angular.forEach($scope.status, function (value, key)
                                {
                                    if ($scope.status[key].message) {
                                        if (c > 0) $scope.message += '<br>';
                                        $scope.message += '<b>' + key + '</b>: ' + $scope.status[key].message;
                                        c++;
                                    }
                                });

                                $scope.status = {};
                                $scope.cancelImport();

                                $scope.path = f;
                                getFiles();
                            }
                        });

                }

                $scope.isUploading = function() {
                    return Upload.isUploadInProgress();
                };

                $scope.toggleReverse = function() { $scope.reverse = !$scope.reverse; };

                $scope.lineupContext = false;
                $scope.toggleContextLineup = function ()
                {
                    $scope.lineupContext = !$scope.lineupContext;
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

        .directive('directoryEditor', function() {
            return {
                restrict: "A",
                templateUrl: 'repo/files/directoryForm.tpl.html',
                scope: true,
                controller: ['$rootScope', '$scope', '$http', 'focus', '$httpParamSerializer',
                    function($rootScope, $scope, $http, focus, $httpParamSerializer)
                    {
                        var form, i, newPath;

                        $scope.errorMessage = '';

                        $scope.directory.f = {
                            name: $scope.directory.name,
                        };

                        $scope.disableDirectoryEditor = function() {
                            $scope.directory.isEdited = false;
                            $scope.directory.renaming = false;
                        };

                        $scope.deleteDirectory = function()
                        {
                            console.log("++++ calling ****");
                            $scope.message = '';
                            $http({
                                method: 'POST',
                                url: '/rest/deleteDir/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer({path: $scope.directory.path}),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function (response)
                            {
                                console.log(response);
                                if (response.data.success)
                                    $scope.refresh();
                                else
                                    $scope.message = response.data.message;
                            });
                        };

                        $scope.renameDir = function()
                        {
                            i = $scope.directory.path.lastIndexOf("/");
                            if (-1 == i)
                                newPath = $scope.directory.f.name;
                            else
                                newPath = $scope.directory.path.substring(0, i) + '/' + $scope.directory.f.name;

                            form = {
                                oldPath: $scope.directory.path,
                                newPath: newPath
                            };

                            $scope.message = '';
                            $http({
                                method: 'POST',
                                url: '/rest/renameDir/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer(form),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function (response)
                            {
                                if (response.data.success)
                                    $scope.refresh();
                                else
                                    $scope.message = response.data.message;
                            });
                        };
                    }
                ]
            }
        })

        .directive('contentMove', function() {
            return {
                restrict: "A",
                templateUrl: 'repo/files/directoryContentMove.tpl.html',
                scope: true,
                controller: ['$rootScope', '$scope', '$http', '$timeout', '$httpParamSerializer', '$window',
                    function($rootScope, $scope, $http, $timeout, $httpParamSerializer, $window)
                    {
                        $scope.directory.f = {
                            path: $scope.directory.path
                        };

                        $timeout(function() {
                            var element = $window.document.getElementById('moveToFolder');
                            if(element)
                            {
                                element.focus();
                                if ($scope.directory.path)
                                    setCaretPosition(element, $scope.directory.path.length);
                            }
                        }, 100);

                        $scope.getDirectories = function(val) {
                            return $http.get('/rest/directorySearch/' + $scope.account + "/" + $scope.repoName, {
                                params: { t: val }
                            }).then(function(response){
                                return response.data;
                            });
                        };


                        $scope.cancelContentMove = function() {
                            delete $scope.directory.f;
                            $scope.directory.isEdited = false;
                            $scope.directory.moveTo = false;
                        };

                        $scope.moveContent = function()
                        {
                            var form = {
                                oldPath: $scope.directory.path,
                                newPath: $scope.directory.f.path
                            };

                            $scope.errorMessage = '';
                            $http({
                                method: 'POST',
                                url: '/rest/renameDir/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer(form),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function (response)
                            {
                                if (response.data.success)
                                    $scope.refresh();
                                else
                                    $scope.errorMessage = response.data.message;
                            });
                        };
                    }
                ]
            }
        })

        .controller('FileEditorCtrl',
            ['$http', '$scope', '$stateParams', '$httpParamSerializer', 'editorInit', '$timeout', '$rootScope',
                'resolverService', 'toUtc', '$state', 'store', 'secretService', '$modal', '$q',
                function($http, $scope, $stateParams, $httpParamSerializer, editorInit, $timeout, $rootScope,
                         resolverService, toUtc, $state, store, secretService, $modal, $q)
                {

                    $scope.fileEditor = true;

                    $scope.repoName = $stateParams.name;
                    $scope.account = $stateParams.owner;
                    $scope.id = $stateParams.id;
                    $scope.fullPath = $stateParams.fullPath ? $stateParams.fullPath : '';
                    $scope.ts = $stateParams.ts;
                    $scope.spName = $stateParams.sp;
                    $scope.active = true;
                    $scope.context = {};

                    var o = {
                            saved: false,
                            fn: $scope.fullPath ? $scope.fullPath : $stateParams.path,
                            ct: '',
                            sp: ''
                        },
                        b, d, i,
                        score,
                        name,
                        editor,
                        editSession,
                        tag = '',
                        keys = [],
                        staticWordCompleter,
                        position,
                        type,
                        errors,
                        curr,
                        len,
                        row,
                        lineTokens,
                        tokens,
                        token,
                        newKeys,
                        add,
                        rem,
                        newSp,
                        oldSp,
                        _session,
                        tmp,
                        depth,
                        addComment,
                        tokenProcessTimer = 0,
                        deferred, confirm, parentShow, ignoreChanges = false,
                        form;

                    $scope.isDirty = function()
                    {
                        if (!o.saved)
                        {
                            try {
                                _session = editor.getSession();

                                b = o.active == $scope.active &&
                                    o.filename == $scope.filename &&
                                    o.fileContent === _session.getValue() &&
                                    o.sp === $scope.newSp &&
                                    angular.equals(o.filePath, $scope.filePath) &&
                                    angular.equals(o.context, $scope.context);

                                return !b;
                            } catch(e) { return false; }
                        }

                        return false;
                    };

                    $scope.$on('$stateChangeStart', function(event, toState, toParams, from, fromParams)
                    {
                        if (!ignoreChanges && toState.name != 'repo.file' && $scope.isDirty())
                        {
                            event.preventDefault();
                            confirmAbandonChanges(toState, toParams).show();
                        }
                    });

                    $scope.disabledContextSelector = {};
                    $scope.validateContext = function()
                    {
                        for (depth in $scope.context)
                        {
                            if (!$scope.context[depth])
                                delete $scope.disabledContextSelector[depth];
                            else
                                $scope.disabledContextSelector[depth] = true;
                        }

                        editorInit.setContextOptions($scope.context);
                    };

                    $scope.validateFileContext = function()
                    {
                        if (!$scope.active) return;

                        $http({
                            method: 'POST',
                            url: '/rest/contextChange/file/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                context: contextParam($scope.context),
                                path: $scope.filePath.join("/"),
                                name: $scope.filename,
                                fileId: $scope.fileId }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function(response) {

                            if (response.data.success)
                                $scope.conflict = response.data.conflict;
                            else
                                $scope.errorMessage = response.data.message;
                        });
                    };

                    function confirmAbandonChanges(toState, toParams)
                    {
                        confirm = $modal({
                            template: '/repo/files/unsavedChanges.tpl.html',
                            scope: $scope,
                            show: false,
                            controller: [function() {

                                $scope.saveAndExit = function()
                                {
                                    ignoreChanges = true;
                                    $scope.saveFile($scope.filename);
                                };

                                $scope.ignoreChanges = function()
                                {
                                    ignoreChanges = true;
                                    $state.go(toState.name, toParams);
                                };
                            }]
                        });
                        parentShow = confirm.show;

                        confirm.show = function() {
                            deferred = $q.defer();
                            $timeout(function () {
                                parentShow();
                            }, 250);
                            return deferred.promise;
                        };

                        return confirm;
                    }

                    function confirmDeleteLastSibling()
                    {
                        confirm = $modal({
                            template: '/repo/files/lastSiblingFile.tpl.html',
                            scope: $scope,
                            show: false,
                            controller: [function() {

                                $scope.deleteAnyway = function()
                                {
                                    ignoreChanges = true;
                                    $scope.deleteThisFile();
                                };

                                $scope.doNothing = function()
                                {
                                    ignoreChanges = true;
                                };
                            }]
                        });
                        parentShow = confirm.show;

                        confirm.show = function() {
                            deferred = $q.defer();
                            $timeout(function () {
                                parentShow();
                            }, 250);
                            return deferred.promise;
                        };

                        return confirm;
                    }

                    $scope.updateFileName = function(name) {
                        $scope.filename = name;
                        checkContextPostNameChange()
                    };

                    $scope.pendingContextPromise = false;
                    $scope.renamed = false;
                    function checkContextPostNameChange()
                    {
                        if ($scope.fileId)
                            $scope.renamed = $scope.orgAbsFilePath.path != $scope.filePath.join("/") ||
                                             $scope.orgAbsFilePath.name != $scope.filename;

                        if ($scope.pendingContextPromise) { $timeout.cancel($scope.pendingContextPromise); }
                        $scope.pendingContextPromise = $timeout(function () {
                            $scope.validateFileContext();
                        }, 1000);
                    }

                    $scope.cancelFileEditor = function() {
                        ignoreChanges = true;
                        $state.go('repo.files', {owner: $scope.account, name: $scope.repoName });
                    };

                    $scope.isPreview = false;

                    $scope.toggleFilePreview = function() {

                        $scope.filePreview = '';

                        if ($scope.isPreview)
                            $scope.isPreview = false;
                        else
                        {
                            _session = editor.getSession();

                            form = {
                                password: $scope.sp ? secretService.get($scope.sp.name) : '',
                                context: contextParam(editorInit.getContext()),
                                fileContent: _session.getValue(),
                                tag: $scope.selectedTag,
                                ts: toUtc.toMS($scope.date)
                            };

                            tmp = secretService.getAll();
                            for (var i in tmp)
                                form[i] = tmp[i].secret;

                            $http({
                                method: 'POST',
                                url: '/rest/getFilePreview/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer(form),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function (response)
                            {
                                if (response.data.success)
                                {
                                    $scope.filePreview = response.data.content;
                                    $scope.isPreview = true;

                                } else {
                                    $scope.message = response.data.message;
                                    if (response.data.culprit)
                                    {
                                        $scope.message += "<br>";
                                        $scope.message += "Caused by: <span class='key'>" + response.data.culprit.key + "</span> with file value: <span class='value'>" + response.data.culprit.value + "</span>";
                                    }
                                }
                            });

                        }
                    };

                    $scope.filePath = $scope.fullPath
                        ? []
                        : $stateParams.path
                            ? $stateParams.path.split('/') : [];

                    $scope.filename = '';

                    // -------------------------------------------------------------------
                    // Date / Tag
                    // -------------------------------------------------------------------
                    {
                        $scope.tagLabel = $scope.selectedTag = $stateParams.tag;
                        $scope.hdcStoreName = 'hcd_' + $scope.account + "/" + $scope.repoName;
                        $scope.tagStoreName = 'tag_' + $scope.account + "/" + $scope.repoName;

                        if ($stateParams.ts) {
                            $scope.date = new Date(JSON.parse($stateParams.ts));
                            $scope.timeToSet = $scope.timeLabel = $scope.date;
                        }
                        else {
                            d = store.get($scope.hdcStoreName);
                            if (d) {
                                $scope.date = new Date(JSON.parse(d));
                                $scope.timeToSet = $scope.timeLabel = $scope.date;
                                $state.go('repo.file',
                                    {
                                        owner: $scope.account,
                                        name: $scope.repoName,
                                        filename: $scope.filename,
                                        tag: $scope.selectedTag,
                                        ts: toUtc.toMS($scope.date)
                                    }, {notify: false});
                            }
                            else
                                $scope.date = null;

                            $scope.now = Date.now();
                        }

                        $scope.setDate = function (date) { $scope.date = date; };
                        $scope.setTag = function (tag) { $scope.selectedTag = tag; };

                        $scope.updateUrl = function ()
                        {
                            $state.go('repo.file',
                                {
                                    owner: $scope.account,
                                    name: $scope.repoName,
                                    filename: $scope.filename,
                                    tag: $scope.selectedTag,
                                    ts: toUtc.toMS($scope.date)
                                }, {notify: false});
                        };
                        $scope.postTimeChange = function()
                        {
                            loadFile();
                            editorInit.updateTime($scope.date, $scope.selectedTag);
                            $scope.updateUrl();
                        };
                    }
                    // -------------------------------------------------------------------
                    $scope.darkTheme = store.get('darkTheme') === null ? true : store.get("darkTheme");

                    $scope.expanded = false;
                    $scope.toggleExpand = function() { $scope.expanded = !$scope.expanded; };
                    $scope.toggleTheme = function() {

                        if ($scope.darkTheme)
                            editor.setTheme("ace/theme/confighub");
                        else
                            editor.setTheme("ace/theme/confighub-dark");
                        $scope.darkTheme = !$scope.darkTheme;
                        store.set("darkTheme", $scope.darkTheme);
                    };

                    $rootScope.selectedTab = 0;

                    editorInit.setState('fileConf');
                    editorInit.initialize($scope, false, $scope.date, $stateParams.ctx);

                    $scope.initialized = false;
                    $scope.message = '';
                    $scope.fileContent = '';
                    $scope.fileId = '';
                    $scope.newSp = '';
                    $scope.editable = true;

                    $scope.loadAce = false;

                    $http
                        .get('/rest/canUserManageContext/' + $scope.account + "/" + $scope.repoName)
                        .then(function (response)
                        {
                            $scope.propContextSelectConfig = response.data.canManageContext
                                ? propContextSelectConfig
                                : propContextSelectConfigNoEdit;
                        });

                    if ($scope.fullPath)
                        loadFile(secretService.get($scope.spName, $scope.date));
                    else
                        $scope.loadAce = true;

                    $scope.aceLoaded = function(_editor)
                    {
                        editor = _editor;
                        if ($scope.darkTheme)
                            editor.setTheme("ace/theme/confighub-dark");
                        else
                            editor.setTheme("ace/theme/confighub");
                        editSession = editor.getSession();
                        editSession.setUseWrapMode(true);
                        editor.setReadOnly($scope.readOnly);

                        $scope.initialized = true;

                        $http
                            .get("/rest/getRepoKeys/" + $scope.account + "/" + $scope.repoName)
                            .then(function (response)
                            {
                                editor.setOptions({
                                    enableBasicAutocompletion: true
                                });

                                // todo add to key, if new keys are created

                                staticWordCompleter = {
                                    getCompletions: function (e, session, pos, prefix, callback)
                                    {
                                        position = e.getCursorPosition();
                                        type = e.session.getTokenAt(position.row, position.column).type;

                                        callback(null, response.data.map(function (word)
                                        {
                                            if ('key' == type)
                                                return {
                                                    caption: word,
                                                    value: word
                                                };
                                        }));

                                    }
                                };

                                editor.completers = [staticWordCompleter];
                            });
                    };

                    $scope.acePreviewLoaded = function(_editor)
                    {
                        $scope.contextHTML = fullContextToHTML(editorInit.getContext());

                        if ($scope.darkTheme)
                            _editor.setTheme("ace/theme/confighub-dark");
                        else
                            _editor.setTheme("ace/theme/confighub");
                        editSession = _editor.getSession();
                        editSession.setUseWrapMode(true);
                        _editor.setReadOnly(true);

                    };

                    $scope.orgAbsFilePath = {
                        path: '',
                        name: ''
                    };
                    $scope.renameAll = false;
                    $scope.updateRefs = false;

                    function loadFile(password)
                    {
                        if (!$scope.id)
                            return;

                        $http
                            .get("/rest/getFile/" + $scope.account + "/" + $scope.repoName, {
                                params: {
                                    id: $scope.id,
                                    ts: toUtc.toMS($scope.date),
                                    tag: tag,
                                    password: password ? password : ''
                                }
                            })
                            .then(function(response)
                            {
                                if (response.data.success && response.data.id)
                                {
                                    $scope.sp = response.data.sp;
                                    $scope.siblings = response.data.siblings;
                                    $scope.refs = response.data.refs;
                                    $scope.orgAbsFilePath.path = response.data.path;
                                    $scope.orgAbsFilePath.name = response.data.filename;
                                    $scope.filePath = response.data.path ? response.data.path.split('/') : [];
                                    $scope.filename = response.data.filename;
                                    $scope.context = {};
                                    $scope.readOnly = $scope.editable = response.data.editable;
                                    $scope.active = response.data.active;

                                    for (i in response.data.levels)
                                    {
                                        score = response.data.levels[i].p;
                                        name = response.data.levels[i].n ? response.data.levels[i].n : undefined;
                                        $scope.context[score] = name;
                                    }

                                    $scope.validateContext();
                                    $scope.locked = false;
                                    if ($scope.sp)
                                    {
                                        o.sp = $scope.newSp = $scope.sp.name;

                                        if (response.data.unlocked)
                                        {
                                            $scope.readOnly = null != $scope.date;
                                            $scope.loadAce = true;
                                        }
                                        else
                                        {
                                            $scope.locked = true;
                                            $scope.readOnly = true;
                                            $scope.loadAce = !$scope.sp.cipher;
                                        }
                                    }
                                    else
                                    {
                                        $scope.readOnly = null != $scope.date;
                                        $scope.loadAce = true;
                                    }

                                    if (!$scope.readOnly && !$scope.editable)
                                        $scope.readOnly = true;

                                    $scope.fileContent = response.data.content;
                                    $scope.fileId = response.data.id;
                                    $scope.message = '';

                                    o.id = $scope.fileId;
                                    o.active = $scope.active;
                                    o.filePath = angular.copy($scope.filePath);
                                    o.filename = $scope.filename;
                                    o.context = angular.copy($scope.context);
                                    o.fileContent = $scope.fileContent;

                                }
                                else {
                                    $scope.message = response.data.message;
                                    $scope.loadAce = true;
                                    $scope.readOnly = true;
                                    $scope.editable = true;
                                    $scope.fileId = -1;
                                }
                            });
                    }

                    $scope.aceChanged = function()
                    {
                        if ($scope.pendingPromise) { $timeout.cancel($scope.pendingPromise); }
                        $scope.pendingPromise = $timeout(function () {
                            processTokens();
                            tokenProcessTimer = 1500;
                        }, tokenProcessTimer);
                    };

                    $scope.getCurrentTokens = function()
                    {
                        getTokens();
                        if (curr)
                            return Object.keys(curr);
                        return [];
                    };

                    function getTokens()
                    {
                        errors = false;
                        curr = {};
                        len = 0;

                        if (null == editSession) return;
                        for (row=0; row < editSession.getLength(); row++)
                        {
                            lineTokens = [];
                            try {
                                lineTokens = editSession.getTokens(row);
                            } catch (e) { return; }


                            tokens = [];
                            for (i=0 ; i<lineTokens.length ; i++)
                            {
                                token = lineTokens[i];
                                if (token.type === 'key') {
                                    if (lineTokens.length > i+1 && lineTokens[i+1].type === 'closeBracket')
                                    {
                                        tokens.push(token.value.replace(/\s/g, ''));
                                        len++;
                                    }
                                }
                                else if (token.type === 'error')
                                {
                                    tokens.pop();
                                    len--;
                                    errors = true;
                                }
                            }

                            if (tokens)
                                for (i in tokens) curr[tokens[i]] = 1;
                        }

                    }

                    $scope.cloneFile = function() {
                        $scope.fileId = undefined;
                        checkContextPostNameChange();
                    };

                    $scope.hasErrors = false;
                    function processTokens()
                    {
                        if ($scope.isPreview) return;

                        getTokens();

                        newKeys = Object.keys(curr);

                        add = diff(newKeys, keys);
                        if (add.length > 0) {
                            $scope.addKeys(add);
                        }

                        rem = diff(keys, newKeys);
                        if (rem.length > 0) {
                            $rootScope.removeKeys(rem);
                        }
                        $scope.hasErrors = errors;
                        keys = newKeys;
                    }

                    $scope.authFile = function()
                    {
                        secretService
                            .authAndExec($scope,
                                $scope.date,
                                $scope.sp.name,
                                function () {
                                    if ($scope.sp.cipher)
                                    {
                                        loadFile(secretService.get($scope.sp.name, $scope.date));
                                        $scope.readOnly = false;
                                        $scope.loadAce = true;
                                    }
                                    else {
                                        $scope.readOnly = false;
                                        editor.setReadOnly($scope.readOnly);
                                    }
                                    $scope.locked = false;
                                });
                    };

                    $scope.chooseSP = function(lsp)
                    {
                        $scope.newSp = lsp ? lsp.name : '';
                    };


                    $scope.updateActive = function(active) {
                        $scope.active = active;
                        $scope.validateFileContext();
                    };

                    $scope.saveFile = function()
                    {
                        newSp = $scope.newSp;
                        oldSp = $scope.sp ? $scope.sp.name : '';

                        if (!oldSp && !newSp)
                        {
                            saveFile();
                        }
                        else if (oldSp === newSp)
                        {
                            secretService.authAndExec($scope, null, oldSp, saveFile);
                        }
                        else
                        {
                            if (!oldSp)
                                secretService.authAndExec($scope, null, newSp, saveFile);
                            else if (!newSp)
                                secretService.authAndExec($scope, null, oldSp, saveFile);
                            else
                                secretService.authSwitchAndExec($scope, newSp, oldSp, saveFile);
                        }
                    };

                    function saveFile()
                    {
                        _session = editor.getSession();
                        addComment = angular.element(document.querySelector('#changeCommentField')).length > 0;

                        form = {
                            renameAll: $scope.renameAll,
                            updateRefs: $scope.updateRefs,
                            path: $scope.filePath.join("/"),
                            name: $scope.filename,
                            content: _session.getValue(),
                            id: $scope.fileId ? $scope.fileId : -1,
                            currentPassword: $scope.sp ? secretService.get($scope.sp.name) : '',
                            newProfilePassword: secretService.get($scope.newSp),
                            changeComment: addComment ? $scope.changeComment : '',
                            spName: $scope.newSp,
                            active: $scope.active,
                            context: contextParam($scope.context)
                        };

                        $scope.message = '';
                        $http({
                            method: 'POST',
                            url: '/rest/saveConfigFile/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer(form),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                o.saved = true;
                                $state.go('repo.files', {
                                    owner: $scope.account,
                                    name: $scope.repoName
                                });
                            } else {

                                if (response.data.circularRef)
                                {
                                    $scope.circularRef = response.data.circularRef;
                                    $scope.errorMessage = null;
                                } else {
                                    $scope.message = response.data.message;
                                }

                                o.saved = false;
                            }
                        });
                    }

                    $scope.deleteFile = function()
                    {
                        if ($scope.fileId < 0) return;

                        if ($scope.siblings <= 1 && $scope.refs > 0)
                            confirmDeleteLastSibling().show();
                        else
                            $scope.deleteThisFile();
                    };

                    $scope.deleteThisFile = function()
                    {
                        form = {
                            id: $scope.fileId
                        };

                        $scope.message = '';
                        $http({
                            method: 'POST',
                            url: '/rest/deleteConfigFile/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer(form),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            if (response.data.success)
                            {
                                o.saved = true;
                                editSession = null;
                                $state.go('repo.files', {
                                    owner: $scope.account,
                                    name: $scope.repoName
                                });
                            } else {
                                $scope.message = response.data.message;
                            }
                        });
                    };

                    $scope.$on('keyUpdated', function (e, a) {

                        _session = editor.getSession();
                        $http({
                            method: 'POST',
                            url: '/rest/rekeyConfigFileContent',
                            data: $httpParamSerializer({
                                'content': _session.getValue(),
                                'from' :a.from,
                                'to': a.to
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function (response)
                        {
                            editor.setValue(response.data, 1);
                        });
                    });

                    $scope.addKeys = function(keys)
                    {
                        if (keys && keys.length > 0)
                        {
                            editorInit.setKeys(keys.join());
                            $scope.$broadcast('fileChanged');
                        }
                    };

                    $scope.setFilename = function(name) {
                        $scope.filename = name;
                    };

                    // --------------------------------
                    // Security
                    // --------------------------------
                    $scope.encryptionProfiles = [];
                    $scope.SGConfig = SGConfig;
                    $scope.CipherConfig = CipherConfig;
                    $scope.ciphers = [];
                    $scope.newEncriptionProfile = false;

                    $http
                        .get("/rest/getSecurityProfiles/" + $stateParams.owner + "/" + $stateParams.name)
                        .then(function (response)
                        {
                            $scope.encryptionProfiles = response.groups;
                            $scope.ciphers = response.ciphers;

                            if (!$scope.encryptionProfiles || $scope.encryptionProfiles.length == 0)
                                $scope.newEncriptionProfile = true;
                        });
                }
            ])

        .directive('dirNameInput', function() {

            return {
                restrict: 'A',
                scope: true,

                link: function(scope, element, iAttrs) {

                    if (!scope.currPos) scope.currPos = 0;

                    element.on('keydown click', function(event)
                    {
                        scope.$apply(function() { scope.currPos = getPos(element[0]); });

                        // ignore space
                        if (event.which === 32) event.preventDefault();

                        if (event.which === 191)
                        {
                            // first character is /
                            if (scope.currPos == 0)
                                event.preventDefault();
                            else
                                scope.newPath.push(scope.lastDirName.substring(0, scope.currPos));
                        }
                    });

                    element.on('keyup', function(event)
                    {
                        // backspace
                        if (event.which === 8 && scope.currPos == 0)
                        {
                            if (scope.newPath && scope.newPath.length > 0)
                            {
                                var dir = scope.newPath.pop();
                                scope.$parent.lastDirName = dir + scope.lastDirName;

                                element.trigger('click');
                                setCaretPosition(element[0], dir.length);
                                scope.$apply(function() { scope.currPos = dir.length;  });
                            }
                        }

                        // dir separator
                        if (event.which === 191)
                        {
                            if (scope.currPos == 0)
                                event.preventDefault();
                            else
                            {
                                scope.$parent.lastDirName =
                                    scope.lastDirName.substring(scope.currPos + 1, scope.lastDirName.length);

                                element.trigger('click');
                                setCaretPosition(element[0], 0);
                                scope.$apply(function() { scope.currPos = 0;  });
                            }
                        }
                    });
                }
            };
        })

        .directive('fileNameInput', function() {

            return {
                restrict: 'A',
                scope: true,

                link: function(scope, element, iAttrs) {

                    if (!scope.currPos) scope.currPos = 0;

                    element.on('keydown click', function(event)
                    {
                        scope.$apply(function() { scope.currPos = getPos(element[0]); });

                        // ignore space
                        if (event.which === 32) event.preventDefault();

                        if (event.which === 191)
                        {
                            // first character is /
                            if (scope.currPos == 0)
                                event.preventDefault();
                            else
                                scope.filePath.push(scope.filename.substring(0, scope.currPos));
                        }
                    });

                    element.on('keyup', function(event)
                    {
                        // backspace
                        if (event.which === 8 && scope.currPos == 0)
                        {
                            if (scope.filePath && scope.filePath.length > 0)
                            {
                                var dir = scope.filePath.pop();
                                scope.$parent.filename = dir + scope.filename;
                                scope.setFilename(scope.$parent.filename);
                                element.trigger('click');
                                setCaretPosition(element[0], dir.length);
                                scope.$apply(function() { scope.currPos = dir.length;  });
                            }
                        }

                        // dir separator
                        if (event.which === 191)
                        {
                            if (scope.currPos == 0)
                            {
                                event.preventDefault();
                            }
                            else
                            {
                                scope.$parent.filename =
                                    scope.filename.substring(scope.currPos + 1, scope.filename.length);

                                scope.setFilename(scope.$parent.filename);
                                element.trigger('click');
                                setCaretPosition(element[0], 0);
                                scope.$apply(function() { scope.currPos = 0;  });
                            }
                        }
                    });
                }
            };
        })
    ;


    function getPos(element)
    {
        if ('selectionStart' in element) {
            return element.selectionStart;
        } else if (document.selection) {
            element.focus();
            var sel = document.selection.createRange(),
                selLen = document.selection.createRange().text.length;
            sel.moveStart('character', -element.value.length);
            return sel.text.length - selLen;
        }
    }

    function setCaretPosition(elem, caretPos) {
        if (elem !== null) {
            if (elem.createTextRange) {
                var range = elem.createTextRange();
                range.move('character', caretPos);
                range.select();
            } else {
                if (elem.setSelectionRange) {
                    elem.focus();
                    elem.setSelectionRange(caretPos, caretPos);
                } else
                    elem.focus();
            }
        }
    }
}

