import React, { Component } from 'react';
import Card from '../../common-components/Card';

class Home extends Component {
  render() {
    return (
      <div>
        <div className="top-banner"/>
        <div className="row justify-content-center">
          <div className="col-lg-3 col-sm-5 text-center">
            <Card title="Reactica roller coster"
                  description="The most amazing roller coaster in the Reactica team park. It's bigger, better and more reactive than all other rides!"
                  image="reactica-rollercoster.jpg"
                  showButton={true}/>
          </div>
          <div className="col-lg-3 col-sm-5 text-center">
            <Card title="Reactica horror house"
                  description="A really scary horror house that will make you react to stimulus! Expect a lot of random events when entering this ride"
                  image="horror-house.jpg"
                  showButton={true}/>
          </div>
        </div>
      </div>
    );
  }
}

export default Home;
