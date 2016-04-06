
var BASE_API_URL = "api/internal/";

var USER = "admin";
var PASSWORD = "admin";


var authorizationHeadersObject = {"Authorization": "Basic " + btoa(USER + ":" + PASSWORD)};

var httpGet = function(url){
    return $.ajax({
        type: "GET", url: url, dataType: 'json', headers: authorizationHeadersObject
    });
}

var httpPost = function(url, dataObject){
    return $.ajax({
        type: "POST",
        url: url,
        dataType: 'json',
        data: JSON.stringify(dataObject),
        contentType: 'application/json',
        headers: authorizationHeadersObject
    });
}

var getProject = function (id) {
    return httpGet(BASE_API_URL + "project/" + id);
}

var getNumberOfConnectedVolunteers = function(){
    return httpGet(BASE_API_URL + "volunteerCount/");
}

var getVersion = function(){
    return httpGet(BASE_API_URL + "version/");
}

var getJob = function (id) {
    return httpGet(BASE_API_URL + "job/" + id);
}


var getTask = function (id) {
    return httpGet(BASE_API_URL + "task/" + id)
    .then(function (task) {
        return httpGet(BASE_API_URL + "task/" + id + "/status")
        .then(function (status) {
            task.status = status;
            return task;
        })
    })
}


var getExecution = function (id) {
    return httpGet(BASE_API_URL + "execution/" + id)
}
var getExecutionResult = function (id) {
    return httpGet(BASE_API_URL + "execution/" + id + "/result")
}

var getData = function (id) {
    return httpGet(BASE_API_URL + "data/" + id)
}

var getProjects = function () {
    return httpGet(BASE_API_URL + "project")
}

var putProject = function (project) {
    return httpPost(BASE_API_URL + "project", project);
}


var putJob = function (job) {
    return httpPost(BASE_API_URL + "job", job);
}

var putTasks = function (jobId, inputs, priority, concurrentExecutionsCount, timeout) {
    return httpPost(BASE_API_URL + "job/" + jobId + '/tasks/' + priority + "/" + concurrentExecutionsCount + "/" + timeout, JSON.parse(inputs));
}


