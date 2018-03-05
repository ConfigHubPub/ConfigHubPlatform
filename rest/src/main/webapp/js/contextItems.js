angular
    .module('configHub.repository.contextItems', [])

    .controller('ContextItemsController', ['$stateParams', '$scope', '$http', '$rootScope',
        function ($stateParams, $scope, $http, $rootScope)
        {
            $scope.depthData = {};
            $scope.depthScores = [];
            $scope.initialized = false;
            $rootScope.selectedTab = 0;

            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;

            $http
                .get('/rest/contextItems/' + $stateParams.owner + "/" + $stateParams.name)
                .then(function successCallback(response)
                {
                    $scope.depthData = response.data.depthData;
                    $scope.depthScores = response.data.depthScores;
                    $scope.canManageContext = response.data.canManageContext;
                    $scope.initialized = true;
                });

            $scope.loadingIsDone = function() { return $scope.loadingIsDone; }
            $scope.repoName = $stateParams.name;
        }])

    // filter all levels on name only
    .filter('levelFilter', function ()
    {
        return function (items, field, reverse)
        {
            if (!field)
                return items;

            var filtered = [],
                re = new RegExp(field, 'i'),
                ki = 0,
                item;

            for (; ki < items.length; ki++) {
                item = items[ki];
                if (re.test(item.name)) {
                    filtered.push(item);
                }
            }

            return filtered;
        }
    })

    ;
