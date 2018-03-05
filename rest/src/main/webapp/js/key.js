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