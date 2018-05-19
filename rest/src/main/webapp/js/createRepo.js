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
    .module('configHub.repoCreate', [])

    .controller('CreateRepositoryController',
        ['$state', '$stateParams', '$http', '$scope', '$rootScope', 'repositoryNameCheckService', '$httpParamSerializer',
            function ($state, $stateParams, $http, $scope, $rootScope, repositoryNameCheckService, $httpParamSerializer)
            {
                $scope.administeredAccounts = [];
                $scope.isNew = true;

                $http
                    .get('/rest/getAdministeredAccounts')
                    .then(function (response)
                    {
                        $scope.administeredAccounts = response.data;
                    });

                $scope.ownerSelectConfig = {
                    create: false,
                    valueField: 'un',
                    labelField: 'un',
                    searchField: ['un'],
                    delimiter: ',',
                    placeholder: 'Choose owner',
                    closeAfterSelect: true,
                    openOnFocus: true,
                    sortField: 'un',
                    maxItems: 1
                };


                $scope.togglesDisabled = false;
                $scope.repo = {};
                $scope.features = {};
                $scope.repo.owner = $stateParams.account ? $stateParams.account : $scope.user().username;
                $scope.features.accessControlEnabled = false;
                $scope.features.securityProfilesEnabled = false;
                $scope.features.valueTypeEnabled = false;
                $scope.features.contextClustersEnabled = false;

                $scope.hideFeaturesBtns = true;

                var allGood = true,
                    i = 0,
                    form = {},
                    lastRequest;
                
                function validateForm()
                {
                    $scope.errorMessage = '';
                    $scope.errorName = '';
                    $scope.contextError = '';
                    $scope.ctxLbl = [];

                    allGood = true;
                    if (!$scope.repo.owner)
                    {
                        $scope.errorOwner = 'Select repository owner.';
                        allGood = false;
                    }

                    if (!$scope.repo.name) {
                        $scope.errorName = 'Repository name is not specified.';
                        allGood = false;
                    }


                    for (i in $scope.contexts)
                    {
                        if (!$scope.contexts[i].label)
                        {
                            $scope.contexts[i].bad = true;
                            $scope.contextError  = "Context labels have to be specified";
                            allGood = false;
                        }
                        else
                            delete $scope.contexts[i].bad;
                    }

                    return allGood;
                }

                $scope.createRepository = function()
                {
                    if (!validateForm())
                        return;

                    form = {
                        owner: $scope.repo.owner,
                        name: $scope.repo.name,
                        description: $scope.repo.description,
                        'private': true,
                        contextSize: $scope.contexts.length,
                        labels: getLabels()
                    };

                    $http({
                        method: 'POST',
                        url: '/rest/createRepository',
                        data: $httpParamSerializer(form),
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function successCallback(response)
                    {
                        if (!response.data.success) {

                            $scope.errorMessage = response.data.message;

                            if (response.data.hasOwnProperty('errors')) {
                                $scope.errorName = response.data.errors.name;
                                $scope.errorDepth = response.data.errors.depth;
                            } 
                        } else {
                            $state.go('repo.editor', {owner: $scope.repo.owner, name: $scope.repo.name });
                        }
                    });
                };


                // ------------------------------------------------------------------------------
                // Depth
                // ------------------------------------------------------------------------------

                function getLabels()
                {
                    return $scope.contexts.map(function(elem){
                        return elem.label;
                    }).join(",");
                }

                $scope.contexts = [
                    { label: 'Product' },
                    { label: 'Environment' },
                    { label: 'Application' },
                    { label: 'Instance' }
                ];

                $scope.removeContext = function(index) {
                    $scope.contexts.splice(index, 1);
                };

                $scope.addContext = function() {
                    if ($scope.contexts.length < 10)
                        $scope.contexts.push({ label: '' })
                };

                // ------------------------------------------------------------------------------
                // Repo name
                // ------------------------------------------------------------------------------
                $scope.errorName = '';
                $scope.errorOwner = '';

                $scope.validateName = function ()
                {
                    if (!$scope.repo.owner)
                        $scope.errorOwner = true;

                    else {
                        repositoryNameCheckService.cancel(lastRequest);
                        lastRequest = repositoryNameCheckService.isValid($scope.repo.owner, $scope.repo.name);
                        lastRequest.then(
                            function handleCollaboratorsResolve(ret)
                            {
                                switch (ret) {
                                    case '1': $scope.errorName = ""; break;
                                    case '2': $scope.errorName = nameError; break;
                                    case '3': $scope.errorName = "You already own a repository with the same name"; break;

                                    default: $scope.errorName = ""; break;
                                }
                            }
                        );
                        return ( lastRequest );
                    }
                };
            }
        ])

    ;
