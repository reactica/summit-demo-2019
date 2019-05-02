import React, { Component } from 'react';
import {
    Route,
    HashRouter
} from "react-router-dom";
import Home from "./pages/home/Home";
import MonitorRide from "./pages/monitor-ride/MonitorRide";
import BookRide from "./pages/book-ride/BookRide";

import Navigation from './pages/home/components/Navigation';

class Main extends Component {
    render() {
        return (
            <HashRouter>
                <Navigation />
                <div>
                    <Route exact path="/" component={Home}/>
                    <Route path="/book" component={BookRide}/>
                    <Route path="/monitor" component={MonitorRide}/>
                </div>
            </HashRouter>


        );
    }
}

export default Main;
