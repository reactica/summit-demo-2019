import React from 'react';
import {HashRouter} from "react-router-dom";
import Websocket from 'react-websocket';
import moment from 'moment';


class UserItem extends React.Component {
  render() {
    return (
        <tr>
          <td>{ this.props.user.name }</td>
          <td>{ this.props.user.rideId }</td>
          <td>{ this.props.user.currentState }</td>
          <td>{ moment(new Date(this.props.user.enterQueueTime*1000).getTime()).fromNow(true) }</td>
        </tr>
    );
  }
}

class UserList extends React.Component {
  render () {
    return (
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
          {this.props.users.map( (user,key) =>
              <UserItem user={user} key={user.id.toString()}/>
          )}
          </tbody>
        </table>
    );
  }
}

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
      result.sort((a,b) => ((a.enterQueueTime < b.enterQueueTime) ? 1 : ((b.enterQueueTime < a.enterQueueTime) ? -1 : 0)))
      this.setState( {
        users: result
      })
    } else {
      if(result.action === "update") {
        this.updateUser(result.user);
      } else if(result.action === "delete") {
        this.removeUser(result.user);
      } else if(result.action === "new") {
        this.addUser(result.user);
      }
    }
  }

  addUser(user) {
    let data = this.state.users;
    data.push(user);
    data.sort((a,b) => ((a.enterQueueTime < b.enterQueueTime) ? -1 : ((b.enterQueueTime < a.enterQueueTime) ? 1 : 0)));
    this.setState({ users: data });
  }

  removeUser(user) {
    let data = this.state.users.filter(function (item) {
      return item.id !== user.id
    });
    data.sort((a,b) => ((a.enterQueueTime < b.enterQueueTime) ? -1 : ((b.enterQueueTime < a.enterQueueTime) ? 1 : 0)));
    this.setState({ users: data });
  }

  updateUser(user) {
    let data = this.state.users;
    let item = data.find(value => { return value.id === user.id });
    item.currentState = user.currentState;
    data.sort((a,b) => ((a.enterQueueTime < b.enterQueueTime) ? -1 : ((b.enterQueueTime < a.enterQueueTime) ? 1 : 0)));
    this.setState({ users: data });
  }

  render() {

    let wsUrl="ws://localhost:8081/queue-line-update/" + this.props.rideId;
    return (
        <HashRouter>
          <UserList users={this.state.users}/>
          <Websocket url={wsUrl}
                     onMessage={this.onMessage.bind(this)}/>
        </HashRouter>
    )
  }
}

export default Queue;
