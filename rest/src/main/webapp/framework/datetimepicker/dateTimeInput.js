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

/*globals angular, moment, jQuery */
/*jslint vars:true */

/**
 * @license angular-date-time-input  v0.1.0
 * (c) 2013 Knight Rider Consulting, Inc. http://www.knightrider.com
 * License: MIT
 */

/**
 *
 *    @author Dale "Ducky" Lotts
 *    @since  2013-Sep-23
 */

angular.module('ui.dateTimeInput', []).directive('dateTimeInput',
  [
    function () {
      "use strict";
      return {
        require: 'ngModel',
        restrict: 'A',
        link: function (scope, element, attrs, controller) {

          if (!attrs.dateTimeInput) {
            throw ("dateTimeInput must specify a date format");
          }

          var validateFn = function (viewValue) {

            var result = viewValue;

            if (viewValue) {
              var momentValue = moment(viewValue);
              if (momentValue.isValid()) {
                controller.$setValidity(attrs.ngModel, true);
                result = momentValue.format();
              } else {
                controller.$setValidity(attrs.ngModel, false);
              }
            }

            return result;
          };


          var formatFn = function (modelValue) {
            var result = modelValue;
            if (modelValue && moment(modelValue).isValid()) {
              result = moment(modelValue).format(attrs.dateTimeInput);
            }
            return result;
          };

          controller.$parsers.unshift(validateFn);

          controller.$formatters.push(formatFn);

          element.bind('blur', function () {
            var viewValue = controller.$modelValue;
            angular.forEach(controller.$formatters, function (formatter) {
              viewValue = formatter(viewValue);
            });
            controller.$viewValue = viewValue;
            controller.$render();
          });
        }
      };
    }
  ]);
