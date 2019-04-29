import React from 'react';
import {HashRouter, NavLink} from "react-router-dom";

class Navigation extends React.Component {
  render() {
    return (
        <HashRouter>
          <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
            <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
              <span className="navbar-toggler-icon"></span>
            </button>

            <div className="collapse navbar-collapse" id="navbarSupportedContent">
              <ul className="navbar-nav mr-auto">
                <li className="nav-item">
                  <NavLink exact to="/" className="nav-link">Home</NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/book" className="nav-link">Book a Ride</NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/monitor" className="nav-link">Monitor a Ride</NavLink>
                </li>
              </ul>
            </div>
          </nav>
        </HashRouter>
    )
  }
}

export default Navigation;
