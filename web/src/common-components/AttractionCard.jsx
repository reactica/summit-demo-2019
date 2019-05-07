import React from 'react';
import {NavLink} from "react-router-dom";
import Websocket from "react-websocket";

class EstimatedTime extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    if (this.props.waitTime==0)
      return (<span className="">Estimated Queue time: Under 1 min</span>);
    else if(this.props.waitTime==1)
      return (<span className="">Estimated Queue time: 1 minute</span> );
    else if(this.props.waitTime>1)
      return (<span className="">Estimated Queue time: {this.props.waitTime} minutes</span> );
    else
      return (<span className="">Calculating estimated queue time!</span> );
  }
}

class AttractionCard extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      estimatedWaitTime: -1
    };
  }

  onMessage(data) {
    let result = JSON.parse(data);
    this.setState({ estimatedWaitTime: result.estimatedWaitTime });
  }

  renderImage() {
    if(!!this.props.image) {
      return (<img className="card-img-top" src={ "./img/" + this.props.image} alt="attraction"/>);
    }
  }
  render() {
    return(
      <div className="card">
        {this.renderImage()}
        <div className="card-body">
          <h5 className="card-title">{this.props.title}</h5>
          <p className="card-text">{this.props.description}</p>
          <p><EstimatedTime waitTime={this.state.estimatedWaitTime}/></p>
        </div>
        <Websocket url={this.props.qlc_address}
                   onMessage={this.onMessage.bind(this)}/>
      </div>
    );
  }
}

export default AttractionCard;
