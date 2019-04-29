import React, { Component } from "react";

class WaitingLine extends Component {
  render() {
    return <div className="card">
      <h3>Your current waiting list</h3>
      <ul>
        <li>Roller coster - 3 min</li>
        <li>Horror house - 10 min</li>
      </ul>
    </div>;
  }
}

export default WaitingLine;