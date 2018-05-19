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
    .module('configHub.repository.contextService', [
        'monospaced.elastic',
        'ngAnimate',
        'puElasticInput'
    ])
    .service('contextService', ['$http', '$q', '$stateParams', 'toUtc',
        function ($http, $q, $stateParams, toUtc)
        {
            return ({
                contextElements: getContextElements
            });

            function getContextElements(date, tag, account, repoName)
            {
                var httpTimeout = $q.defer(),
                    request = $http({
                        method: 'GET',
                        url: '/rest/contextHistory/' + account + "/" + repoName,
                        params: {
                            ts: toUtc.toMS(date),
                            tag: tag
                        },
                        timeout: httpTimeout.promise
                    }),
                    promise = request.then(processContext);

                promise._httpTimeout = httpTimeout;
                return (promise);
            }

            function processContext(response)
            {
                var repoContext = {
                    depths: response.data.depthData,
                    depthScores: response.data.depthScores,
                    contextElements: {},
                    selectableContext: {},
                    tags: response.data.tags
                };

                angular.forEach(response.data.depthScores, function (depth)
                {
                    repoContext.contextElements[depth] = response.data.depthData[depth].levels;
                });

                repoContext.selectableContext = angular.copy(repoContext.contextElements);
                return repoContext;
            }
        }]);
