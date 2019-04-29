import React from 'react';
import {HashRouter, NavLink} from "react-router-dom";
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
      } else if(result.action === "add") {
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
  }

  updateUser(user) {
    let data = this.state.users;
    this.state.users.some(function (obj) {
      if (obj.id === user.id) {
        //change the value here
        obj.status = user.status;
        return true;    //breaks out of he loop
      }
    });
    this.setState({ users: data });
    return data;
  }

  renderRow(user) {
    return (
        <tr>
          <th scope="row">{ user.id }</th>
          <td>{ user.name }</td>
          <td>{ user.rideId }</td>
          <td>{ user.status }</td>
        </tr>
    );
  }


  render() {
    return (
        <HashRouter>
          <Websocket url={'ws://localhost:8080/queue-line-update/' + this.props.rideId}
                     onMessage={this.onMessage.bind(this)}/>
          <table className="table table-striped table-bordered">
            <thead className="thead-dark">
            <tr>
              <th scope="col">#</th>
              <th scope="col">Name</th>
              <th scope="col">Ride</th>
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
