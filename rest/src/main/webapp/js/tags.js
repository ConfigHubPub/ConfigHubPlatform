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
    .module('configHub.repository.tags', [
    ])

    .controller('TagsController',
        ['$scope', '$rootScope', '$stateParams', '$httpParamSerializer', '$http', 'toUtc',
        function($scope, $rootScope, $stateParams, $httpParamSerializer, $http, toUtc) {

            $scope.account = $stateParams.owner;
            $scope.repoName = $stateParams.name;

            $scope.day = moment();

            $rootScope.selectedTab = 0;

            $scope.selectedLabel = "Date";
            $scope.selectedField = "ts";
            $scope.asc = false;
            $scope.sortBy = function(label, field)
            {
                if ($scope.selectedField === field)
                    return;
                else
                {
                    $scope.selectedField = field;
                    $scope.selectedLabel = label;
                }
            };

            $scope.toggleSortOrder = function()
            {
                $scope.asc = !$scope.asc;
            };

            $scope.showNewTagForm = function()
            {
                $scope.ts = new Date();
                $scope.newTagForm = true;
            };

            $scope.edit = function(tag)
            {
                var f = angular.copy(tag);
                tag.f = f;
                tag.edit = true;
            };

            $scope.cancelEdit = function(tag)
            {
                delete tag.f;
                tag.edit = false;
            };

            $scope.newTagForm = false;
            $scope.cancelNew = function() {
                $scope.newTagForm = false;
            };

            $scope.tsFormat = tsFormat;

            $scope.createTag = function(name, readme, ts)
            {
                $http({
                    method: 'POST',
                    url: '/rest/createTag/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        name: name,
                        readme: readme,
                        ts: toUtc.toMS(ts)
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        getTags();
                        $scope.newTagForm = false;
                    }
                    else
                        $scope.message = response.data.message;
                });
            };


            $scope.updateTag = function(tag)
            {
                $http({
                    method: 'POST',
                    url: '/rest/updateTag/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({
                        name: tag.name,
                        newName: tag.f.name,
                        readme: tag.f.readme,
                        ts: toUtc.toMS(tag.f.ts)
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                    {
                        getTags();
                        //$scope.newTagForm = false;
                    }
                    else
                        $scope.message = response.data.message;
                });
            };

            $scope.deleteTag = function(name)
            {
                $http({
                    method: 'POST',
                    url: '/rest/deleteTag/' + $scope.account + "/" + $scope.repoName,
                    data: $httpParamSerializer({ name: name }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response)
                {
                    if (response.data.success)
                        getTags();
                    else
                        $scope.message = response.data.message;
                });
            };

            $scope.tags = [];
            function getTags()
            {
                $http
                    .get("/rest/getTags/" + $scope.account + "/" + $scope.repoName)
                    .then(function (response)
                    {
                        if (response.data.success)
                            $scope.tags = response.data.tags;
                        else
                            $scope.message = response.data.message;
                    });
            }

            getTags();

        }])

    ;