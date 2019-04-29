import React, {Component} from 'react';
import Card from "../../common-components/Card";
import ScheduleRide from './components/SceduleRide'
import WaitingLine from './components/WaitingLine'


class BookRide extends Component {
  render() {
    return (
        <div>
          <div className="top-banner"/>
          <div className="row justify-content-center">
            <div className="col-lg-3 col-sm-5">
              <ScheduleRide/>
            </div>
            <div className="col-lg-3 col-sm-5 text-center">
              <Card title="5 min waiting time"
                    description="" image="reactica-rollercoster.jpg"/>
            </div>
          </div>
          <div className="row justify-content-center">
            <div className="col-lg-3 col-sm-5">&nbsp;</div>
          </div>
          <div className="row justify-content-center">
            <div className="col-lg-3 col-sm-5">
              <WaitingLine/>
            </div>
            <div className="col-lg-3 col-sm-5 text-center">
              <Card title="43 persons inline"
                    description=""
                    image="horror-house.jpg"/>
            </div>
          </div>
        </div>
    );
  }
}

export default BookRide;
