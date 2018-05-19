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

var lastFrom = '',
    lastFromParams = '',
    ignoreFrom = ['login', 'signup', 'emailVerification', '404', '500',
                  'repo.newContextItem', 'home', 'api', 'terms', 'blog'];

angular
    .module('configHub', [
        'ui.router',
        'angular-storage',
        'angular-jwt',
        'configHub.account',
        'configHub.login',
        'configHub.dashboard',
        'configHub.signup',
        'configHub.repoCreate',
        'configHub.organization',
        'configHub.repository',
        'configHub.download',
        'configHub.common',
        'configHub.info',
        'configHub.admin',
        'configHub.repository.timeTagSelect',
        'ngAnimate',
        'ngSanitize',
        'nzToggle',
        'mgcrea.ngStrap',
        'angularMoment',
        'hljs',
        'btford.markdown'
    ])

    .config(['hljsServiceProvider', '$locationProvider', '$urlRouterProvider', '$stateProvider', '$httpProvider', '$datepickerProvider',
        function(hljsServiceProvider, $locationProvider, $urlRouterProvider, $stateProvider, $httpProvider, $datepickerProvider)
    {
        hljsServiceProvider.setOptions({
            // replace tab with 4 spaces
            tabReplace: '    '
        });

        angular.extend($datepickerProvider.defaults, {
            dateFormat: 'MM/dd/yy'
        });

        $urlRouterProvider
            .when('/{owner}/{name}/files', '/{owner}/{name}/files/')
            .when('/system/', '/system')
            .when('/download/', '/download')
            .when('/api/', '/api')
            .when('/terms/', '/terms')
            .when('/login/', '/login')
            .when('/signup/', '/signup')
            .when('/forgot/', '/forgot')
            .when('/repository/create/', '/repository/create')
            .when('/404/', '/404')
            .when('/500/', '/500')
            .when('/email-verification/', '/email-verification')
            .when('/passwordReset/', '/passwordReset')
            .otherwise(function($injector, $location) {
                var state = $injector.get('$state');
                state.go('404', {notify: false});
                return $location.path();
            });

        $stateProvider

            .state('login', {
                url: '/login',
                templateUrl: 'login/login.html',
                pageTitle: 'Sign In'
            })

            .state('signup', {
                url: '/signup',
                templateUrl: 'signup/signup.html',
                pageTitle: 'Sign Up'
            })

            .state('forgot', {
                url: '/forgot',
                templateUrl: 'login/forgot.html',
                pageTitle: 'Sign In'
            })

            .state('info', {
                url: '/system',
                templateUrl: 'info/info.html',
                pageTitle: 'System Info'
            })

            .state('system', {
                templateUrl: 'info/info.html',
                'abstract': true
            })

            .state('system.settings', {
                url: '/system/settings?s',
                templateUrl: 'info/info.html',
                pageTitle: 'System'
            })

            .state('api', {
                url: '/api',
                templateUrl: 'api/api.html',
                pageTitle: 'API'
            })

            .state('download', {
                url: '/download',
                templateUrl: 'download/download.html',
                pageTitle: 'Downloads'
            })

            .state('terms', {
                url: '/terms',
                templateUrl: 'terms/terms.html',
                pageTitle: 'Terms of Use'
            })

            .state('privacy', {
                url: '/privacy',
                templateUrl: 'terms/privacy.html',
                pageTitle: 'Privacy Policy'
            })

            .state('401', {
                url: '/401',
                templateUrl: '401.html',
                pageTitle: '401'
            })

            .state('404', {
                templateUrl: '404.html',
                pageTitle: '404'
            })

            .state('500', {
                url: '/500',
                templateUrl: '500.html',
                pageTitle: '500'
            })

            .state('passwordReset', {
                url: '/passwordReset?t',
                templateUrl: 'user/passwordReset.html',
                pageTitle: 'Password Reset'
            })

            .state('home', {
                url: '/',
                pageTitle: 'Home'
            })

            .state('owner', {
                url: '/account/:accountName?s',
                templateUrl: 'public/owner.html',
                pageTitle: 'Account'
            })
        ;

        $locationProvider.html5Mode(true);

        $httpProvider.interceptors.push(['$timeout', '$q', '$injector', 'store', 'jwtHelper', '$rootScope',
            function ($timeout, $q, $injector, store, jwtHelper, $rootScope)
        {
            function isLoggedIn()
            {
                return null != store.get('token');
            }

            var $http, $state,
                re = new RegExp('^\/rest\/', 'i'),
                defer,
                token,
                u,
                deferred;

            // this trick must be done so that we don't receive
            // `Uncaught Error: [$injector:cdep] Circular dependency found`
            $timeout(function ()
            {
                $http = $injector.get('$http');
                $state = $injector.get('$state');
            });


            return {

                request: function (config)
                {
                    $rootScope.notAuthorized = false;

                    if(!re.test(config.url)) {
                        defer = $q.defer();
                        config.timeout = defer.promise;
                        return config;
                    }

                    token = store.get('token');
                    if (token) {
                        if (jwtHelper.isTokenExpired(token))
                        {
                            $rootScope.logout();
                        }
                        else
                        {
                            config.headers = config.headers || {};
                            config.headers.Authorization = token;
                            u = store.get('user');
                            if (u)
                                config.headers.CHUser = u.username;
                        }
                    }

                    return config;
                },

                responseError: function (response)
                {
                    switch(response.status)
                    {
                        case 400: // Bad request
                        case 406: // Not acceptable
                            deferred = $q.defer();
                            if (isLoggedIn())
                                $state.go('dashboard');
                            else
                                $state.go('login');
                            return deferred.promise;

                        case 401: // Authorization
                        case 403: // Forbidden
                            deferred = $q.defer();
                            $rootScope.notAuthorized = true;
                            return response;

                        case 404:
                            deferred = $q.defer();
                            $state.go('404');
                            return deferred.promise;

                        case 500:
                            deferred = $q.defer();
                            $state.go('500');
                            return deferred.promise;

                        case 0:
                            deferred = $q.defer();
                            return deferred.promise;
                    }

                    return response;
                }
            };
        }]);
    }])

    .run(['$rootScope', '$state', function ($rootScope, $state)
    {
        $rootScope.notAuthorized = false;
        $rootScope.type = {
            owner: 4,
            admin: 3,
            member: 2,
            demo: 1,
            nonMember: 0
        };

        $rootScope.$on('$stateChangeStart', function (event, toState, toParams, from, fromParams)
        {
            if (from && from.name)
            {
                if (ignoreFrom.indexOf(from.name) === -1)
                {
                    lastFrom = from;
                    lastFromParams = fromParams;
                }
            }
            else
            {
                if (ignoreFrom.indexOf(toState.name) === -1)
                {
                    lastFrom = toState;
                    lastFromParams = toParams;
                }
            }

            var requireLogin = toState.data && toState.data.requireLogin ? true : false,
                loggedIn = $rootScope.isLoggedIn();

            if (requireLogin && !loggedIn) {
                event.preventDefault();
                $state.go('login');
            }

            else if (toState.name === 'home') {
                event.preventDefault();
                if (loggedIn)
                    $state.go('dashboard');
                else
                    $state.go('login');
            }

        });
    }])

    .controller('AppCtrl', ['$http', '$state', '$scope', '$rootScope', 'store', '$interval', 'jwtHelper',
        function ($http, $state, $scope, $rootScope, store, $interval, jwtHelper)
        {
            $scope.closeNotAuthWarning = function() { $rootScope.notAuthorized = false; };
            $scope.tzOffset = moment().utcOffset();

            $rootScope.changeComment;
            $scope.setCommitComment = function(comment)
            {
                $rootScope.changeComment = comment;
            };

            $scope.objKeyPresent = function(obj, key)
            {
                if (!obj) return false;
                return obj.hasOwnProperty(key);
            };

            function refreshToken()
            {
                var token = store.get('token'),
                    expiresOn,
                    u;

                if (token) {

                    try {
                        expiresOn = jwtHelper.getTokenExpirationDate(token);
                        if (!expiresOn || expiresOn.getTime() - (new Date()).getTime() < (432000000)) // 5 days
                        {
                            $http
                                .get('/rest/refreshToken')
                                .then(function(response) {
                                    if (response.data && response.data.token)
                                    {
                                        $scope.processLoginToken(response.data.token);
                                    }

                                });
                        }
                    } catch (err) {
                        console.log(err);
                        $rootScope.logout();
                    }
                }
            }

            $scope.upgrade = null;
            function checkUpgradeAvailability()
            {
                $http
                    .get('/rest/upgradeCheck')
                    .then(function(response) {
                        if (response.data && response.data.hasUpgrade)
                            $scope.upgrade = response.data;
                        else
                            $scope.upgrade = null;
                    });
            }

            refreshToken();
            checkUpgradeAvailability();

            $scope.processLoginToken = function(token) {
                $scope._u = jwtHelper.decodeToken(token);

                store.set('user', $scope._u);
                store.set('token', token);
            };

            // check every hour if token needs to be refreshed
            $interval(function() { refreshToken(); }, 3600000);

            // check every 6 hours if upgrade is available
            $interval(function() { checkUpgradeAvailability(); }, 3600000 * 6);

            $rootScope.isLoggedIn = function ()
            {
                return null != store.get('token');
            };

            $scope._u = store.get('user');
            $scope.user = function () {
                return $scope._u;
            };

            $rootScope.logout = function ()
            {
                store.remove('user');
                store.remove('token');
                $rootScope.globalWarnings = [];
                $state.go('home');
            };

            $rootScope.getLastLocation = function() { return lastFrom; };

            $rootScope.getLastInfo = function() {
                return {
                    name: lastFrom.name,
                    params: lastFromParams
                }
            };

            $rootScope.goToLastLocation = function ()
            {
                if (lastFrom.name) {
                    $state.go(lastFrom.name, lastFromParams);
                    return true;
                }
                return false;
            };
        }])

    .directive('updateTitle', ['$rootScope', '$timeout',
        function($rootScope, $timeout) {
            return {
                link: function(scope, element) {
                    var listener, title;
                    listener = function(event, toState)
                    {
                        title = 'ConfigHub';
                        if (toState.pageTitle)
                            title = 'ConfigHub ' + toState.pageTitle;
                        $timeout(function() { element.text(title); }, 0, false);
                    };

                    $rootScope.$on('$stateChangeSuccess', listener);
                }
            };
        }
    ])

    .directive('myTarget', function () {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                element.attr("target", "_blank");
            }
        };
    });


