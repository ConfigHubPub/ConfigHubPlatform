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

{
    function escapeRegex(str) {
        return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
    }

    // Splits space-separated search fields into plain OR-matched regexes ("res") and
    // "key=value" AND clauses ("kvClauses" - key must match AND some property value must match).
    function parseFields(fields)
    {
        var res = [],
            kvClauses = [],
            fi,
            field,
            eqIdx,
            keyTerm,
            valueTerm;

        for (fi=0; fi<fields.length; fi++) {
            field = fields[fi];
            eqIdx = field.indexOf('=');

            if (eqIdx >= 0) {
                keyTerm = escapeRegex(field.substring(0, eqIdx).trim());
                valueTerm = escapeRegex(field.substring(eqIdx + 1).trim());

                kvClauses.push({
                    keyRe: keyTerm ? new RegExp(keyTerm, 'i') : null,
                    valueRe: new RegExp(valueTerm, 'i')
                });
            }
            else {
                res.push(new RegExp(escapeRegex(field), 'i'));
            }
        }

        return { res: res, kvClauses: kvClauses };
    }

    function gPropertyFilter(re, property, filtered, toPush)
    {
        if (re.test(property.value)) {
            filtered.push(toPush);
            return true;
        }
        else {
            var li, ciName;

            for (li in depthScores) {
                ciName = property.levels[depthScores[li]];
                if (ciName && re.test(ciName.nm)) {
                    filtered.push(toPush);
                    return true;
                }
            }
        }

        return false;
    }

    angular
        .module('configHub.repository.filters', [])


        .filter('tokenFilter', function()
        {
            return function (tokens, fields)
            {

                if (!fields || fields.length == 0)
                    return tokens;

                var res = [],
                    fi,
                    field,
                    i,
                    re,
                    ti = 0,
                    filtered = [],
                    sp,
                    token;

                for (fi=0; fi<fields.length; fi++) {
                    field = fields[fi].replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
                    res.push(new RegExp(field, 'i'));
                }

                for (; ti<tokens.length; ti++)
                {
                    token = tokens[ti];
                    for (i in res) {
                        re = res[i];

                        if (re.test(token.token) ||
                            re.test(token.name) ||
                            re.test(token.rulesTeam) ||
                            re.test(token.managingTeam) ||
                            re.test(token.user)) {
                            filtered.push(token);
                            break;
                        }
                        else if (token.sps && token.sps.length > 0) {
                            for (sp in token.sps) {
                                if (re.test(token.sps[sp])) {
                                    filtered.push(token);
                                    break;
                                }
                            }
                        }
                    }
                }


                return filtered;
            }
        })

        .filter('propertyFilter', function ()
        {
            return function (propertyList, fields, entry, localSearch)
            {
                if (!localSearch || !fields || fields.length == 0)
                    return propertyList;

                var parsed = parseFields(fields),
                    res = parsed.res,
                    kvClauses = parsed.kvClauses,
                    added = false,
                    i,
                    re,
                    clause,
                    filtered = [],
                    pi = 0,
                    property;

                if (entry.newProperty)
                    added = true;
                else
                {
                    for (i in res) {
                        re = res[i];
                        if (re.test(entry.key) || re.test(entry[1].readme) || re.test(entry[1].spName))
                            added = true;
                    }
                }

                if (added)
                    return propertyList;

                for (; pi<propertyList.length; pi++)
                {
                    property = propertyList[pi];
                    for (i in res)
                    {
                        re = res[i];
                        if (gPropertyFilter(re, property, filtered, property))
                            break;
                    }
                }

                for (i = 0; i < kvClauses.length; i++)
                {
                    clause = kvClauses[i];

                    if (clause.keyRe && !clause.keyRe.test(entry.key))
                        continue;

                    for (pi = 0; pi<propertyList.length; pi++)
                    {
                        property = propertyList[pi];
                        if (filtered.indexOf(property) < 0 && clause.valueRe.test(property.value))
                            filtered.push(property);
                    }
                }

                return filtered;
            }
        })

        .filter('cmpPropertyFilter', function ()
        {
            return function (propertyList, fields, entry, localSearch)
            {
                if (!localSearch || !fields || fields.length == 0)
                    return propertyList;

                var parsed = parseFields(fields),
                    res = parsed.res,
                    kvClauses = parsed.kvClauses,
                    added = false,
                    i,
                    re,
                    clause,
                    filtered = [],
                    pi,
                    pair;

                for (i in res)
                {
                    re = res[i];
                    if (re.test(entry.key) ||
                        (entry[0] && re.test(entry[0].readme)) ||
                        (entry[0] && re.test(entry[0].spName)) ||
                        (entry[2] && re.test(entry[2].readme)) ||
                        (entry[2] && re.test(entry[2].spName)))
                    {
                        added = true;
                        break;
                    }
                }

                if (added)
                    return propertyList;

                for (pi=0; pi<propertyList.length; pi++)
                {
                    pair = propertyList[pi];

                    for (i in res)
                    {
                        re = res[i];

                        if (!added) {
                            if (!added && pair[0]) added = gPropertyFilter(re, pair[0], filtered, pair);
                            if (!added && pair[2]) added = gPropertyFilter(re, pair[2], filtered, pair);
                        }
                    }
                }

                for (i = 0; i < kvClauses.length; i++)
                {
                    clause = kvClauses[i];

                    if (clause.keyRe && !clause.keyRe.test(entry.key))
                        continue;

                    for (pi = 0; pi<propertyList.length; pi++)
                    {
                        pair = propertyList[pi];
                        if (filtered.indexOf(pair) < 0 &&
                            ((pair[0] && clause.valueRe.test(pair[0].value)) ||
                             (pair[2] && clause.valueRe.test(pair[2].value))))
                            filtered.push(pair);
                    }
                }

                return filtered;
            }
        })

        .filter('keyFilter', function ()
        {
            function escapeRegex(str) {
                return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
            }

            return function (items, fields, localSearch)
            {
                if (!localSearch || !fields || fields.length == 0)
                    return items;

                var res = [],
                    kvClauses = [],
                    fi,
                    field,
                    eqIdx,
                    keyTerm,
                    valueTerm,
                    filtered = [],
                    added = false,
                    it = 0,
                    item,
                    i,
                    re,
                    pi = 0,
                    property;


                for (fi=0; fi<fields.length; fi++) {
                    field = fields[fi];
                    eqIdx = field.indexOf('=');

                    // "key=value" syntax: require the key to match AND some property value to match
                    if (eqIdx >= 0) {
                        keyTerm = escapeRegex(field.substring(0, eqIdx).trim());
                        valueTerm = escapeRegex(field.substring(eqIdx + 1).trim());

                        kvClauses.push({
                            keyRe: keyTerm ? new RegExp(keyTerm, 'i') : null,
                            valueRe: new RegExp(valueTerm, 'i')
                        });
                    }
                    else {
                        res.push(new RegExp(escapeRegex(field), 'i'));
                    }
                }


                for (; it<items.length; it++)
                {
                    item = items[it];
                    added = false;

                    for (i in res)
                    {
                        re = res[i];
                        if (re.test(item.key) || re.test(item[1].readme) || re.test(item[1].spName)) {
                            filtered.push(item);
                            added = true;
                            break;
                        }
                    }

                    if (added) continue;

                    for (i in res)
                    {
                        re = res[i];
                        for (pi = 0; pi<item.properties.length; pi++)
                        {
                            property = item.properties[pi];
                            added = gPropertyFilter(re, property, filtered, item);
                            if (added) break;
                        }
                        if (added) break;
                    }

                    if (added) continue;

                    for (i = 0; i < kvClauses.length; i++)
                    {
                        var clause = kvClauses[i];

                        if (clause.keyRe && !clause.keyRe.test(item.key))
                            continue;

                        for (pi = 0; pi<item.properties.length; pi++)
                        {
                            if (clause.valueRe.test(item.properties[pi].value)) {
                                filtered.push(item);
                                added = true;
                                break;
                            }
                        }

                        if (added) break;
                    }
                }

                return filtered;
            }
        })

        .filter('fileKeyFilter', function ()
        {
            return function (items, localSearch, fileTokens)
            {
                if (!localSearch || !fileTokens) return items;
                return items.filter(function (item) {
                    return fileTokens.hasOwnProperty(item.key);
                });
            };
        })

        .filter('cmpKeyFilter', function ()
        {
            return function (items, fields, localSearch)
            {
                if (!localSearch || !fields || fields.length == 0)
                    return items;

                var res = [],
                    fi = 0,
                    field,
                    filtered = [],
                    added = false,
                    index = 0,
                    item,
                    i,
                    re,
                    pi = 0,
                    pair;


                for (; fi<fields.length; fi++) {
                    field = fields[fi].replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
                    res.push(new RegExp(field, 'i'));
                }

                for (; index<items.length; index++)
                {
                    item = items[index];
                    added = false;

                    for (i in res)
                    {
                        re = res[i];
                        if (re.test(item.key) ||
                            (item[0] && re.test(item[0].readme)) ||
                            (item[0] && re.test(item[0].spName)) ||
                            (item[2] && re.test(item[2].readme)) ||
                            (item[2] && re.test(item[2].spName))) {
                            filtered.push(item);
                            added = true;
                            break;
                        }
                    }

                    if (added) continue;

                    for (i in res) {
                        re = res[i];

                        for (; pi<item.properties.length; pi++)
                        {
                            pair = item.properties[pi];

                            if (!added && pair[0]) added = gPropertyFilter(re, pair[0], filtered, item);
                            if (!added && pair[2]) added = gPropertyFilter(re, pair[2], filtered, item);
                        }
                    }

                }

                return filtered;
            }
        })


        .filter('orderMixObjectBy', function ()
        {
            return function (items, field, reverse)
            {
                var filtered = [];
                angular.forEach(items, function (item) {
                    filtered.push(item);
                });
                filtered.sort(function (a, b) {
                    if (!a || !a[field]) return -1;
                    return (a[field] > b[field] ? 1 : -1);
                });
                if (reverse) filtered.reverse();
                return filtered;

            };
        })

        .filter('orderObjectBy', function ()
        {
            return function (items, field, reverse)
            {
                var filtered = [];
                angular.forEach(items, function (item) {
                    filtered.push(item);
                });
                filtered.sort(function (a, b) {
                    if (!a || !a[field]) return -1;
                    return (a[field].toLowerCase() > b[field].toLowerCase() ? 1 : -1);
                });
                if (reverse) filtered.reverse();
                return filtered;

            };
        })

        .filter('orderObjectByInt', function ()
        {
            return function (items, field, reverse)
            {
                var filtered = [];
                angular.forEach(items, function (item) {
                    filtered.push(item);
                });
                filtered.sort(function (a, b) {
                    if (!a || !a[field]) return -1;
                    return (a[field] > b[field] ? 1 : -1);
                });
                if (reverse) filtered.reverse();
                return filtered;

            };
        })
    ;
}