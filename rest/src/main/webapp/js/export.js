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

// TODO REMOVE

angular
    .module('configHub.repository.export', [
        'configHub.repository.contextService',
        'ngAnimate',
        'puElasticInput',
        'monospaced.elastic'
    ])

    .directive('selectOnClick', ['$window', function ($window) {
        return {
            link: function (scope, element) {
                element.on('dblclick', function () {
                    var selection = $window.getSelection(),
                        range = document.createRange();

                    range.selectNodeContents(element[0]);
                    selection.removeAllRanges();
                    selection.addRange(range);
                });
            }
        }
    }])

    .controller('ExportController',
    ['$timeout', '$stateParams', '$scope', '$http', 'contextService', 'store', '$filter', '$httpParamSerializer',
        function ($timeout, $stateParams, $scope, $http, contextService, store, $filter, $httpParamSerializer)
        {
            var ctxStoreName = 'c_' + $stateParams.owner + "/" + $stateParams.name,
                hdcStoreName = 'hcd_' + $stateParams.owner + "/" + $stateParams.name,
                o = {},
                t = store.get(ctxStoreName),
                ctxParsed,
                i, a, e,
                data,
                filename,
                blob,
                d = store.get(hdcStoreName);


            if (t)
            {
                ctxParsed = JSON.parse(t);
                for (i in ctxParsed)
                {
                    if (ctxParsed[i] && ctxParsed[i].length == 1)
                        o[i] = ctxParsed[i][0];
                }
            }

            $scope.commonFormats = [
                {
                    f: 'Text',
                    name: 'Text',
                    type: 'text'
                },
                {
                    f: 'JSON_Array',
                    name: 'JSON Array',
                    type: 'json'
                },
                {
                    f: 'JSON_Map',
                    name: 'JSON Map',
                    type: 'json'
                },
                {
                    f: 'JSON_Simple_Map',
                    name: 'JSON Map (Simple)',
                    type: 'json'
                }
            ];

            $scope.format = $scope.commonFormats[1];
            $scope.includeComments = true;

            $scope.setFormat = function(format) {
                $scope.format = format;
                $scope.updateContext();
            };

            $scope.context = o;
            $scope.configFile = '';
            $scope.errorMessage = '';


            $scope.downloadFile = function()
            {
                data = $scope.configFile;
                if (!data) return;

                filename = "config";

                if ($scope.format.type === 'json')
                    filename = filename + ".json";
                else
                    filename = filename + ".cfg";

                blob = new Blob([data], {
                        type: 'text/json;charset=utf-8',
                        name: filename,
                        lastModifiedDate: new Date()
                    }),
                    e = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    }),
                    a = document.createElement('a');

                a.download = filename;
                a.href = window.URL.createObjectURL(blob);
                a.dataset.downloadurl = ['text/json;charset=utf-8', a.download, a.href].join(':');
                a.dispatchEvent(e);
            };

            function generateConfigFile()
            {
                $http({
                    method: 'POST',
                    url: '/rest/generateConfigFile/' + $stateParams.owner + "/" + $stateParams.name,
                    data: $httpParamSerializer({
                        context: contextParam($scope.context),
                        formatType: $scope.format.f,
                        comments: $scope.includeComments
                    }),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function(response) {

                    if (response.data.success)
                    {
                        if ($scope.format.type === 'json')
                            $scope.configFile = JSON.stringify(response.data.file, undefined, 2);
                        else
                            $scope.configFile = response.data.file;

                        $scope.errorMessage = '';
                    }
                    else
                    {
                        $scope.errorMessage = response.data.message;
                        $scope.configFile = null;
                    }
                });
            }


            // --------------------------------
            // Repo date
            // --------------------------------
            $scope.date = d ? new Date(JSON.parse(d)) : null;
            $scope.isLive = function () { return null === $scope.date; };
            $scope.getHistoryLabel = function ()
            {
                if (null == $scope.date)
                    return "Latest";
                else
                    return $filter('date')($scope.date, 'MMMM d, y @ h:mm a');
            };
            $scope.clearDate = function ()
            {
                $scope.date = null;
                getRepoContext();
            };
            $scope.changeDate = function ()
            {
                getRepoContext();
            };

            // --------------------------------
            // Repo context
            // --------------------------------
            $scope.contextSelectConfig = propContextSelectConfig;
            $scope.contextSelectConfig.create = true;

            $scope.repoContext = { loaded: false };
            $scope.isFullContext = false;

            $scope.updateContext = function()
            {
                Object.keys($scope.context).forEach(function(score) {
                    if (!$scope.context[score]) {
                        delete $scope.context[score];
                    }
                });

                if (Object.keys($scope.context).length == $scope.repoContext.depthScores.length)
                {
                    $scope.isFullContext = true;
                    generateConfigFile();
                }
                else {
                    $scope.isFullContext = false;
                }
            };

            function getRepoContext()
            {
                $scope.repoContext.loaded = false;
                contextService.contextElements($scope.date).then(function(ctx) {
                    $scope.repoContext = ctx;
                    $scope.repoContext.loaded = true;
                    $scope.updateContext();
                });
            }

            // --------------------------------
            // Initialization
            // --------------------------------
            getRepoContext();
        }

    ])

;