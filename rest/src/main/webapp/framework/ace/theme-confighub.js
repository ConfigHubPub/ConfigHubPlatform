ace.define("ace/theme/confighub",["require","exports","module","ace/lib/dom"], function(require, exports, module) {

    exports.isDark = false;
    exports.cssClass = "ace-confighub";
    exports.cssText = "\
.ace-confighub .ace_gutter {\
background: rgba(0,0,0,0.03);\
color: rgba(0,0,0,0.15);\
font-size: 11px !important; \
}\
.ace-confighub  {\
background-color: #f3f5f6;\
border-top: 1px solid #e5e5e5;\
color: #3a464e;\
font-family: Consolas, 'Liberation Mono', Menlo, Courier, monospace; \
font-size: 12px !important; \
}\
.ace-confighub .ace_key {\
color: #df5000;\
}\
.ace-confighub .ace_openBracket, .ace-confighub .ace_closeBracket {\
color: #0086b3;\
}\
.ace-confighub .ace_error {\
color: #f00;\
font-weight: 600;\
}\
.ace-confighub .ace_string {\
color: #395391;\
}\
.ace-confighub .ace_comment {\
color: #969896;\
}\
.ace-confighub .ace_marker-layer .ace_selection {\
background: rgb(181, 213, 255);\
}\
.ace-confighub.ace_multiselect .ace_selection.ace_start {\
box-shadow: 0 0 3px 0px white;\
}\
.ace-confighub.ace_nobold .ace_line > span {\
font-weight: normal !important;\
}\
.ace-confighub .ace_marker-layer .ace_step {\
background: rgb(252, 255, 0);\
}\
.ace-confighub .ace_marker-layer .ace_stack {\
background: rgb(164, 229, 101);\
}\
.ace-confighub .ace_marker-layer .ace_bracket {\
margin: -1px 0 0 -1px;\
border: 1px solid rgba(0,0,0, 0.02);\
}\
.ace-confighub .ace_gutter-active-line {\
background-color : rgba(0,0,0, 0.07);\
}\
.ace-confighub .ace_marker-layer .ace_selected-word {\
background: #fecb3b;\
}\
.ace-confighub .ace_invisible {\
color: #BFBFBF\
}\
.ace-confighub .ace_print-margin {\
width: 1px;\
background: rgba(0,0,0,0.05);\
}\
.ace-github .ace_cursor {\
color: #fff;\
}\
.ace-confighub .ace_indent-guide {\
background: url(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAE0lEQVQImWP4////f4bLly//BwAmVgd1/w11/gAAAABJRU5ErkJggg==\") right repeat-y;\
}";

    var dom = require("../lib/dom");
    dom.importCssString(exports.cssText, exports.cssClass);
});