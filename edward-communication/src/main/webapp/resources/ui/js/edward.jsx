var Router = window.ReactRouter
var Route = Router.Route;
var Routes = Router.Routes;
var NotFoundRoute = Router.NotFoundRoute;
var DefaultRoute = Router.DefaultRoute;
var Link = Router.Link;
var ActiveState = Router.ActiveState;
var Navigation = Router.Navigation;
var VOLUNTEER_COUNT_REFRESH_INTERVAL = 5000;

var PAGES = {
    ALL: 0, PROJECT: 1, JOB: 2, TASK: 3, EXECUTION: 4
}


var BASE_API_URL = "api/internal/"

function createCodeMirror(parentNode, selector, options) {
    var lastEditor = null;
    options = options || {isJson: false, isEditable: false}
    $(parentNode).find(selector).each(function (index, area) {
        lastEditor = CodeMirror.fromTextArea(area,
            {readOnly: !options.isEditable, lineNumbers: true, mode: {name: "javascript", json: options.isJson}})
    });
    return lastEditor;
}


var getProject = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "project/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var getNumberOfConnectedVolunteers = function(){
    return $.ajax({
        type: "GET", url: BASE_API_URL + "volunteerCount/", headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    });
}

var refreshNumberOfConnectedVolunteers = function(){
    getNumberOfConnectedVolunteers().done(function(data){
        $("#connectedVolunteers").text(data);
    })
}


window.setInterval(refreshNumberOfConnectedVolunteers, VOLUNTEER_COUNT_REFRESH_INTERVAL);
refreshNumberOfConnectedVolunteers();


var getJob = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "job/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}


var getTask = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "task/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    }).then(function (task) {
        return $.ajax({
            type: "GET", url: BASE_API_URL + "task/" + id + "/status", dataType: 'json', headers: {
                "Authorization": "Basic " + btoa("admin" + ":" + "admin")
            }
        }).then(function (status) {
            task.status = status;
            return task;
        })
    })
}


var getExecution = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "execution/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}
var getExecutionResult = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "execution/" + id + "/result", dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var getData = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "data/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}


