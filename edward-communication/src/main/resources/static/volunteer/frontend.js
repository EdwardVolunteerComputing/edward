var baseApiUrl = "../api/volunteer";
$.get(baseApiUrl + "/version", function(data) {$("span#version").text(data);});