(function edward() {
    var workerWrapper = new WorkerWrapper();

    var prefix = '(function(input){';

    var suffix = '})';

    var volunteerId = new Fingerprint().get();

    console.log("VolunteerId: " + volunteerId);

    function jobIdToFunctionName(jobId) {
        return "job" + jobId;
    }

    function processNextTask() {
        $.getJSON("../client/getNextTask/" + volunteerId, function (result) {
            if (!result.inputData) {
                console.log("No tasks received from server.");
                window.setTimeout(processNextTask, 1000);
                return;
            }
            var jobId = result.jobId;
            var inputData = result.inputData;
            var executionId = result.executionId;

            var callbackForWorkerSuccess = function (result) {
                sendResultToServer(result, executionId);
            }

            var callbackForWorkerError = function (error) {
                sendErrorToServer(error, executionId);
            }


            var executeTask = function () {
                workerWrapper.execute(jobIdToFunctionName(jobId),
                    [JSON.parse(inputData)]).then(callbackForWorkerSuccess, callbackForWorkerError);
            }


            if (!workerWrapper[jobIdToFunctionName(jobId)]) {
                $.get("/client/getCode/" + jobId, function (result) {
                    var functionCode = prefix + result + suffix;
                    console.log("Compiling code for job " + jobId);
                    workerWrapper.transferFunctionCode(jobIdToFunctionName(jobId), functionCode).then(executeTask,
                        callbackForWorkerError)
                });
            } else {
                executeTask();
            }
        })
    }

    function sendResultToServer(result, executionId) {
        console.log("Sending execution result to server " + executionId);
        $.ajax({
            type: "POST",
            url: "/client/sendResult/" + executionId,
            data: JSON.stringify(result),
            contentType: "application/json"
        })
        window.setTimeout(processNextTask, 0);
    }

    function sendErrorToServer(error, executionId) {
        $.ajax({
            type: "POST",
            url: "/client/sendError/" + executionId,
            data: JSON.stringify(error),
            contentType: "application/json"
        })
        window.setTimeout(processNextTask, 0);
    }

    window.setTimeout(processNextTask, 0);
}())