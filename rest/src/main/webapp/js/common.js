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
    .module('configHub.common', [])

    .directive('changeComment', function()
    {
        return {
            restrict: 'EA',
            scope: true,
            controller: ['$scope', '$rootScope', 'focus', '$timeout',
                function ($scope, $rootScope, focus, $timeout)
                {
                    $timeout(function()
                    {
                        focus('changeCommentFieldText');
                    }, 1500);

                    $scope.commitComment = $rootScope.changeComment;
                    $scope.updateComment = function(comment)
                    {
                        $scope.setCommitComment(comment);
                    };
                }
            ]
        }
    })

    .service('accountNameCheckService', ['$http', '$q', function ($http, $q)
    {
        // Return the public API.
        return ({
            cancel: cancel,
            isNameTaken: isNameTaken
        });

        function cancel(promise)
        {
            if (promise &&
                promise._httpTimeout &&
                promise._httpTimeout.resolve) {
                promise._httpTimeout.resolve();
            }
        }

        var httpTimeout,
            request,
            promise;

        function isNameTaken(name)
        {
            httpTimeout = $q.defer();
            request = $http({
                method: 'GET',
                url: '/rest/isNameTaken',
                params: {t: name},
                timeout: httpTimeout.promise
            });

            promise = request.then(unwrapResolve);
            promise._httpTimeout = httpTimeout;
            return ( promise );
        }

        function unwrapResolve(response)
        {
            return ( response.data );
        }
    }])

    .service('repositoryNameCheckService', ['$http', '$q', function ($http, $q)
    {
        // Return the public API.
        return ({
            cancel: cancel,
            isValid: isValid
        });

        function cancel(promise)
        {
            if (promise &&
                promise._httpTimeout &&
                promise._httpTimeout.resolve) {
                promise._httpTimeout.resolve();
            }
        }

        var httpTimeout,
            request,
            promise;

        function isValid(account, name)
        {
            httpTimeout = $q.defer();

            request = $http({
                method: 'GET',
                url: '/rest/isRepoNameValid/' + account,
                params: {t: name},
                timeout: httpTimeout.promise
            });

            promise = request.then(unwrapResolve);
            promise._httpTimeout = httpTimeout;
            return ( promise );
        }

        function unwrapResolve(response)
        {
            return ( response.data );
        }
    }])

    .animation('.slider', function ()
    {
        return {
            enter: function (element, done)
            {
                jQuery(element).hide().slideDown(slideTime);
            },

            leave: function (element, done)
            {
                jQuery(element).slideUp(slideTime);
            },

            removeClass: function(element, className, done)
            {
                if (className === 'ng-hide')
                    jQuery(element).hide().slideDown(slideTime);
            },

            addClass: function(element, className, done) {
                if (className === 'ng-hide')
                    jQuery(element).slideUp(slideTime);
            }
        };
    })

    .animation('.diff-slider', function ()
    {
        return {
            enter: function (element, done)
            {
                jQuery(element).hide().slideDown(0);
            },

            leave: function (element, done)
            {
                jQuery(element).slideUp(0);
            },

            removeClass: function(element, className, done)
            {
                if (className === 'ng-hide')
                    jQuery(element).hide().slideDown(0);
            },

            addClass: function(element, className, done) {
                if (className === 'ng-hide')
                    jQuery(element).slideUp(0);
            }
        };
    })

    .factory('focus', ['$timeout', '$window', function($timeout, $window) {
        return function(id) {
            $timeout(function() {
                var element = $window.document.getElementById(id);
                if(element) element.focus();
            }, 300);
        };
    }])

    .service('toUtc', ['$filter',
        function($filter) {
            var toUtcFilter = $filter('amUtc'),
                m;

            return ({
                convert: convertToUtc,
                toMS: convertToUtcMs
            });

            function convertToUtc(date)
            {
                if (null == date) return '';

                m = moment(date);
                return toUtcFilter(m).format("MM/DD/YYYY hh:mm a");
            }


            function convertToUtcMs(date)
            {
                if (null == date) return null;

                m = moment(date);
                return toUtcFilter(m).valueOf();
            }

        }])

    .service('auth', ['$interval', '$modal', '$q', '$timeout', '$http', 'focus',
        function ($interval, $modal, $q, $timeout, $http, focus)
        {
            var ps = {},
                now,
                username,
                diff,
                deferred,
                confirm,
                parentShow;

            $interval(function() {
                now = Date.now();
                for (username in ps)
                {
                    diff = now - ps[username].ts;
                    if (diff > 600000)
                    {
                        delete ps[username];
                    }
                }
            }, 60000);

            return ({
                authAndExec: authAndExec,
                get: get
            });

            function get(username)
            {
                if (ps[username])
                {
                    ps[username].now = Date.now();
                    return ps[username].secret;
                }
                return null;
            }

            function cache(profile, secret)
            {
                ps[profile] = {
                    secret: secret,
                    ts: Date.now()
                };
            }

            function authAndExec($scope, callback)
            {
                if (get($scope.user().username)) callback(get($scope.user().username));
                else modal($scope, callback).show();
            }

            function modal($scope, callback)
            {
                confirm = $modal({
                    template: '/common/authUser.tpl.html',
                    scope: $scope,
                    show: false,
                    controller: ['$httpParamSerializer', function($httpParamSerializer) {
                        focus('password');

                        $scope.validate = function(password, closer)
                        {
                            $http({
                                method: 'POST',
                                url: '/rest/authenticateUser',
                                data: $httpParamSerializer({ password: password }),
                                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                            }).then(function(response)
                            {
                                if (response.data.success)
                                {
                                    cache($scope.user().username, password);
                                    closer();
                                    callback(password);
                                } else {
                                    if (response.data.message)
                                        $scope.validationMessage = response.data.message;
                                    else
                                        $scope.validationMessage = "Invalid password";
                                }
                            });
                        };
                    }]
                });
                parentShow = confirm.show;

                confirm.show = function() {
                    deferred = $q.defer();
                    $timeout(function () {
                        parentShow();
                    }, 250);
                    return deferred.promise;
                };

                return confirm;
            }
        }])

    .directive('eatClickIf', ['$parse', '$rootScope',
        function ($parse, $rootScope)
        {
            return {
                // this ensure eatClickIf be compiled before ngClick
                priority: 100,
                restrict: 'A',
                compile: function ($element, attr)
                {
                    var fn = $parse(attr.eatClickIf),
                        eventName,
                        callback;

                    return {
                        pre: function link(scope, element)
                        {
                            if(fn(scope))
                                element.addClass("disabled");

                            eventName = 'click';
                            element.on(eventName, function (event)
                            {
                                callback = function ()
                                {
                                    if (fn(scope, {$event: event})) {
                                        // prevents ng-click to be executed
                                        event.stopImmediatePropagation();
                                        // prevents href
                                        event.preventDefault();
                                        return false;
                                    }
                                };
                                if ($rootScope.$$phase) {
                                    scope.$evalAsync(callback);
                                } else {
                                    scope.$apply(callback);
                                }
                            });
                        },
                        post: function () {}
                    }
                }
            }
        }
    ])

    .directive('selectOnClick', ['$window', function ($window) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                element.on('click', function () {

                    var selection = $window.getSelection();
                    var range = document.createRange();
                    range.selectNodeContents(element[0]);
                    selection.removeAllRanges();
                    selection.addRange(range);

                });
            }
        };
    }])

;
