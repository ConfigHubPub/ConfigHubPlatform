angular
    .module('configHub.repository.timeTagSelect', [])

    .directive('timeSelect', function()
    {
        return {
            restrict: 'EA',
            templateUrl: 'repo/timeTagSelect.tpl.html',
            scope: true,
            controller: ['$scope', '$stateParams', 'store', '$http',
                function ($scope, $stateParams, store, $http)
                {

                    $http({
                        method: 'GET',
                        url: "/rest/getTags/" + $stateParams.owner + "/" + $stateParams.name
                    }).then(function successCallback(response) {
                        if (response.data.success)
                            $scope.tags = response.data.tags;
                    });

                    $scope.repoName = $stateParams.name;
                    $scope.account = $stateParams.owner;
                    $scope.tagSelectConfig = tagSelectConfig;

                    $scope.tsFormat = tsFormat;

                    function setDate(date) {
                        $scope.date = date;
                        $scope.setDate(date);
                    }

                    function setTag(tag) {
                        $scope.selectedTag = tag;
                        $scope.setTag(tag);
                    }

                    
                    $scope.getRepoTs = function()
                    {
                        if (null === $scope.date)
                            return $scope.now;

                        return $scope.date;
                    };

                    $scope.goLive = function ()
                    {
                        setTag(null);
                        $scope.timeLabel = null;
                        $scope.tagLabel = null;
                        $scope.timeToSet = null;

                        $scope.updateUrl();

                        if (null != $scope.date)
                        {
                            setDate(null);
                            $scope.now = Date.now();
                            $scope.postTimeChange();
                            store.remove($scope.hdcStoreName);
                            store.remove($scope.tagStoreName);
                        }
                    };

                    $scope.changeDate = function(timeToSet)
                    {
                        setTag(null);
                        $scope.timeToSet = timeToSet;
                    };

                    var i, tag;

                    $scope.tagChange = function(selectedTag)
                    {
                        i = indexOf($scope.tags, 'name', selectedTag);
                        if (-1 == i) {
                            $scope.postTimeChange();
                            return;
                        }

                        tag = $scope.tags[i];
                        $scope.timeToSet = new Date(tag.ts);
                        setTag(selectedTag);
                        store.set($scope.tagStoreName, JSON.stringify(selectedTag));
                    };

                    $scope.setDayTime = function()
                    {
                        $scope.tagLabel = $scope.selectedTag;
                        $scope.timeLabel = $scope.timeToSet;
                        setDate($scope.timeToSet);
                        store.set($scope.hdcStoreName, JSON.stringify($scope.timeToSet));

                        $scope.postTimeChange();
                    };


                }
            ]
        }
    });
