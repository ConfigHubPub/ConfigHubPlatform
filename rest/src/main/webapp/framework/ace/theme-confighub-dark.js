ace.define("ace/theme/confighub-dark",["require","exports","module","ace/lib/dom"], function(require, exports, module) {

    exports.isDark = false;
    exports.cssClass = "ace-confighub-dark";
    exports.cssText = "\
.ace-confighub-dark .ace_gutter {\
background: rgba(255,255,255,0.05);\
color: rgba(255,255,255,0.15);\
font-size: 11px !important; \
}\
.ace-confighub-dark  {\
background-color: #2b303b; \
border-top: 1px solid #1b1c1c;\
color: #eaeadf;\
font-family: Consolas, 'Liberation Mono', Menlo, Courier, monospace; \
font-size: 12px !important; \
}\
.ace-confighub-dark .ace_key {\
color: #e58c4d;\
}\
.ace-confighub-dark .ace_openBracket, .ace-confighub-dark .ace_closeBracket {\
color: #0086b3;\
}\
.ace-confighub-dark .ace_error {\
color: #f00;\
font-weight: 600;\
}\
.ace-confighub-dark .ace_string {\
color: #a3afbd;\
}\
.ace-confighub-dark .ace_comment {\
color: #7a7a7a;\
}\
.ace-confighub-dark .ace_marker-layer .ace_selection {\
background: rgba(255,255,255,0.2) !important;\
}\
.ace-confighub-dark.ace_multiselect .ace_selection.ace_start {\
box-shadow: 0 0 3px 0px white;\
}\
.ace-confighub-dark.ace_nobold .ace_line > span {\
font-weight: normal !important;\
}\
.ace-confighub-dark .ace_marker-layer .ace_step {\
background: rgb(252, 255, 0);\
}\
.ace-confighub-dark .ace_marker-layer .ace_stack {\
background: rgb(164, 229, 101);\
}\
.ace-confighub-dark .ace_marker-layer .ace_bracket {\
margin: -1px 0 0 -1px;\
border: 1px solid rgba(255, 255, 255, 0.02);\
}\
.ace-confighub-dark .ace_gutter-active-line {\
background-color : rgba(255, 255, 255, 0.07);\
}\
.ace-confighub-dark .ace_marker-layer .ace_selected-word {\
background: rgb(255, 255, 255);\
border: 1px solid rgb(200, 200, 250);\
}\
.ace-confighub-dark .ace_invisible {\
color: #BFBFBF\
}\
.ace-confighub-dark .ace_print-margin {\
width: 1px;\
background: rgba(255,255,255,0.05);\
}\
.ace-github .ace_cursor {\
color: #fff;\
}\
.ace-confighub-dark .ace_indent-guide {\
background: url(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAECAYAAABP2FU6AAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAA6ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IlhNUCBDb3JlIDUuNC4wIj4KICAgPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICAgICAgPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIKICAgICAgICAgICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgICAgICAgICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iPgogICAgICAgICA8eG1wOk1vZGlmeURhdGU+MjAxNi0wNi0wMVQxOTowNjo5MjwveG1wOk1vZGlmeURhdGU+CiAgICAgICAgIDx4bXA6Q3JlYXRvclRvb2w+UGl4ZWxtYXRvciAzLjQuNDwveG1wOkNyZWF0b3JUb29sPgogICAgICAgICA8dGlmZjpPcmllbnRhdGlvbj4xPC90aWZmOk9yaWVudGF0aW9uPgogICAgICAgICA8dGlmZjpDb21wcmVzc2lvbj41PC90aWZmOkNvbXByZXNzaW9uPgogICAgICAgICA8dGlmZjpSZXNvbHV0aW9uVW5pdD4yPC90aWZmOlJlc29sdXRpb25Vbml0PgogICAgICAgICA8dGlmZjpZUmVzb2x1dGlvbj43MjwvdGlmZjpZUmVzb2x1dGlvbj4KICAgICAgICAgPHRpZmY6WFJlc29sdXRpb24+NzI8L3RpZmY6WFJlc29sdXRpb24+CiAgICAgICAgIDxleGlmOlBpeGVsWERpbWVuc2lvbj4xPC9leGlmOlBpeGVsWERpbWVuc2lvbj4KICAgICAgICAgPGV4aWY6Q29sb3JTcGFjZT4xPC9leGlmOkNvbG9yU3BhY2U+CiAgICAgICAgIDxleGlmOlBpeGVsWURpbWVuc2lvbj40PC9leGlmOlBpeGVsWURpbWVuc2lvbj4KICAgICAgPC9yZGY6RGVzY3JpcHRpb24+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+Ck+ats0AAAASSURBVAgdY/j//78aEwMQIAgAOHoDKjanhr8AAAAASUVORK5CYII=\") right repeat-y;\
}";

    var dom = require("../lib/dom");
    dom.importCssString(exports.cssText, exports.cssClass);
});