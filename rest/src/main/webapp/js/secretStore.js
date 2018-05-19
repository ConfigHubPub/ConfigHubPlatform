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
    .module('configHub.repository.secretStore', [])

    .service('secretService', ['$interval', '$modal', '$q', '$timeout', '$http', 'focus', 'toUtc',
        function ($interval, $modal, $q, $timeout, $http, focus, toUtc)
    {
        var ps = {},
            now,
            profile,
            diff,
            k,
            p1, p2,
            deferred,
            confirm,
            parentShow;

        $interval(function() {
            now = Date.now();
            for (profile in ps)
            {
                diff = now - ps[profile].ts;
                if (diff > 600000)
                {
                    delete ps[profile];
                }
            }
        }, 60000);

        return ({
            cache: cache,
            get: get,
            clear: clear,
            getSKModal: getSKModal,
            updateSPPassword: updateSPPassword,
            authAndExec: authAndExec,
            authAndExecAudit: authAndExecAudit,
            authSwitchAndExec: authSwitchAndExec,
            authSwitchAndExecAudit: authSwitchAndExecAudit,
            getAll: getAll
        });

        function getAll()
        {
            return ps;
        }

        function clear(profile)
        {
            if (ps[profile])
                delete ps[profile];
        }

        function cache(profile, secret, date)
        {
            k = date ? profile + "_" + date : profile;
            ps[k] = {
                secret: secret,
                ts: Date.now()
            };
        }

        function get(profile, date)
        {
            k = date ? profile + "_" + date : profile;
            if (ps[k])
            {
                ps[k].now = Date.now();
                return ps[k].secret;
            }
            return null;
        }

        function authSwitchAndExec($scope, profile1, profile2, callback)
        {
            p1 = get(profile1);
            p2 = get(profile2);

            if (p1 && p2) callback();
            else if (p1 && !p2)
                getSKModal($scope, null, profile2, callback).show();
            else if (!p1 && p2)
                getSKModal($scope, null, profile1, callback).show();
            else
            {
                getSKModal($scope, null, profile1, function() {
                    getSKModal($scope, null, profile2, callback).show();
                }).show();
            }
        }

        function authSwitchAndExecAudit($scope, profile1, profile2, date, callback)
        {
            p1 = get(profile1, date);
            p2 = get(profile2, date);

            if (p1 && p2) callback();
            else if (p1 && !p2)
                getSKModal($scope, date, profile2, callback).show();
            else if (!p1 && p2)
                getSKModal($scope, date, profile1, callback).show();
            else
            {
                getSKModal($scope, date, profile1, function() {
                    getSKModal($scope, date, profile2, callback).show();
                }).show();
            }
        }

        function authAndExec($scope, date, profile, callback, cancelCallback)
        {
            if (!profile) callback();
            else if (get(profile, date)) callback();
            else getSKModal($scope, date, profile, callback, cancelCallback).show();
        }

        function authAndExecAudit($scope, ts, profile, callback)
        {
            authAndExecAuditModel($scope, ts, profile, callback).show();
        }

        function authAndExecAuditModel($scope, ts, profile, callback)
        {
            $scope.profile = profile;

            confirm = $modal({
                template: '/repo/security/confirm.tpl.html',
                scope: $scope,
                show: false,
                controller: ['$httpParamSerializer', function($httpParamSerializer) {
                    focus('spPassInput');
                    $scope.validate = function(secretKey, closer)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/validateSPSecret/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                ts: ts,
                                spName: profile,
                                spPassword: secretKey
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function(response)
                        {
                            $scope.decodeMessage = response.data.message;
                            if (response.data.success)
                            {
                                closer();
                                callback(secretKey);
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

        function updateSPPassword($scope, profile, callback)
        {
            $scope.profile = profile;

            confirm = $modal({
                template: '/repo/security/securityGroupPasswordOverride.tpl.html',
                scope: $scope,
                show: false,
                controller: ['$httpParamSerializer', function($httpParamSerializer) {
                    focus('currentPassword');
                    $scope.updatePassword = function(currentPassword, newPassword1, newPassword2, ownerPass, closer)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/updateSPPassword/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                groupName: profile,
                                currentPass: currentPassword,
                                newPassword1: newPassword1,
                                newPassword2: newPassword2,
                                ownerPass: ownerPass
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function(response)
                        {
                            $scope.decodeMessage = response.data.message;
                            if (response.data.success)
                            {
                                closer();
                                callback();
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

        function getSKModal($scope, date, profile, callback, cancelCallback)
        {
            $scope.profile = profile;

            confirm = $modal({
                template: '/repo/security/confirm.tpl.html',
                scope: $scope,
                show: false,
                onHide: function() {
                    if (cancelCallback && !get(profile, date)) {
                        cancelCallback();
                    }
                },
                controller: ['$httpParamSerializer', function($httpParamSerializer) {
                    focus('spPassInput');
                    $scope.validate = function(secretKey, closer)
                    {
                        $http({
                            method: 'POST',
                            url: '/rest/validateSPSecret/' + $scope.account + "/" + $scope.repoName,
                            data: $httpParamSerializer({
                                ts: toUtc.toMS(date),
                                spName: profile,
                                spPassword: secretKey
                            }),
                            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                        }).then(function(response)
                        {
                            $scope.decodeMessage = response.data.message;
                            if (response.data.success)
                            {
                                // passwordValidated = true;
                                cache(profile, secretKey, date);
                                closer();
                                callback();
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

;