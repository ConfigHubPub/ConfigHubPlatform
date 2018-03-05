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
