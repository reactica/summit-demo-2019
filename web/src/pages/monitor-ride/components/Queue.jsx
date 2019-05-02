import React from 'react';
import {HashRouter} from "react-router-dom";
import Websocket from 'react-websocket';


class Queue extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      users: []
    };
  }

  onMessage(data) {
    let result = JSON.parse(data);
    if(Array.isArray(result)) {
      this.setState( {
        users: result
      })
      console.log(this.state.users);
    } else {
      console.log(result);
      if(result.action === "update") {
        this.updateUser(result.user);
      } else if(result.action === "remove") {
        this.removeUser(result.user);
      } else if(result.action === "new") {
        this.addUser(result.user);
      }
    }
  }

  addUser(user) {
    let data = this.state.users;
    data.push(user);
    this.setState({ users: data });
  }

  removeUser(user) {
    let data = this.state.users.filter(function (obj) {
      return obj.id !== user.id
    });
    this.setState({ users: data });
    this.forceUpdate();
  }

  updateUser(user) {
    let data = this.state.users;
    data.map(function(item) { return item.id === user.id ? user: item });
    this.setState({ users: data });
    this.forceUpdate();
  }

  renderRow(user) {
    return (
        <tr key={user.id.toString()}>
          <td>{ user.name }</td>
          <td>{ user.rideId }</td>
          <td>{ user.currentState }</td>
          <td>{ user.enterQueueTime }</td>
        </tr>
    );
  }


  render() {
    let wsUrl="ws://localhost:8081/queue-line-update/" + this.props.rideId;
    console.log("Opening WebSocket to " + wsUrl);
    return (
        <HashRouter>
          <Websocket url={'ws://localhost:8081/queue-line-update/' + this.props.rideId}
                     onMessage={this.onMessage.bind(this)}/>
          <table className="table table-striped table-bordered">
            <thead className="thead-dark">
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Ride</th>
              <th scope="col">State</th>
              <th scope="col">Time in Queue</th>
            </tr>
            </thead>
            <tbody>
            { this.state.users.map(this.renderRow) }

            </tbody>
          </table>
        </HashRouter>
    )
  }
}

export default Queue;
