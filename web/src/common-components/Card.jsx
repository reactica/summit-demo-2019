import React from 'react';
import {NavLink} from "react-router-dom";


class Card extends React.Component {
  renderButton() {
    if(this.props.showButton) {
      return (<NavLink to="/book" className="btn btn-primary">Book a ride</NavLink>);
    }
  }

  renderImage() {
    if(!!this.props.image) {
      return (<img className="card-img-top" src={ "./img/" + this.props.image} alt="Card image cap"/>);
    }
  }
  render() {
    return(
      <div className="card">
        {this.renderImage()}
        <div className="card-body">
          <h5 className="card-title">{this.props.title}</h5>
          <p className="card-text">{this.props.description}</p>
          {this.renderButton()}
        </div>
      </div>
    );
  }
}

export default Card;
