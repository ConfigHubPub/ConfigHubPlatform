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
        .module('configHub.repository.entry', [
        ])

        .directive('repoEntry', function ()
        {
            return {
                restrict: "A",
                templateUrl: 'repo/entry.tpl.html',
                scope: true,
                controller: ['$scope', '$rootScope', '$window', '$timeout', '$state', 'secretService',
                    function ($scope, $rootScope, $window, $timeout, $state, secretService)
                    {
                        $scope.hover = false;
                        $scope.entry.allValues = false;
                        $scope.entry.f = { k: { 0: {}, 1: {}, 2: {} } };
                        $scope.valueSorter = $scope.keySorters ? $scope.keySorters[0] : null;
                        $scope.entry.currentPage = 1;
                        $scope.entry.itemsPerPage = 20;
                        $scope.entry.paginateId = "ent" + $scope.entry.id;
                        $scope.entry.currPath = 1;

                        // $scope.maxSize = 4;

                        $scope.setSorter = function(srt) {
                            if ($scope.valueSorter.srt === srt.srt)
                                $scope.valueSorter.asc = !$scope.valueSorter.asc;
                            else
                                $scope.valueSorter = srt;
                        };

                        $scope.gotoCi = function(ci, property) {
                            $state.go('repo.contextItem',
                                {   owner: $scope.account,
                                    name: $scope.repoName,
                                    depthLabel: $scope.getRepoContext(property).depths[ci.p].label,
                                    contextItem: ci.n
                                });
                        };

                        $scope.gotoSp = function(spName)
                        {
                            $state.go('repo.security-profiles', {owner: $scope.account, name: $scope.repoName, profile: spName });
                        };

                        $scope.enableValueEditor = function (value, side)
                        {
                            if (!$scope.isValueEditable(value, side)) return;
                            $window.getSelection().removeAllRanges();
                            value.isEdited = true;
                        };

                        var ll,
                            value,
                            pair,
                            date,
                            conflictIndex = -1,
                            conflictFor = -1,
                            conflictProp,
                            type,
                            ed,
                            valueScopes;

                        $scope.disableValueEditor = function(value, side)
                        {
                            // New property value form
                            if ($scope.entry.newProperty && value.stickyForm)
                            {
                                $scope.hideNewPropertyForm();
                                $scope.resetNewPropertyForm();

                                return;
                            }

                            // Single property value form
                            else if ($scope.entry.standalone && value.stickyForm)
                            {
                                ll = $rootScope.getLastLocation();
                                if (!ll || !ll.name || ll.name === $state.name || !$rootScope.goToLastLocation())
                                    $state.go('repo.editor', {owner: $scope.account, name: $scope.repoName });

                                return;
                            }

                            $scope.hover = false;
                            $timeout(function () {
                                value.isEdited = false;
                            }, 100);

                            if (value.id === conflictFor)
                                clearConflictLabel(side);

                            if (value.isNew)
                            {
                                $timeout(function () {
                                    $scope.entry.properties.shift();
                                }, slideTime + 10);
                            }
                        };

                        $scope.showKeyLinks = function(side)
                        {

                            if ($scope.ut == $scope.type.demo) return true;
                            if ($scope.ut < $scope.type.demo) return false;

                            return $scope.isLive(side);
                        };

                        $scope.enableKeyEditor = function(side)
                        {
                            if ($scope.ut < $scope.type.demo) return false;
                            if ($scope.entry.f.k[side].isEdited) return;
                            if (!$scope.isLive(side)) return;

                            $window.getSelection().removeAllRanges();
                            $scope.entry.f.k[side].isEdited = true;
                        };

                        /** Enable new value form */
                        $scope.addValue = function(side) {

                            if ($scope.entry.properties
                                && $scope.entry.properties.length > 0
                                 && $scope.entry.properties[0].isNew) return;

                            value = {
                                id: -1,
                                isNew: true,
                                value: '',
                                isEdited: false,
                                editable: true,
                                active: true,
                                levels: []
                            };

                            if (1 == side)
                                $scope.entry.properties.unshift(value);
                            else
                            {
                                pair = { isNew: true };
                                pair[side] = value;
                                $scope.entry.properties.unshift(pair);
                            }

                            $timeout(function () {
                                value.isEdited = true;
                            }, slideTime);
                        };

                        $scope.toggleAllValues = function(merge, side)
                        {
                            if ($scope.entry.newProperty)
                                $scope.getAllValuesForDetachedEntry($scope.getKey($scope.side), $scope.entry, !$scope.entry.allValues);
                            else if (merge && !$scope.entry.allValues)
                            {
                                $scope.resolveEntries($scope.entry.key,
                                    $scope.entry.f.k[side].key,
                                    !$scope.entry.allValues,
                                    $scope.entry,
                                    null,
                                    $scope.account,
                                    $scope.repoName);
                            }
                            else if ($scope.entry.allValues &&
                                     (!$scope.localSearch && $scope.searchQuery && $scope.searchQuery.length > 0))
                            {
                                ;
                            }
                            else
                            {
                                $scope.resolveEntry($scope.getKey($scope.side), !$scope.entry.allValues);
                            }
                        };


                        $scope.toggleDecryption = function(side)
                        {
                            if ($scope.entry.f.k[side].decrypted)
                            {
                                $scope.resolveEntry($scope.entry.key, $scope.entry.allValues);
                                $scope.entry.f.k[side].decrypted = false;

                                return;
                            }

                            date = $scope.getDate(side);

                            secretService
                                .authAndExec($scope,
                                             date,
                                             $scope.entry[side].spName,
                                             function () {
                                                $scope.resolveEntry($scope.entry.key,
                                                                    $scope.entry.allValues,
                                                                    secretService.get($scope.entry[side].spName, date));
                                                $scope.entry.f.k[side].decrypted = true;
                                            });
                        };


                        function clearConflictLabel(side)
                        {
                            if (conflictIndex >= 0 && $scope.entry.properties[conflictIndex])
                            {
                                conflictProp = 1 == side
                                    ? $scope.entry.properties[conflictIndex]
                                    : $scope.entry.properties[conflictIndex][side];

                                if (conflictProp.isEdited) return;

                                conflictProp.attn = '';
                                if (!$scope.entry.allValues)
                                {
                                    type = conflictProp.type;
                                    if (type != 'self' &&
                                        type != 'match') // && $scope.entry.newProperty
                                    {
                                        $scope.entry.properties.pop();
                                    }

                                }
                            }

                            conflictIndex = -1;
                            conflictFor = -1;
                        }

                        $scope.toggleConflict = function(property, side, conflictProperty)
                        {
                            clearConflictLabel(side);

                            if (!conflictProperty)
                                return;

                            conflictFor = property.id;

                            switch(side)
                            {
                                case 0:
                                case 2:
                                    conflictIndex = indexOfCmpProperty($scope.entry.properties, conflictProperty, side);
                                    if (conflictIndex >= 0)
                                        $scope.entry.properties[conflictIndex][side].attn = conflictProperty.attn;
                                    else {
                                        conflictProperty.type = 'outOfContext';
                                        pair = { score: conflictProperty.score };
                                        pair[side] = conflictProperty;
                                        conflictIndex = $scope.entry.properties.push(pair) -1 ;
                                    }

                                    break;

                                case 1:
                                    conflictIndex = indexOfProperty($scope.entry.properties, conflictProperty);
                                    if (conflictIndex >= 0)
                                        $scope.entry.properties[conflictIndex].attn = conflictProperty.attn;
                                    else {
                                        conflictProperty.type = 'outOfContext';
                                        conflictIndex = $scope.entry.properties.push(conflictProperty) -1 ;
                                    }

                                    break;
                            }
                        };

                        $scope.getEntryData = function(side)
                        {
                            ed = {};

                            if ($scope.entry.newProperty)
                            {
                                ed.key = $scope.entry.f.k[side].key;
                                ed.spName = $scope.entry.f.k[side].keydata.spName;
                                ed.vdt = $scope.entry.f.k[side].keydata.vdt;
                                ed.pushEnabled = $scope.entry.f.k[side].keydata.pushEnabled;
                                ed.comment = $scope.entry.f.k[side].keydata.readme;
                                ed.deprecated = $scope.entry.f.k[side].keydata.deprecated;
                            }
                            else
                            {
                                ed.key = $scope.entry.key;
                                ed.spName = $scope.entry[side].spName;
                                ed.vdt = $scope.entry[side].vdt;
                                ed.pushEnabled = $scope.entry[side].pushEnabled;
                                ed.comment = $scope.entry[side].comment;
                                ed.deprecated = $scope.entry[side].deprecated;
                            }

                            return ed;
                        };

                        $scope.getKey = function(side) {
                            return $scope.entry.newProperty ? $scope.entry.f.k[side].key : $scope.entry.key;
                        };

                        $scope.getSpName = function(side) {
                            return $scope.entry.newProperty ? $scope.entry.f.k[side].keydata.spName : $scope.entry[side].spName;
                        };

                        $scope.entryUpdatePostValueModification = function(key)
                        {
                            // refresh entry
                            $scope.postValueModification(key, $scope.entry.allValues, 
                                function() {
                                    if ($scope.entry.newProperty)
                                        $scope.getAllValuesForDetachedEntry($scope.entry.key, $scope.entry, $scope.entry.allValues);
                                    else
                                        angular.forEach(valueScopes, function(valueScope) {
                                                valueScope.validateContext();
                                        });
                                });
                        };

                        valueScopes = [];
                        $scope.addValueScope = function(valueScope) {
                            valueScopes.push(valueScope);
                        };
                        $scope.removeValueScope = function(valueScope) {
                            valueScopes.splice(valueScopes.indexOf(valueScope), 1);
                        };
                    }]
            }
        })

        .directive('keyEditor', function() {
            return {
                restrict: "A",
                templateUrl: 'repo/keyForm.tpl.html',
                scope: true,
                controller: ['$rootScope', '$scope', '$http', '$timeout', 'secretService', 'focus', '$httpParamSerializer',
                    function($rootScope, $scope, $http, $timeout, secretService, focus, $httpParamSerializer)
                    {
                        $scope.errorType = '';
                        $scope.errorMessage = '';
                        $scope.merging = null;
                        $scope.invalidKey = keyError;

                        $scope.entry.f.k[$scope.side].key = $scope.entry.key;
                        $scope.entry.f.k[$scope.side].keydata = {};
                        angular.copy($scope.entry[$scope.side], $scope.entry.f.k[$scope.side].keydata);

                        focus('key_' + $scope.side + '_' + ($scope.entry.newProperty ? 'new' : $scope.entry.id));

                        var newSp,
                            oldSp,
                            form,
                            addComment,
                            keyChanged,
                            userSetAttribs = {
                                type: 'Text',
                                sp: '',
                                push: false
                            },
                            spName;

                        //-----------------------------------
                        // Key
                        //-----------------------------------

                        $scope.mergingTag = function() {
                            if ($scope.merging && $scope.merging.merging) return "merged"; return "";
                        };

                        $scope.entry.f.k[$scope.side].keydata.isKeyEditable = true;
                        if (!$scope.entry.newProperty)
                        {
                            $http({
                                method: 'GET',
                                url: '/rest/isKeyEditable/' + $scope.account + "/" + $scope.repoName + "/" + $scope.entry.f.k[$scope.side].key,
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function(response) {
                                $scope.entry.f.k[$scope.side].keydata.isKeyEditable = response.data;
                            });
                        }

                        $scope.disableKeyEditor = function()
                        {
                            if ($scope.entry.newProperty)
                            {
                                $scope.hideNewPropertyForm();
                                $scope.resetNewPropertyForm();

                                return;
                            }

                            if ($scope.merging && $scope.entry.allValues)
                                $scope.toggleAllValues($scope.merging.merging, $scope.side);

                            $scope.entry.f.k[$scope.side] = {};

                        };

                        $scope.saveKey = function()
                        {
                            newSp = $scope.entry.f.k[$scope.side].keydata.spName;
                            oldSp = $scope.entry[$scope.side].spName;

                            addComment = angular.element(document.querySelector('#changeCommentField')).length > 0;

                            if (!oldSp && !newSp)
                            {
                                saveThisKey();
                            }
                            else if (oldSp === newSp)
                            {
                                secretService.authAndExec($scope, null, oldSp, saveThisKey);
                            }
                            else
                            {
                                if (!oldSp)
                                    secretService.authAndExec($scope, null, newSp, saveThisKey);
                                else if (!newSp)
                                    secretService.authAndExec($scope, null, oldSp, saveThisKey);
                                else
                                    secretService.authSwitchAndExec($scope, newSp, oldSp, saveThisKey);
                            }
                        };

                        function saveThisKey()
                        {
                            form = {
                                key: $scope.entry.f.k[$scope.side].key,
                                comment: $scope.entry.f.k[$scope.side].keydata.readme,
                                deprecated: $scope.entry.f.k[$scope.side].keydata.deprecated,
                                pushEnabled: $scope.entry.f.k[$scope.side].keydata.pushEnabled,
                                spName: $scope.entry.f.k[$scope.side].keydata.spName,
                                vdt: $scope.entry.f.k[$scope.side].keydata.vdt,
                                newSpPassword: secretService.get($scope.entry.f.k[$scope.side].keydata.spName),
                                currentPassword: secretService.get($scope.entry[$scope.side].spName),
                                changeComment: addComment ? $scope.changeComment : ''
                            };

                            $scope.errorMessage = '';

                            $http({
                                method: 'POST',
                                url: '/rest/updateKey/' + $scope.account + "/" + $scope.repoName + '/' + $scope.entry.key,
                                data: $httpParamSerializer(form),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function(response)
                            {
                                if (response.data.success)
                                {
                                    $scope.entry.f.k[$scope.side].isEdited = false;
                                    keyChanged = $scope.entry.f.k[$scope.side].key != $scope.entry.key;
                                    if (keyChanged)
                                    {
                                        $scope.postValueModification($scope.entry.f.k[$scope.side].key, $scope.entry.allValues);
                                        $scope.$emit('keyUpdated', {
                                            from: $scope.entry.key,
                                            to: $scope.entry.f.k[$scope.side].key
                                        });
                                    }

                                    $scope.entryUpdatePostValueModification($scope.entry.key);

                                    if ($scope.postKeySaveCallback)
                                        $scope.postKeySaveCallback(keyChanged, $scope.entry.f.k[$scope.side].key);
                                }
                                else
                                {
                                    $scope.errorMessage = response.data.message;
                                }
                            })
                        }

                        $scope.toggleAllValuesForEditedKey = function() {
                            $scope.toggleAllValues($scope.merging ? $scope.merging.merging : false, $scope.side);
                        };

                        $scope.getKeys = function(val) {
                            return $http.get('/rest/keySearch/' + $scope.account + "/" + $scope.repoName, {
                                params: { t: val }
                            }).then(function(response){
                                return response.data;
                            });
                        };

                        $scope.updateKey = function(akey)
                        {
                            $scope.entry.f.k[$scope.side].key = akey;
                            $scope.keyChange();
                        };


                        $scope.chooseSP = function(sp)
                        {
                            userSetAttribs.sp = sp ? sp.name : '';
                            $scope.entry.f.k[$scope.side].keydata.spName = userSetAttribs.sp;
                        };
                        $scope.chooseVdt = function(type)
                        {
                            userSetAttribs.type = type;
                            $scope.entry.f.k[$scope.side].keydata.vdt = userSetAttribs.type;
                        };
                        $scope.choosePush = function(pushEnabled)
                        {
                            userSetAttribs.pushEnabled = pushEnabled;
                            $scope.entry.f.k[$scope.side].keydata.pushEnabled = userSetAttribs.pushEnabled;
                        };

                        $scope.attributeLock = false;
                        $scope.keyChange = function()
                        {
                            if ($scope.merging && $scope.entry.allValues)
                                $scope.toggleAllValues($scope.merging.merging, $scope.side);

                            if ($scope.entry.key === $scope.entry.f.k[$scope.side].key)
                            {
                                $scope.merging = null;
                                $scope.errorType = '';
                                $scope.errorMessage = '';
                                $scope.attributeLock = false;

                                return;
                            }

                            $scope.errorType = "";

                            $http({
                                method: 'POST',
                                url: '/rest/validateKey/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer({
                                    fromKey: $scope.entry.key,
                                    toKey: $scope.entry.f.k[$scope.side].key
                                }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function(response)
                            {
                                $scope.merging = response.data;
                                $scope.errorType = $scope.merging.error;
                                $scope.attributeLock = response.data.attributeLock;

                                if ($scope.entry.newProperty)
                                {
                                    if (response.data.spName)
                                        $scope.entry.f.k[$scope.side].keydata.spName = response.data.spName;
                                    else
                                        $scope.entry.f.k[$scope.side].keydata.spName = userSetAttribs.sp;

                                    if (response.data.vdt)
                                        $scope.entry.f.k[$scope.side].keydata.vdt = response.data.vdt;
                                    else
                                        $scope.entry.f.k[$scope.side].keydata.vdt = userSetAttribs.type;

                                    if (response.data.pushEnabled)
                                        $scope.entry.f.k[$scope.side].keydata.pushEnabled = response.data.pushEnabled;
                                    else
                                        $scope.entry.f.k[$scope.side].keydata.pushEnabled = userSetAttribs.pushEnabled;
                                }
                            });
                        };

                        $scope.deleteAllValues = function()
                        {
                            spName = $scope.getSpName($scope.side);
                            secretService.authAndExec($scope, null, spName, function() {
                                $http({
                                    method: 'POST',
                                    url: '/rest/deleteKeyAndProperties/' + $scope.account + "/" + $scope.repoName,
                                    data: $httpParamSerializer({
                                        key: $scope.entry.key,
                                        spPassword: secretService.get(spName)
                                    }),
                                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                }).then(function(response)
                                {
                                    if (response.data.success)
                                    {
                                        $scope.entry.f.k[$scope.side].isEdited = false;

                                        if ($scope.entry.f.k[$scope.side].key != $scope.entry.key)
                                            $scope.postValueModification($scope.entry.f.k[$scope.side].key, $scope.entry.allValues);

                                        $timeout(function () {
                                            $scope.entryUpdatePostValueModification($scope.entry.key);
                                        }, slideTime);

                                        if ($scope.postKeyDeleteCallback)
                                            $scope.postKeyDeleteCallback();
                                    }
                                    else
                                    {
                                        $scope.errorMessage = response.data.message;
                                    }
                                })
                            });
                        };
                    }
                ]
            }
        })

        .directive('valueEditor', function ()
        {
            return {
                restrict: "A",
                templateUrl: 'repo/valueForm.tpl.html',
                scope: true,
                controller: ['$scope', '$http', 'secretService', '$httpParamSerializer', 'focus', '$timeout',
                    function ($scope, $http, secretService, $httpParamSerializer, focus, $timeout)
                    {
                        $scope.addValueScope($scope);

                        $scope.validType = true;
                        $scope.listData = [''];
                        $scope.mapData = [{p: ['','']}];

                        $scope.contextSelectConfig = $scope.canManageContext ? propContextSelectConfig : propContextSelectConfigNoEdit;

                        var t, k, i,
                            score,
                            name,
                            allGood,
                            index,
                            kd,
                            addComment,
                            spName,
                            e,
                            editor,
                            editSession,
                            tmpV = $scope.value,
                            heightUpdateFunction = function() {

                                var newHeight =
                                    editSession.getScreenLength()
                                    * editor.renderer.lineHeight
                                    + editor.renderer.scrollBar.getWidth();

                                if (newHeight > 500)
                                    return;

                                e = angular.element(document.querySelector('#li_' + $scope.side + '_' + ($scope.property.id ? $scope.property.id : 'n')));
                                e.height(newHeight.toString() + "px");
                                editor.resize();
                            }
                            ;

                        $scope.aceLoaded = function(_editor)
                        {
                            editor = _editor;
                            editor.setTheme("ace/theme/github");
                            editSession = editor.getSession();
                            editSession.setUseWrapMode(true);
                            editSession.on('change', heightUpdateFunction);
                            editor.setReadOnly(false);

                            $timeout(function() {
                                heightUpdateFunction();
                                focus('li_' + $scope.side + '_' + ($scope.property.id ? $scope.property.id : 'n'));
                            }, 100);
                        };

                        $scope.getFiles = function(val) {
                            return $http.get('/rest/fileSearch/' + $scope.account + "/" + $scope.repoName, {
                                params: { t: val }
                            }).then(function(response){
                                return response.data;
                            });
                        };


                        $scope.setNull = function() {
                            tmpV = $scope.value;
                            $scope.value = null;

                            kd = $scope.getEntryData($scope.side);

                            if (kd.vdt == 'List')
                            {
                                tmpV = $scope.listData;
                                $scope.listData = null;
                            }
                            else if (kd.vdt == 'Map')
                            {
                                tmpV = $scope.mapData;
                                $scope.mapData = null;
                            }

                            validateValue(kd.vdt);
                        };

                        $scope.setNonNull = function() {
                            if (tmpV)
                                $scope.value = tmpV;
                            else
                                $scope.value = '';

                            kd = $scope.getEntryData($scope.side);

                            if (kd.vdt == 'List')
                            {
                                if (tmpV)
                                    $scope.listData = tmpV;
                            }
                            else if (kd.vdt == 'Map')
                            {
                                if (tmpV)
                                    $scope.mapData = tmpV;
                            }

                            validateValue(kd.vdt);
                        };

                        function setValue(value)
                        {
                            if ($scope.entry[$scope.side].vdt == 'Boolean')
                            {
                                $scope.value = value;
                                if (value)
                                    $scope.value = 'true' == value;
                            }
                            else if ($scope.entry[$scope.side].vdt == 'List')
                            {
                                $scope.value = value;
                                if (value)
                                    $scope.listData = angular.copy(value);
                            }
                            else if ($scope.entry[$scope.side].vdt == 'Map')
                            {
                                $scope.value = value;
                                if (value)
                                {
                                    t = angular.copy(value);
                                    $scope.mapData = [];
                                    for (k in t)
                                        $scope.mapData.push({p: [k, t[k]]});
                                }
                            }
                            else
                            {
                                $scope.value = value;
                            }
                        }

                        setValue($scope.property.value);

                        $scope.active = $scope.property.active;
                        $scope.context = {};

                        for (i in $scope.property.levels)
                        {
                            score = $scope.property.levels[i].p;
                            name = $scope.property.levels[i].n ? $scope.property.levels[i].n : "";

                            $scope.context[score] = name;
                        }

                        // -----------------------------------------------------------------------
                        // Value-Data-Type
                        // -----------------------------------------------------------------------
                        function validateValue(type)
                        {
                            switch (type) {
                                case 'Integer':
                                case 'Long':
                                    $scope.validateInt($scope.value);
                                    break;

                                case 'Double':
                                case 'Float':
                                    $scope.validateDouble($scope.value);
                                    break;

                                case 'Map':
                                    $scope.validateMap();
                                    break;

                                default:
                                    $scope.validType = true;
                                    break;
                            }
                        }

                        // -----------------------------------------------------------------------
                        // Value-Data-Type::List
                        // -----------------------------------------------------------------------
                        $scope.validateInt = function(value)
                        {
                            if (null == value)
                                $scope.validType = true;
                            else
                                $scope.validType = IntType.test(value);
                        };

                        $scope.validateDouble = function(value)
                        {
                            if (null == value)
                                $scope.validType = true;
                            else
                                $scope.validType = DecType.test(value);
                        };

                        $scope.addListItem = function() {
                            $scope.listData.push('');
                        };

                        $scope.removeListItem = function(index)
                        {
                            $scope.listData.splice(index, 1);
                            focus('li_' + $scope.side + '_' + ($scope.property.id ? $scope.property.id : 'n') + "_" + (index-1));
                        };

                        // -----------------------------------------------------------------------
                        // Value-Data-Type::Map
                        // -----------------------------------------------------------------------

                        $scope.addMapItem = function() {
                            $scope.mapData.push({p: ['','']});
                            focus('mp_' + $scope.side + '_' + ($scope.property.id ? $scope.property.id : 'n') + "_" + ($scope.mapData.length-1));
                            $scope.validateMap();
                        };

                        $scope.removeMapItem = function(index)
                        {
                            $scope.mapData.splice(index, 1);
                            focus('mp_' + $scope.side + '_' + ($scope.property.id ? $scope.property.id : 'n') + "_" + (index-1));
                            $scope.validateMap();
                        };

                        $scope.validateMap = function()
                        {
                            t = {};
                            allGood = true;
                            for (index in $scope.mapData)
                            {
                                k = $scope.mapData[index].p[0];
                                if (!k)
                                {
                                    $scope.mapData[index].err = true;
                                    allGood = false;
                                }
                                else if (t[k])
                                {
                                    t[k].v.err = $scope.mapData[index].err = true;
                                    allGood = false;
                                }
                                else
                                {
                                    $scope.mapData[index].err = false;
                                    t[k] = { v: $scope.mapData[index] };
                                }
                            }
                            $scope.validType = allGood;
                        };


                        // -----------------------------------------------------------------------
                        // Navigation
                        // -----------------------------------------------------------------------

                        $scope.decodeMessage = '';
                        $scope.property.decriptValidated = false;

                        $scope.decryptValue = function()
                        {
                            secretService.authAndExec($scope, null, $scope.entry[$scope.side].spName,
                                function() {
                                    $http({
                                        method: 'POST',
                                        url: '/rest/decryptValue/' + $scope.account + "/" + $scope.repoName,
                                        data: $httpParamSerializer({
                                            propertyId: $scope.property.id,
                                            spPassword: secretService.get($scope.entry[$scope.side].spName)
                                        }),
                                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                    }).then(function(response)
                                    {
                                        $scope.decodeMessage = response.data.message;
                                        $scope.property.decriptValidated = response.data.success;

                                        if (response.data.success)
                                        {
                                            setValue(response.data.value);
                                        } else {
                                            $scope.errorMessage = response.data.message;
                                        }

                                    });
                                }
                            );
                        };

                        $scope.saveValue = function()
                        {
                            if ($scope.demo) return;

                            kd = $scope.getEntryData($scope.side);

                            if (kd.vdt == 'List')
                            {
                                if (!$scope.listData || $scope.listData.length == 0)
                                    $scope.value = null;
                                else
                                    $scope.value = JSON.stringify($scope.listData);
                            }
                            else if (kd.vdt == 'Map')
                            {
                                if (!$scope.mapData || $scope.mapData.length == 0)
                                    $scope.value = null;
                                else
                                {
                                    t = {};
                                    for (i in $scope.mapData)
                                        t[$scope.mapData[i].p[0]] = $scope.mapData[i].p[1];

                                    $scope.value = JSON.stringify(t);
                                }
                            }

                            addComment = angular.element(document.querySelector('#changeCommentField')).length > 0;

                            secretService.authAndExec($scope, null, kd.spName, function() {
                                $http({
                                    method: 'POST',
                                    url: '/rest/saveProperty/' + $scope.account + "/" + $scope.repoName,
                                    data: $httpParamSerializer({
                                        key: kd.key,
                                        comment: kd.comment,
                                        deprecated: kd.deprecated,
                                        vdt: kd.vdt,
                                        pushEnabled: kd.pushEnabled,
                                        value: $scope.value,
                                        active: $scope.active,
                                        context: contextParam($scope.context),
                                        changeComment: addComment ? $scope.changeComment : '',
                                        propertyId: $scope.property.id,
                                        spPassword: secretService.get(kd.spName),
                                        spName: kd.spName
                                    }),
                                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                }).then(function(response)
                                {
                                    if (!response.data.success) {
                                        if (response.data.circularRef) {
                                            $scope.circularRef = response.data.circularRef;
                                            $scope.errorMessage = null;
                                        }
                                        else {
                                            $scope.errorMessage = response.data.message;
                                            $scope.circularRef = null;
                                        }
                                    }
                                    else
                                    {
                                        $scope.conflictProperty = null;
                                        $scope.conflict = false;

                                        $scope.entryUpdatePostValueModification($scope.getKey($scope.side));
                                        $scope.disableValueEditor($scope.property, $scope.side);

                                        if ($scope.postValueSaveCallback)
                                            $scope.postValueSaveCallback();
                                    }
                                });
                            });
                        };

                        $scope.deleteValue = function()
                        {
                            if ($scope.demo) return;

                            spName = $scope.getSpName($scope.side);
                            secretService.authAndExec($scope, null, spName, function() {
                                $http({
                                    method: 'POST',
                                    url: '/rest/deleteProperty/' + $scope.account + "/" + $scope.repoName,
                                    data: $httpParamSerializer({
                                        id: $scope.property.id,
                                        spPassword: secretService.get(spName)
                                    }),
                                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                                }).then(function(response)
                                {
                                    if (!response.data.success)
                                        $scope.errorMessage = response.data.message;
                                    else {
                                        $scope.disableValueEditor($scope.property, $scope.side);
                                        $scope.entryUpdatePostValueModification($scope.entry.key);

                                        if ($scope.postValueDeleteCallback)
                                            $scope.postValueDeleteCallback($scope.entry);
                                    }
                                })
                            });
                        };

                        /**
                         * Each time context specification changes for a value, check
                         * there are no conflicts with existing values for the same key.
                         */
                        $scope.validateContext = function()
                        {
                            if ($scope.demo || !$scope.active) return;

                            $http({
                                method: 'POST',
                                url: '/rest/contextChange/value/' + $scope.account + "/" + $scope.repoName,
                                data: $httpParamSerializer({
                                    context: contextParam($scope.context),
                                    key:  $scope.getKey($scope.side),
                                    propertyId: $scope.property.id }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function(response) {

                                if (response.data.success)
                                {
                                    $scope.conflict = response.data.conflict;
                                    if ($scope.conflict)
                                        $scope.conflictProperty = response.data.conflictProperty;
                                    else
                                        $scope.conflictProperty = null;

                                    $scope.toggleConflict($scope.property, $scope.side, $scope.conflictProperty);
                                }
                                else
                                {
                                    $scope.errorMessage = response.data.message;
                                }

                            })
                        };

                        $scope.$watch('active', function(newVal, oldVal) {
                            $scope.validateContext();
                        }, true);

                        if ($scope.entry.newProperty) {
                            $scope.$watch('entry.f.k[side].key', function(newVal, oldVal) {
                                $scope.validateContext();
                            }, true);
                            $scope.$watch('entry.f.k[side].keydata.vdt', function(newVal, oldVal) {
                                validateValue(newVal);
                            }, true);
                        } else {
                            // when form is opened, validate context
                            $scope.validateContext();
                        }

                        $scope.toggleConflictProperty = function() {
                            $scope.toggleConflict($scope.property, $scope.side, $scope.conflictProperty);
                        };

                        $scope.$on("$destroy", function handleDestroyEvent() {
                                $scope.removeValueScope($scope);
                            }
                        );

                    }]
            }
        })

        ;
}
