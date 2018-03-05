{
    angular
        .module('configHub.repository.newProperty', [])

        .directive('newProperty', function ()
        {
            return {
                restrict: "A",
                templateUrl: 'repo/property.tpl.html',
                scope: true,
                controller: ['$scope',
                    function ($scope)
                    {
                        $scope.entry = {
                            newProperty: true
                        };

                        $scope.resetNewPropertyForm = function() {
                            $scope.entry = {
                                newProperty: true,
                                isKeyEditable: true,
                                key: "",
                                1: {
                                    vdt: 'Text'
                                },
                                properties: [{
                                    stickyForm: true,
                                    value: '',
                                    levels: [],
                                    editable: true,
                                    active: true,
                                    isNew: true
                                }]
                            };
                        };

                        $scope.resetNewPropertyForm();

                    }
                ]
            }
        })

    ;
}