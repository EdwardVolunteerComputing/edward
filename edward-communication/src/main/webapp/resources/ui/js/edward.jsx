var Router = window.ReactRouter
var Route = Router.Route;
var Routes = Router.Routes;
var NotFoundRoute = Router.NotFoundRoute;
var DefaultRoute = Router.DefaultRoute;
var Link = Router.Link;
var ActiveState = Router.ActiveState;


var PAGES = {
    ALL: 0, PROJECT: 1, JOB: 2, TASK: 3, EXECUTION: 4
}


var BASE_API_URL = "../../"

function createCodeMirror(parentNode, selector, options) {
    options = options || {isJson: false, isEditable: false}
    $(parentNode).find(selector).each(function (index, area) {
        CodeMirror.fromTextArea(area,
            {readOnly: !options.isEditable, lineNumbers: true, mode: {name: "javascript", json: options.isJson}})
    });
}


var getProject = function (id) {
    return $.ajax({
        type: "GET", url: BASE_API_URL + "project/" + id, dataType: 'json', headers: {
            "Authorization": "Basic " + btoa("admin" + ":" + "admin")
        }
    })
}
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
            }).map(function (markup) {
                return ({markup}
                )
            });
            if (this.state.items.length > 0) {
                return (
                    <table className="table">
                        {markup}
                    </table>);
            }else{
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
    console.log("wfef")
    var date = new Date(0);
    date.setUTCMilliseconds(item.creationTime);
    return (<tr>
        <CellWithItemId itemId={item.id}/>
        <td>
            <Link to="execution" params={{
                projectId: props.params.projectId, jobId: props.params.jobId, taskId: item.taskId, executionId: item.id
            }}> {item.status} </Link>
        </td>
        <td> {date.toLocaleDateString()} {date.toLocaleTimeString()}</td>
    </tr>);
}


var ProjectBox = React.createClass({
    componentDidMount: function () {
        var that = this;
        getProject(this.props.params.projectId).then(function (project) {
            that.setState(project);
        })
    }, render: function () {
        return (
            this.state ? (<div>
                <h1> Project: {this.state.name} </h1>
                <div className="form-group">
                    <label for="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label  for="name">Name: </label>
                    <input disabled="true"  type="text" id="name" name="name" value={this.state.name} className="form-control"/>
                </div>
                <h2> Jobs </h2>
                <GenericList getItemsFunction={jobsListGetItems} itemToMarkupFunction= {jobsListRenderItem} params={this.props.params}/>
            </div>
            ) : (
                <Waiting/>
            )
        );
    }
});


var JobBox = React.createClass({
    componentDidMount: function () {
        var that = this;
        getJob(this.props.params.jobId).then(function (job) {
            that.setState(job);
        })
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Job: {this.state.name}</h1>
                <div className="form-group">
                    <label for="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label for="projectId">Project id: </label>
                    <input disabled="true" type="text" id="projectId" name="projectId" value= {this.state.projectId} className="form-control"/>
                    <label for="name">Name: </label>
                    <input disabled="true"  type="text" id="name" name="name" value={this.state.name} className="form-control"/>
                </div>
                <h2> Tasks </h2>
                <GenericList getItemsFunction={tasksListGetItems} itemToMarkupFunction={tasksListRenderItem} params={this.props.params}/>
                <h2> Code </h2>
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
    }, render: function () {
        return (
            this.state ? (<div>
                <h1>
                Task: {this.state.id}</h1>
                <div className="form-group">
                    <label for="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label for="jobId">Project id: </label>
                    <input disabled="true" type="text" id="jobId" name="jobId" value= {this.state.jobId} className="form-control"/>
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
                    <label for="id">Id: </label>
                    <input disabled="true" type="text" id="id" name="id" value= {this.state.id} className="form-control"/>
                    <label for="taskId">Task id: </label>
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
            <Route name="job" path="/project/:projectId/job/:jobId" handler={JobBox}/>
            <Route name="task" path="/project/:projectId/job/:jobId/task/:taskId" handler={TaskBox}/>
            <Route name="execution" path="/project/:projectId/job/:jobId/task/:taskId/execution/:executionId" handler={ExecutionBox}/>
            <DefaultRoute getItemsFunction={projectListGetItems} itemToMarkupFunction={projectListRenderItem} handler={GenericList} />
        </Route>
    </Routes>
);


var containerComponent = React.render(routes, document.getElementById('currentContent'));