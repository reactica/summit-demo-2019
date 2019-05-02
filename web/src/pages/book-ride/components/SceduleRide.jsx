import React, {Component} from "react";

class ScheduleRide extends Component {

  constructor(props) {
    super(props);
    this.state = {selectedRide: 0};

    // This binding is necessary to make `this` work in the callback
    this.setRollerCoaster = this.setRollerCoaster.bind(this);
    this.setHorrorHouse = this.setHorrorHouse.bind(this);
  }

  setRollerCoaster() {
    console.log('Roller Coaster selected');
    this.setState(state => ({
      selectedRide: 1
    }));
  }
  setHorrorHouse() {
    console.log('Horror House selected');
    this.setState(state => ({
      selectedRide: 2
    }));
  }

  render() {
    return (
        <div className="card">
          <div className="card-body">
            <h3 className="card-title">Book your ride here</h3>
            <div className="form-group">
              <button className="btn btn-primary dropdown-toggle mr-4" type="button" data-toggle="dropdown"
                      aria-haspopup="true" aria-expanded="false">Select Ride
              </button>
              <div className="dropdown-menu">
                <a className="dropdown-item" onClick={this.setRollerCoaster}>Roller Coaster</a>
                <a className="dropdown-item" onClick={this.setHorrorHouse}>Horror house</a>
              </div>
            </div>
            { this.state.selectedRide === 1 &&
            <div className="form-group">
              <label htmlFor="bookARideWithName">Name</label>
              <input type="text" className="form-control" id="bookARideWithName"
                     placeholder="Enter your name"/>
              <button type="submit" className="btn btn-secondary" style={{ marginTop: "5px" }}>Book Roller Coaster</button>
            </div>
            }
            { this.state.selectedRide === 2 &&
            <span style={{ color: "#FF0000", fontWeight: "bold"}}>Not implemented yet!</span>
            }
          </div>
        </div>
    );
  }
}

export default ScheduleRide;

