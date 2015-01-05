(function edward() {
    var workerWrapper = new WorkerWrapper();

    var volunteerId = new Fingerprint().get();

    console.log("VolunteerId: " + volunteerId);


    function jobIdToFunctionName(jobId) {
        return "job" + jobId;
    }


    function getIntervalByLastInterval(oldInterval){
        if(oldInterval === 0) {
            return 500;
        }else{
            return Math.min(10000, oldInterval*1.1);
        }
    }


    var checkInterval = 0;
    function scheduleProcessing(isImmediate){
        if(isImmediate){
            checkInterval = 0;
        }else{
            checkInterval = getIntervalByLastInterval(checkInterval);
        }
        window.setTimeout(processNextTask, checkInterval);
    }


    function processNextTask() {
        $.getJSON("../client/getNextTask/" + volunteerId, function (result) {
            if (!result.inputData) {
                console.log("No tasks received from server.");
                scheduleProcessing(false);
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
                    var functionCode = "function() { \n " + result + "\n; return compute}()";
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
        scheduleProcessing(true);
    }

    function sendErrorToServer(error, executionId) {
        $.ajax({
            type: "POST",
            url: "/client/sendError/" + executionId,
            data: JSON.stringify(error),
            contentType: "application/json"
        })
        scheduleProcessing(true);
    }

    scheduleProcessing(true);
}())