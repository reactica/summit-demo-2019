import React, {Component} from 'react';
import Queue from './components/Queue'

const rideRollerCoasterName = "Roller Coaster";
const rideScreamerName = "Screamer";

class MonitorRide extends Component {
  constructor(props) {
    super(props);
    this.state = {selectedRide: 'roller-coaster', selectedRideName: rideRollerCoasterName};

    // This binding is necessary to make `this` work in the callback
    this.setRide = this.setRide.bind(this);
  }

  setRide(id) {
    let name="";
    switch (id) {
      case "roller-coaster" :
        name=rideRollerCoasterName;
        break;
      case "screamer" :
        name=rideScreamerName;
        break;
      default :
        name="Unknown";
    }
    this.setState(state => ({
      selectedRide: id,
      selectedRideName: name
    }));
  }

  render() {
    return (
        <div>
          <div className="top-banner"/>
          <div className="row justify-content-center">
            <div className="col-lg-8 col-sm-12">
              <h3>User Queue for &nbsp;
                <button className="btn btn-primary dropdown-toggle mr-4" type="button" data-toggle="dropdown"
                        aria-haspopup="true" aria-expanded="false"> {this.state.selectedRideName}
                </button>
                <div className="dropdown-menu">
                  <a className="dropdown-item" onClick={() => this.setRide("roller-coaster")}>Roller Coaster</a>
                  <a className="dropdown-item" onClick={() => this.setRide("screamer")}>Screamer</a>
                </div>
              </h3>
            </div>
          </div>

          <div className="row justify-content-center">
            <div className="col-lg-8 col-sm-12">
              <Queue rideId={this.state.selectedRide} rideName={this.state.selectedRideName}/>
            </div>
          </div>
        </div>
    );
  }
}

export default MonitorRide;