var getProjects = function () {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "project", dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var putProject = function (project) {
    return $.ajax({
        type: "POST",
        url: BASE_API_URL + "project",
        dataType: 'json',
        data: JSON.stringify(project),
        contentType: 'application/json',
        headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}


var putJob = function (job) {
    return $.ajax({
        type: "POST",
        url: BASE_API_URL + "/job",
        dataType: 'json',
        data: JSON.stringify(job),
        contentType: 'application/json',
        headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var putTasks = function (jobId, inputs, priority, concurrentExecutionsCount, timeout) {
    return $.ajax({
        type: "POST",
        url: BASE_API_URL + "/job/" + jobId + '/tasks/' + priority + "/" + concurrentExecutionsCount + "/" + timeout,
        dataType: 'json',
        data: inputs,
        contentType: 'application/json',
        headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var Waiting = React.createClass({
    render: function () {
        return (
            <div>
            Waiting for data... </div>

        )
    }
})

var BreadCrumb = React.createClass({
    render: function () {
        var markup = [];
        var urlProperties = this.props.urlProperties;
        var getBreadcrumbPart = function (link) {
            return (
                <li>
                    <a href="#">
                    {link}
                    </a>
                </li>
            )
        }
        markup.push(getBreadcrumbPart(<Link to="/"> projects </Link>));
        if (urlProperties.projectId) {
            markup.push(getBreadcrumbPart(<Link to="project" params={urlProperties}> project: {urlProperties.projectId}</Link>))
            if (urlProperties.jobId) {
                markup.push(getBreadcrumbPart(<Link to="job" params={urlProperties}> job: {urlProperties.jobId}</Link>))
                if (urlProperties.taskId) {
                    markup.push(getBreadcrumbPart(<Link to="task" params={urlProperties}> task: {urlProperties.taskId}</Link>))
                }
            }
        }
        return (
            <ol className="breadcrumb">
		  {markup}
            </ol>
        );
    }
});
var GenericList = React.createClass({
    componentDidMount: function () {
        var that = this;
        this.props.getItemsFunction(this.props).then(function (items) {
            that.setState({items: items});
        })
    }, render: function () {
        if (!this.state) {
            return <Waiting/>
        } else {
            var that = this;
            var markup = this.state.items.map(function (item) {
                return that.props.itemToMarkupFunction(item, that.props)
            })
            if (this.state.items.length > 0) {
                return (
                    <table className="table">
                        {markup}
                    </table>);
            } else {
                return (
                    <div> No records to display. </div>
                )
            }
        }
    }

});

var projectListGetItems = function () {
    return getProjects();
};


var CellWithItemId = React.createClass({
    render: function () {
        return (<td>  &#35; {this.props.itemId} </td>);
    }
});


var projectListRenderItem = function (item) {
    return (<tr>
        <td>
            <Link to="project" params={{projectId: item.id}}>{item.name}  </Link>
        </td>
        <CellWithItemId itemId={item.id}/>
    </tr>
    )
};
var jobsListGetItems = function (props) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "project/" + props.params.projectId + "/jobs", dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var jobsListRenderItem = function (item) {
    return (<tr>
        <td>
            <Link to="job" params={{projectId: item.projectId, jobId: item.id}}>{item.name}</Link>
        </td>
        <CellWithItemId itemId={item.id}/>
    </tr>
    );
}

var tasksListGetItems = function (props) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "job/" + props.params.jobId + "/tasks", dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var tasksListRenderItem = function (item, props) {
    return (<tr>
        <td>
            <Link to="task" params={{
                projectId: props.params.projectId, jobId: item.jobId, taskId: item.id
            }}>&#35;{item.id}</Link>
        </td>
        <td>{item.priority}</td>
    </tr>);
}

var executionsListGetItems = function (props) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "task/" + props.params.taskId + "/executions", dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}

var executionsListRenderItem = function (item, props) {
    var date = new Date(0);
    date.setUTCMilliseconds(item.creationTime);
    if (item.status === "FINISHED") {
        var linkPart = (<Link to="execution" params={{
            projectId: props.params.projectId, jobId: props.params.jobId, taskId: item.taskId, executionId: item.id
        }}> {item.status} </Link> )
    } else {
        var linkPart = item.status;
    }
    return (<tr>
        <CellWithItemId itemId={item.id}/>
        <td>
        {linkPart}
        </td>
        <td> {item.error} </td>
        <td> {date.toLocaleDateString()} {date.toLocaleTimeString()}</td>

    </tr>);
}


var ProjectBox = React.createClass({
    mixins: [Navigation], componentDidMount: function () {
        var that = this;
        getProject(this.props.params.projectId).then(function (project) {
            that.setState(project);
        })
    }, onJobAdd: function () {
        this.transitionTo("addJob", this.props.params)
    }, render: function () {
        var disabled = !this.props.isEditable;
        return (
            this.state ? (<div>
                <h1> Project: {this.state.name} </h1>
                <div className="form-group">
                    <label htmlFor="id">Id: </label>
                    <input disabled={disabled} type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label  htmlFor="name">Name: </label>
                    <input disabled={disabled}  type="text" id="name" name="name" value={this.state.name} className="form-control"/>
                </div>
                <h2> Jobs </h2>
                <GenericList getItemsFunction={jobsListGetItems} itemToMarkupFunction= {jobsListRenderItem} params={this.props.params}/>
                <button className="btn" onClick={this.onJobAdd}> Add job </button>
            </div>
            ) : (
                <Waiting/>
            )
        );
    }
});

var AddProjectBox = React.createClass({
    mixins: [Navigation], getInitialState: function () {
        return {
            ownerId: 1, id: null, name: ""
        }
    }, nameChange: function (event) {
        this.setState({
            name: event.target.value
        })
    }, addClick: function () {
        var that = this;
        putProject(this.state).then(function (idContainer) {
            that.replaceWith("app");
        });
    }, render: function () {
        return (
            <div>
                <h1> Project: {this.state.name} </h1>
                <div className="form-group">
                    <label  htmlFor="name">Name: </label>
                    <input  type="text" id="name" name="name" value={this.state.name} onChange={this.nameChange} className="form-control"/>
                    <button className="btn" onClick={this.addClick}> Add </button>
                </div>
            </div>

        );
    }
});


var JobBox = React.createClass({
    mixins: [Navigation], componentDidMount: function () {
        var that = this;
        getJob(this.props.params.jobId).then(function (job) {
            that.setState(job);
        })
    }, onTasksAdd: function () {
        this.transitionTo("addTasks", this.props.params)
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Job: {this.state.name}</h1>
                <div className="form-group">
                    <label htmlFor="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label htmlFor="projectId">Project id: </label>
                    <input disabled="true" type="text" id="projectId" name="projectId" value= {this.state.projectId} className="form-control"/>
                    <label htmlFor="name">Name: </label>
                    <input disabled="true"  type="text" id="name" name="name" value={this.state.name} className="form-control"/>
                </div>
                <h2> Tasks </h2>
                <GenericList getItemsFunction={tasksListGetItems} itemToMarkupFunction={tasksListRenderItem} params={this.props.params}/>
                <button className="btn" onClick = {this.onTasksAdd}> Add tasks </button>
                <h2> Code </h2>
                <pre>Please define function compute(input) </pre>
                <textarea id="codeArea">
                     {this.state.code}
                </textarea>

            </div>

            ) : (<Waiting/>)
        );
    }, componentDidUpdate: function () {
        createCodeMirror(this.getDOMNode(), "#codeArea", {isJson: false, isEditable: false});
    }
});


var AddJobBox = React.createClass({
    mixins: [Navigation], getInitialState: function () {
        return {
            projectId: this.props.params.projectId, id: null, name: "", code: ""
        }

    }, nameChange: function (event) {
        this.setState({
            name: event.target.value
        })
    }, codeChange: function (codeMirror) {
        this.setState({
            code: codeMirror.getValue()
        })
    }, addClick: function (event) {
        var that = this;
        putJob(this.state).then(function () {
            that.transitionTo("project", that.props.params);
        })
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Job: {this.state.name}</h1>
                <div className="form-group">
                    <label htmlFor="name">Name: </label>
                    <input onChange={this.nameChange} type="text" id="name" name="name" value={this.state.name} className="form-control"/>
                </div>
                <h2> Code </h2>
                <pre>Please define function compute(input) </pre>
                <textarea id="codeArea"/>
                <button className="btn" onClick={this.addClick}> Add </button>

            </div>

            ) : (<Waiting/>)
        );
    }, componentDidMount: function () {
        var editor = createCodeMirror(this.getDOMNode(), "#codeArea", {isJson: false, isEditable: true});
        editor.on("change", this.codeChange)
    }
});

var TaskBox = React.createClass({
    componentDidMount: function () {
        var that = this;
        getTask(this.props.params.taskId).then(function (task) {
            return getData(task.inputDataId).then(function (data) {
                task.inputData = data.data;
                return task;
            })
        }).then(function (task) {
            that.setState(task);
        })
    }, dataChange: function (codeMirror) {
        this.setState({
            data: codeMirror.getValue()
        })
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Task: {this.state.id}</h1>
                <div className="form-group">
                    <label htmlFor="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label htmlFor="projectId">Project id: </label>
                    <input disabled="true" type="text" id="projectId" name="projectId" value= {this.state.jobId} className="form-control"/>
                    <label htmlFor="priority">Priority:</label>
                    <input disabled="true" type="text" id="priority" name="priority" value= {this.state.priority} className="form-control"/>
                    <label htmlFor="concurrentExecutions">Concurrent executions:</label>
                    <input disabled="true" type="text" id="concurrentExecutions" name="concurrentExecutions" value= {this.state.concurrentExecutionsCount} className="form-control"/>
                    <label htmlFor="timeout">Timeout:</label>
                    <input disabled="true" type="text" id="timeout" name="timeout" value= {this.state.timeout} className="form-control"/>
                    <label htmlFor="status">Status: </label>
                    <input disabled="true" type="text" id="status" name="status" value= {this.state.status} className="form-control"/>
                </div>
                <h2> Executions </h2>
                <GenericList getItemsFunction={executionsListGetItems} itemToMarkupFunction={executionsListRenderItem} params={this.props.params}/>
                <h2> Input data </h2>
                <textarea id="dataArea">
                {JSON.stringify(JSON.parse(this.state.inputData), null, 2)}
                </textarea>
            </div>
            ) : (<Waiting/>)
        );
    }, componentDidUpdate: function () {
        createCodeMirror(this.getDOMNode(), "#dataArea", {isJson: true, isEditable: false});
    }
});


var AddTasksBox = React.createClass({
    mixins: [Navigation], getInitialState: function () {
        return {priority: 0, concurrentExecutionsCount: 1, timeout:5000, inputs: ""};
    }, dataChange: function (codeMirror) {
        console.log("setting state", codeMirror.getValue())
        this.setState({
            inputs: codeMirror.getValue()
        })
    }, priorityChange: function (event) {
        this.setState({priority: event.target.value})
    }, concurrentExecutionsChange: function (event) {
        this.setState({concurrentExecutionsCount: event.target.value})
    }, timeoutChange: function (event) {
        this.setState({timeout: event.target.value})
    }, addClick: function () {
        var that = this;
        putTasks(this.props.params.jobId, this.state.inputs, this.state.priority,
            this.state.concurrentExecutionsCount, this.state.timeout).then(function () {
                that.transitionTo("job", that.props.params);
            })
    }, render: function () {
        return (
            <div>
                <h1>
                Add new tasks</h1>
                <h2> Options </h2>
                <div className="form-group">
                    <label htmlFor="name">Priority</label>
                    <input onChange={this.priorityChange} type="number" id="priority" min="0" name="priority" value={this.state.priority} className="form-control"/>
                    <label htmlFor="name">Number of concurrent executions</label>
                    <input onChange={this.concurrentExecutionsChange} type="number" id="priority" min="0" name="priority" value={this.state.concurrentExecutionsCount} className="form-control"/>
                    <label htmlFor="name">Timeout [ms]</label>
                    <input onChange={this.timeoutChange} type="number" id="timeout" min="100" name="priority" value={this.state.timeout} className="form-control"/>
                </div>
                <h2> Input data </h2>
                <div>Array of inputs, ex. [1,2,3]:</div>
                <textarea id="dataArea"/>
                <button className="btn" onClick={this.addClick}> Add Tasks </button>
            </div>

        );
    }, componentDidMount: function () {
        var editor = createCodeMirror(this.getDOMNode(), "#dataArea", {isJson: true, isEditable: true});
        editor.on("change", this.dataChange)
    }

});


var ExecutionBox = React.createClass({
    componentDidMount: function () {
        var that = this;
        var newState = null;
        getExecution(this.props.params.executionId).then(function (execution) {
            newState = execution;
            return getData(execution.outputDataId).then(function (data) {
                execution.result = data.data;
                return execution;
            }, function () {
                return execution;
            })
        }).always(function () {
            that.setState(newState);
        })
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Execution: {this.state.id}</h1>
                <div className="form-group">
                    <label htmlFor="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label htmlFor="taskId">Task id: </label>
                    <input disabled="true" type="text" id="taskId" name="taskId" value= {this.state.taskId} className="form-control"/>
                </div>
                <h2> Result </h2>
                <textarea id="dataArea">
                {this.state.result ? (JSON.stringify(JSON.parse(this.state.result), null, 2)) : ("No result.")}
                </textarea>
            </div>
            ) : (<Waiting/>)
        );
    }, componentDidUpdate: function () {
        createCodeMirror(this.getDOMNode(), "#dataArea", {isJson: true, isEditable: false});
    }
});


var AllProjectsBox = React.createClass({
    mixins: [Navigation], onAdd: function () {
        this.transitionTo("addProject");
    }, render: function () {
        return (
            <div>
                <GenericList  getItemsFunction={projectListGetItems} itemToMarkupFunction={projectListRenderItem}/>
                <button className="btn" onClick={this.onAdd}> Add project </button>
            </div>
        )
    }

})

var Container = React.createClass({
    mixins: [ActiveState],

    render: function () {
        return (
            <div>
                <BreadCrumb urlProperties={this.getActiveParams()}/>
                <this.props.activeRouteHandler/>
            </div>
        )
    }

});


var routes = (
    <Routes >
        <Route name="app" path="/" handler={Container}>
            <Route name="project" path="/project/:projectId" handler={ProjectBox}/>
            <Route name="addProject" path="add/project" handler={AddProjectBox}/>
            <Route name="addJob" path="add/project/:projectId/job" handler={AddJobBox}/>
            <Route name="addTasks" path="add/project/:projectId/job/:jobId/tasks" handler={AddTasksBox}/>
            <Route name="job" path="/project/:projectId/job/:jobId" handler={JobBox}/>
            <Route name="task" path="/project/:projectId/job/:jobId/task/:taskId" handler={TaskBox}/>
            <Route name="execution" path="/project/:projectId/job/:jobId/task/:taskId/execution/:executionId" handler={ExecutionBox}/>
            <DefaultRoute handler={AllProjectsBox} />
        </Route>
    </Routes>
);


var containerComponent = React.render(routes, document.getElementById('currentContent'));