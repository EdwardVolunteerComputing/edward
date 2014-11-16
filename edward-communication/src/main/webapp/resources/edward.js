var jobCodesCache = {}

window.setTimeout(processNextTask, 1000);

var prefix = '(function(input){';

function processNextTask() {
	$.getJSON("../client/getNextTask", function(result) {
		if (!result.inputData) {
			console.log("No tasks received from server.");
			window.setTimeout(processNextTask, 1000);
			return;
		}
		var jobId = result.jobId;
		var inputData = result.inputData;
		var executionId = result.executionId;

		var callbackForWorkerSuccess = function(result) {
			sendResultToServer(result, executionId);
		}

		var callbackForWorkerError = function(error) {
			sendErrorToServer(error, executionId);
		}

		var callbacksForWorker = {
			success : callbackForWorkerSuccess,
			error : callbackForWorkerError
		};

		if (jobCodesCache[jobId]) {
			processTask(jobId, inputData, callbacksForWorker, executionId);
		} else {
			addJobToCache(jobId, function() {
				processTask(jobId, inputData, callbacksForWorker, executionId);
			});
		}
	})
}

function addJobToCache(jobId, callback) {
	$.get("/client/getCode/" + jobId, function(result) {
		jobCodesCache[jobId] = result;
		console.log("Adding code for job " + jobId + " : " + result);
		callback();
	});
}

function sendResultToServer(result, executionId) {
	$.ajax({
		type : "POST",
		url : "/client/sendResult/" + executionId,
		data : JSON.stringify(result),
		contentType : "application/json"
	})
	window.setTimeout(processNextTask, 0);
}

function sendErrorToServer(error, executionId) {
	$.ajax({
		type : "POST",
		url : "/client/sendError/" + executionId,
		data : JSON.stringify(error),
		contentType : "application/json"
	})
	window.setTimeout(processNextTask, 0);
}


function processTask(jobId, inputData, callbacksForWorker, executionId) {
	var code = jobCodesCache[jobId];

	var syntaxTestCode = prefix+ code + "});";

	try {
		// TODO: well it might not always be ok to eval here - may take resources and is not in worker
		eval(syntaxTestCode);

		var suffix = '})(' + inputData + ")";

		var effectiveCode = prefix + code + suffix;

		console.log("Sending code to worker" + effectiveCode);

		var lemming = new Lemming(effectiveCode);
		lemming.onResult(callbacksForWorker.success);
		lemming.onError(callbacksForWorker.error);

		lemming.run({timeout: 300000});
	} catch (err) {
		// TODO: what if no message?
		callbacksForWorker.error(err.message, executionId)
	}

}