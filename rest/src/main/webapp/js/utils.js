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

function indexOf(arr, field, val)
{
    for (var i = 0; i < arr.length; i++)
        if (arr[i][field] === val) return i;
    return -1;
}

function indexOfEntry(arr, key)
{
    for (var i = 0; i < arr.length; i++)
        if (arr[i].key === key) return i;
    return -1;
}

function indexOfProperty(arr, obj)
{
    for (var i = 0; i < arr.length; i++)
        if (arr[i].id === obj.id) return i;
    return -1;
}

function indexOfCmpProperty(arr, obj, side)
{
    for (var i = 0; i < arr.length; i++)
        if (arr[i][side] && obj && arr[i][side].id === obj.id) return i;

    return -1;
}

function diff(A, B)
{
    var _a = [],
        _b = [];
    angular.forEach(A, function (item) { _a.push(item.toLowerCase()); });
    angular.forEach(B, function (item) { _b.push(item.toLowerCase()); });

    return _a.filter(function (a)
    {
        return _b.indexOf(a) == -1;
    });
}

Object.size = function (obj)
{
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

var tsFormat = 'ddd, MMM DD YYYY, hh:mm a',
    genRuleProcessTypes = {
        'false': 'Process all rules, last match determines access',
        'true': 'Stop processing rules on a first match'
    },

    tagSelectConfig = {
        create: false,
        valueField: 'name',
        labelField: 'name',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '--',
        sortField: {field: 'name', direction: 'asc'},
        maxItems: 1,
        closeAfterSelect: true,
        openOnFocus: true
    },

    contextSelectConfig = {
        create: false,
        plugins: ['remove_button'],
        valueField: 'name',
        labelField: 'name',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '*',
        openOnFocus: true,
        sortField: 'name',
        render: {
            item: function (item, escape)
            {
                return '<div>' +
                    (item.type
                        ? (item.type == 'Group'
                        ? '<span class="lgroup node"></span>'
                        : '<span class="lmember node"></span>')
                        : "") +
                    (escape(item.name)) +
                    '</div>';
            },
            option: function (item, escape)
            {
                return '<div>' +
                    (escape(item.name)) +
                    (item.type
                        ? (item.type == 'Group'
                        ? '<span class="pull-right lgroup drop"></span>'
                        : '<span class="pull-right lmember drop"></span>')
                        : "") +
                    '</div>';
            }
        }
    },

    propContextSelectConfigNoEdit = {
        create: false,
        valueField: 'name',
        labelField: 'name',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '*',
        closeAfterSelect: true,
        openOnFocus: true,
        sortField: 'name',
        maxItems: 1,
        render: {
            item: function (item, escape)
            {
                return '<div>' +
                    (item.type
                        ? (item.type == 'Group'
                            ? '<span class="lgroup single"></span>'
                            : '<span class="lmember single"></span>')
                        : "") +
                    (escape(item.name)) +
                    '</div>';
            },
            option: function (item, escape)
            {
                return '<div>' +
                    (escape(item.name)) +
                    (item.type
                        ? (item.type == 'Group'
                            ? '<span class="pull-right lgroup inline"></span>'
                            : '<span class="pull-right lmember inline"></span>')
                        : "") +
                    '</div>';
            }
        }
    },

    propContextSelectConfig = {
        create: true,
        valueField: 'name',
        labelField: 'name',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '*',
        closeAfterSelect: true,
        openOnFocus: true,
        sortField: 'name',
        maxItems: 1,
        render: {
            item: function (item, escape)
            {
                return '<div>' +
                    (item.type
                        ? (item.type == 'Group'
                        ? '<span class="lgroup single"></span>'
                        : '<span class="lmember single"></span>')
                        : "") +
                    (escape(item.name)) +
                    '</div>';
            },
            option: function (item, escape)
            {
                return '<div>' +
                    (escape(item.name)) +
                    (item.type
                        ? (item.type == 'Group'
                        ? '<span class="pull-right lgroup inline"></span>'
                        : '<span class="pull-right lmember inline"></span>')
                        : "") +
                    '</div>';
            }
        }
    },

    dts = [
        {value: 'Text', name: 'Text'},
        {sep: true},
        {value: 'Boolean', name: 'Boolean'},
        {value: 'Integer', name: 'Integer'},
        {value: 'Long', name: 'Long'},
        {value: 'Double', name: 'Double'},
        {value: 'Float', name: 'Float'},
        {sep: true},
        {value: 'FileRef', name: 'File Reference'},
        {value: 'FileEmbed', name: 'File Embed'},
        {sep: true},
        {value: 'JSON', name: 'JSON'},
        {value: 'Code', name: 'Code/ML'},
        {sep: true},
        {value: 'Map', name: 'Map'},
        {value: 'List', name: 'List'}
    ],

    DTConfig = {
        create: false,
        valueField: 'value',
        labelField: 'name',
        optgroupField: 'class',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '',
        closeAfterSelect: false,
        openOnFocus: true,
        maxItems: 1,
        optgroups: [
            {value: 'Basic', label: 'Basic'},
            {value: 'Numbers', label: 'Numbers'},
            {value: 'Data Structures', label: 'Data Structures'}
        ],
        render: {
            optgroup_header: function (data, escape)
            {
                return '<div class="optgroup-header">' + escape(data.label) + '</div>';
            }
        }
    },

    ciphers = [
        {'title': 'No encryption',             'cipher': 'None' },
        {'title': 'AES/CBC/PKCS5Padding',      'cipher': 'AES/CBC/PKCS5Padding'},
        {'title': 'AES/ECB/PKCS5Padding',      'cipher': 'AES/ECB/PKCS5Padding'},
        {'title': 'DES/CBC/PKCS5Padding',      'cipher': 'DES/CBC/PKCS5Padding'},
        {'title': 'DES/ECB/PKCS5Padding',      'cipher': 'DES/ECB/PKCS5Padding'},
        {'title': 'DESede/CBC/PKCS5Padding',   'cipher': 'DESede/CBC/PKCS5Padding'},
        {'title': 'DESede/ECB/PKCS5Padding',   'cipher': 'DESede/ECB/PKCS5Padding'}
    ],
    
    CipherConfig = {
        create: false,
        valueField: 'cipher',
        labelField: 'title',
        searchField: ['title'],
        closeAfterSelect: false,
        openOnFocus: true,
        maxItems: 1
    },

    SGConfig = {
        create: false,
        valueField: 'name',
        labelField: 'name',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '',
        closeAfterSelect: false,
        openOnFocus: true,
        sortField: 'name',
        maxItems: 1,
        render: {
            item: function (item, escape)
            {
                return '<div>' +
                    ('<span class="spnl">' + escape(item.name) + '</span>') +
                    (item.cipher ? '<span class="spcl">| <i class="fa fa-lock"></i> ' + escape(item.cipher) + '</span>' : '<span class="spcl">| Encryption: disabled</span>') +
                    '</div>';
            },
            option: function (item, escape)
            {
                var label = item.name,
                    cipher = item.cipher ? item.cipher : null;

                return '<div>' +
                    '<span class="spnl">' + escape(label) + '</span>' +
                    (cipher ? '<span class="spcl">| <i class="fa fa-lock"></i> ' + escape(cipher) + '</span>' : '<span class="spcl">| Encryption: disabled</span>') +
                    '</div>';
            }
        }
    },

    d0 = {score: 5120, label: 'Instance'},
    d1 = {score: 2560, label: 'Application'},
    d2 = {score: 1280, label: 'Environment'},
    d3 = {score: 640, label: 'Product'},
    d4 = {score: 320, label: 'Enterprise'},
    d5 = {score: 160, label: 'D5'},
    d6 = {score: 80, label: 'D6'},
    d7 = {score: 40, label: 'D7'},
    d8 = {score: 20, label: 'D8'},
    d9 = {score: 10, label: 'D9'},


    depthScores = [10, 20, 40, 80, 160, 320, 640, 1280, 2560, 5120],
    scores = [5120, 2560, 1280, 640, 320, 160, 80, 40, 20, 10],
    depths = {
        5120: [d0],
        2560: [d1,d0],
        1280: [d2,d1,d0],
        640:  [d3,d2,d1,d0],
        320:  [d4,d3,d2,d1,d0],
        160:  [d5,d4,d3,d2,d1,d0],
        80:   [d6,d5,d4,d3,d2,d1,d0],
        40:   [d7,d6,d5,d4,d3,d2,d1,d0],
        20:   [d8,d7,d6,d5,d4,d3,d2,d1,d0],
        10:   [d9,d8,d7,d6,d5,d4,d3,d2,d1,d0]
    },
    IntType = new RegExp(/^-?\d+$/),
    DecType = new RegExp(/^-?\d{0,}(\.\d{0,}){0,1}$/),
    slideTime = 250,

    ////////////
    // Errors //
    ////////////

    nameError =
        "Name has to begin with a number or a letter. It may contain non-repeating " +
        "period (.), dash (-) or a underscore (_), and it must end with a number or a letter.",

    keyError =
        "Key can be made of letters, numbers and following characters: []()*_+-.",

    keyCannotChange =
        "Access rules assigned for the team you belong to, " +
        "prevent you from editing this key.",

    scopeLimit = 10;

function contextParam(context)
{
    var contextElements = [],
        score;

    for (score in context) {
        if (context[score] && context[score].length > 0)
            contextElements.push(score + ':' + context[score]);
    }
    return contextElements.join(";");
}

function fullContextToHTML(context)
{
    var contextElements = [],
        score;

    for (score in context) {
        if (context[score] && context[score].length > 0)
            contextElements.push(context[score]);
    }
    return contextElements.join("<i></i>");
}

function sameArrays(a, b, orderBy)
{
    if (!a && !b) return true;
    if ((a && !b) || (!a && b)) return false;
    if (a.length != b.length) return false;

    a = orderBy(a);
    b = orderBy(b);

    return angular.equals(a, b);
}