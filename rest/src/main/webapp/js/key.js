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
    .module('configHub.repository.key', ['diff-match-patch'])

    .controller('KeyController', ['$scope', '$http', '$stateParams', '$httpParamSerializer', 'editorInit', '$state', '$rootScope',
        function($scope, $http, $stateParams, $httpParamSerializer, editorInit, $state, $rootScope)
        {
            $scope.auditRefreshCnt = 0;

            editorInit.setState('key');
            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;
            $rootScope.selectedTab = 0;

            $scope.mode = 'key';

            $scope.key = $stateParams.key;
            $scope.initialized = true;

            editorInit.initialize($scope);

            $scope.postKeyDeleteCallback = function()
            {
                $scope.auditRefreshCnt++;
            };

            $scope.postKeySaveCallback = function(keyChanged, newKey)
            {
                if (keyChanged)
                {
                    $state.go('repo.key',
                        {owner: $scope.account, name: $scope.repoName, key: newKey},
                        {notify: true});
                }
                else
                    $scope.auditRefreshCnt++;
            };

            $scope.postValueSaveCallback = function()
            {
                $scope.auditRefreshCnt++;
            };

            $scope.postValueDeleteCallback = function(entry)
            {
                $scope.auditRefreshCnt++;
            };

        }])


    ;