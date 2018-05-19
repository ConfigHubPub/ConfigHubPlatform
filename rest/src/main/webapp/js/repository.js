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
    .module('configHub.repository', [
        'configHub.repository.compare',
        'configHub.repository.contextItem',
        'configHub.repository.contextItems',
        'configHub.repository.editor',
        'configHub.repository.export',
        'configHub.repository.newProperty',
        'configHub.repository.filters',
        'configHub.repository.tags',
        'configHub.repository.settings',
        'configHub.repository.teams',
        'configHub.repository.tokens',
        'configHub.repository.profiles',
        'configHub.repository.secretStore',
        'configHub.repository.key',
        'configHub.repository.audit',
        'ui.keypress',
        'diff-match-patch',
        'fileEditor'
    ])

    .config(['$stateProvider', function ($stateProvider)
    {
        $stateProvider

            .state('repo', {
                templateUrl: 'repository/templates/repositoryHeader.tpl.html',
                'abstract': true
            })

            .state('repo.editor', {
                url: '/r/:owner/:name?tag&ts&ctx',
                templateUrl: 'repo/editor/editor.html',
                pageTitle: 'Editor'
            })

            .state('repo.compare', {
                url: '/:owner/:name/compare?at&ats&bt&bts',
                templateUrl: 'repo/compare/compare.html',
                pageTitle: 'Compare'
            })

            .state('repo.tokens', {
                url: '/:owner/:name/tokens',
                templateUrl: 'repo/tokens/tokens.html',
                pageTitle: 'Tokens'
            })

            .state('repo.key', {
                url: '/:owner/:name/key?key',
                templateUrl: 'repo/key/key.html',
                pageTitle: 'Editor'
            })

            // Security
            .state('repo.security-profiles', {
                url: '/:owner/:name/security-groups',
                templateUrl: 'repo/security/profiles.html',
                pageTitle: 'Security Groups'
            })

            .state('repo.security-profile', {
                url: '/:owner/:name/security-group?profile',
                templateUrl: 'repo/security/profile.html',
                pageTitle: 'Security Group'
            })

            .state('repo.new-security-group', {
                url: '/:owner/:name/create-security-group',
                templateUrl: 'repo/security/newProfile.html',
                pageTitle: 'New Security Group'
            })

            // Tools

            // > Export
            .state('repo.export', {
                url: '/:owner/:name/export',
                templateUrl: 'repo/export/export.html',
                pageTitle: 'Configuration Export'
            })

            .state('repo.audit', {
                url: '/:owner/:name/revisions',
                templateUrl: 'repo/audit/audit.html',
                pageTitle: 'Revisions'
            })

            // Tags
            .state('repo.tags', {
                url: '/:owner/:name/edit/tags?s',
                templateUrl: 'repo/tags/tags.html',
                pageTitle: 'Tags'
            })

            // Settings
            .state('repo.settings', {
                url: '/:owner/:name/edit/settings?s',
                templateUrl: 'repo/settings/settings.html',
                pageTitle: 'Settings'
            })

            // Teams
            .state('repo.teams', {
                url: '/:owner/:name/teams?team',
                templateUrl: 'repo/team/teams.html',
                pageTitle: 'Teams'
            })

            // Context
            .state('repo.context', {
                url: '/:owner/:name/edit/context',
                templateUrl: 'repo/context/contextItems.html',
                pageTitle: 'Context Items'
            })

            .state('repo.contextItem', {
                url: '/:owner/:name/edit/context/:depthLabel/item/:contextItem',
                templateUrl: 'repo/context/contextItem.html',
                pageTitle: 'Context Item'
            })

            .state('repo.newContextItem', {
                url: '/:owner/:name/new/:depthLabel',
                templateUrl: 'repo/context/contextItem.html',
                pageTitle: 'New Context Item'
            })

            .state('createRepo', {
                url: '/r/create?account',
                templateUrl: 'repo/create/createRepo.html',
                pageTitle: 'New Repository',
                data: {
                    requireLogin: true
                }
            })

            .state('repo.files', {
                url: '/r/:owner/:name/files/{path:any}?tag&ts',
                templateUrl: 'repo/files/files.html',
                pageTitle: 'Config Files'
            })

            .state('repo.file', {
                url: '/:owner/:name/edit/file/{id:[0-9]{1,15}}/{fullPath:any}?tag&ts&sp&path',
                templateUrl: 'repo/files/fileEditor.html',
                pageTitle: 'Config File'
            })

        ;
    }])

    .controller('RepositoryInfoController', ['$scope', '$http', '$stateParams', '$rootScope', 'store', '$state',
        function ($scope, $http, $stateParams, $rootScope, store, $state)
        {
            $rootScope.repository = null;
            $rootScope.ut = 0;
            $rootScope.isAdminOrOwner = false;
            //function() { return response.data.ut == 'admin' || response.data.ut == 'owner'; };

            $scope.repoName = $stateParams.name;
            $scope.account = $stateParams.owner;

            var lastConfLocation = "props";

            $rootScope.keys = function(obj){
                return obj? Object.keys(obj) : [];
            };

            $rootScope.gotoConf = function()
            {
                if (lastConfLocation === 'props')
                    $scope.gotoEditor();
                else
                    $scope.gotoFiles();
            };

            $rootScope.gotoEditor = function() {
                $state.go('repo.editor', { owner: $scope.account, name: $scope.repoName });
                lastConfLocation = 'props';
            };

            $rootScope.gotoFiles = function() {
                $state.go('repo.files', { owner: $scope.account, name: $scope.repoName });
                lastConfLocation = 'files';
            };

            $http({
                method: 'GET',
                url: "/rest/repositoryInfo/" + $stateParams.owner + "/" + $stateParams.name
            }).then(function successCallback(response) {

                switch (response.data.ut) {
                    case 'owner': $rootScope.isAdminOrOwner = true; $rootScope.ut = $rootScope.type.owner; break;
                    case 'admin': $rootScope.isAdminOrOwner = true; $rootScope.ut = $rootScope.type.admin; break;
                    case 'member': $rootScope.isAdminOrOwner = false; $rootScope.ut = $rootScope.type.member; break;
                    case 'nonMember': $rootScope.isAdminOrOwner = false; $rootScope.ut = $rootScope.type.nonMember; break;
                    default: $rootScope.isAdminOrOwner = false; break;
                }
                if (response.data.demo && $rootScope.ut < $rootScope.type.member)
                    $rootScope.ut = $rootScope.type.demo;

                store.set('lastRepo', { id: response.data.id, acc: response.data.owner, name: response.data.name });
                $rootScope.repository = response.data;
            });


        }])

    .directive('repositoryHeader', function ()
    {
        return {
            templateUrl: 'repository/templates/repositoryHeader.tpl.html'
        }
    })
    ;