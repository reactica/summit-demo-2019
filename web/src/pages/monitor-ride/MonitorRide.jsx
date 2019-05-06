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
            <div className="col-lg-4">
              <h3>User Queue for Roller Coaster</h3>
              <Queue rideId="roller-coaster" rideName="Roller Coaster"/>
            </div>
            <div className="col-lg-4">
              <h3>User Queue for Screamer</h3>
              <Queue rideId="screamer" rideName="Screamer"/>
            </div>
          </div>
        </div>
    );
  }
}

export default MonitorRide;
