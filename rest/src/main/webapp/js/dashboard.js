angular

    .module('configHub.dashboard', [])

    .run(['$rootScope', function($rootScope) {
        $rootScope.repository = {};
    }])

    .config(['$stateProvider', function ($stateProvider)
    {
        $stateProvider.state('dashboard', {
            url: '/home/',
            controller: 'DashboardCtrl',
            templateUrl: 'dashboard/dashboard.html',
            data: {
                requireLogin: true
            }
        });
    }])


    .controller('DashboardCtrl', ['$http', '$scope', '$filter', 'store',
        function ($http, $scope, $filter, store)
    {
        $scope.repositories = null;
        $scope.selectedRepo = null;
        $scope.initialized = false;

        $scope.account;
        $scope.repoName;

        $http({
            method: 'GET',
            url: '/rest/getDashboardElements'
        }).then(function successCallback(response) {

            $scope.sideBySide = store.get("splitView");
            if (!$scope.sideBySide)
                $scope.sideBySide = false;

            $scope.$on('sideBySide', function (a, sideBySide) {
                $scope.sideBySide = sideBySide;
            });
            $scope.repositories = response.data.repositories;

            if ($scope.repositories)
            {
                var orderBy = $filter('orderBy'),
                    index = 0,
                    lastRepo = store.get('lastRepo');

                $scope.repositories = orderBy($scope.repositories, 'name');

                if (lastRepo)
                {
                    index = indexOf($scope.repositories, 'id', lastRepo.id);
                    if (index < 0) index = 0;
                }

                $scope.selectRepo($scope.repositories[index]);
            }

            $scope.initialized = true;
        }, function errorCallback(response) {
            console.log("Caught error:");
            console.log(response);
        });



        $scope.auditRefreshCnt = 0;
        $scope.selectRepo = function(repo)
        {
            $scope.selectedRepo = repo;
            $scope.account = repo.account;
            $scope.repoName = repo.name;
            $scope.auditRefreshCnt++;
        };
    }])

    ;
