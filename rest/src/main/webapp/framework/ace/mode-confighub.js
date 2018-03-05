ace.define('ace/mode/confighub', [], function (require, exports, module)
{

    var oop = require("ace/lib/oop");
    var TextMode = require("ace/mode/text").Mode;
    var Tokenizer = require("ace/tokenizer").Tokenizer;
    var ConfigHubHighlightRules = require("ace/mode/confighub_highlight_rules").ConfigHubHighlightRules;

    var Mode = function ()
    {
        this.HighlightRules = ConfigHubHighlightRules;
    };
    oop.inherits(Mode, TextMode);

    exports.Mode = Mode;
});



ace.define('ace/mode/confighub_highlight_rules', [], function (require, exports, module)
{
    var oop = require("ace/lib/oop");
    var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

    var escapedRe = "\\\\(?:x[0-9a-fA-F]{2}|" + // hex
        "u[0-9a-fA-F]{4}|" + // unicode
        "u{[0-9a-fA-F]{1,6}}|" + // es6 unicode
        "[0-2][0-7]{0,2}|" + // oct
        "3[0-7][0-7]?|" + // oct
        "[4-7][0-7]?|" + //oct
        ".)";

    var ConfigHubHighlightRules = function ()
    {
        this.$rules = {
            "start": [
                {
                    token : "comment",
                    regex : "#.*$"
                },
                {
                    token : "comment",
                    regex : "\\/\\/.*$"
                },
                {
                    token : "text",
                    regex: "http[s]?://"
                },
                {
                    token : "text",
                    regex: "mysql://"
                },
                {
                    token : "text",
                    regex: "[s]?ftp://"
                },
                {
                    token : "comment",
                    regex : "<\\!--",
                    next : "comment"
                },
                {
                    token : "string",
                    regex : '"(?=.)',
                    next  : "qqstring"
                },
                {
                    token: "text",
                    regex: /\\\${/,
                    next: "start"
                },
                {
                    token: "openBracket",
                    regex: /\${/,
                    next: "key"
                }
            ],
            "qqstring" : [
                {
                    token : "constant.language.escape",
                    regex : escapedRe
                }, {
                    token : "string",
                    regex : "\\\\$",
                    next  : "qqstring"
                }, {
                    token : "string",
                    regex : '"|$',
                    next  : "start"
                }, {
                    defaultToken: "string"
                },
                {
                    token: "openBracket",
                    regex: /\${/,
                    next: "in-key"
                }
            ],
            "in-key": [
                {
                    token: "key",
                    regex: "[\\[\\]\(\\)\\*\\-\\._+a-zA-Z0-9]+\\s*",
                    next: "in-closingTags"
                }
            ],
            "in-closingTags": [
                {
                    token: "closeBracket",
                    regex: /}/,
                    next: "qqstring"
                },
                {
                    token: "error",
                    regex: /(^|[^}]).*/,
                    next: "qqstring"
                }
            ],
            "key": [
                {
                    token: "key",
                    regex: "[\\[\\]\(\\)\\*\\-\\._+a-zA-Z0-9]+\\s*",
                    next: "closingTags"
                }
            ],
            "closingTags": [
                {
                    token: "closeBracket",
                    regex: /}/,
                    next: "start"
                },
                {
                    token: "error",
                    regex: /(^|[^}]).*/,
                    next: "start"
                }
            ],
            "comment" : [
                {
                    token : "comment",
                    regex : "-->",
                    next : "start"
                },
                {defaultToken : "comment"}
            ]
        };
        this.normalizeRules()
    };

    oop.inherits(ConfigHubHighlightRules, TextHighlightRules);

    exports.ConfigHubHighlightRules = ConfigHubHighlightRules;
});
