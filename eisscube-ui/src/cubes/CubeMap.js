import React, { Component } from 'react';
import { connect } from 'react-redux';
import L from 'leaflet';
import { Map, LayersControl, TileLayer, Marker, Popup } from 'react-leaflet';
import { showNotification, UPDATE } from 'react-admin';
import { dataProvider } from '../App';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.js';

const { BaseLayer } = LayersControl;

const blueMarker = L.AwesomeMarkers.icon({
    icon: 'circle',
    prefix: 'fa',
    markerColor: 'blue'
});

class CubeMap extends Component {
    constructor(props) {
        super(props);
        this.state = {
            location: {
                lat: 40.2769179,
                lng: -74.0388226
            },
            draggable: true
        };
    }

    componentDidUpdate(prevProps) {
        if (this.props.record !== prevProps.record) {
            let { location } = this.props.record;
            if (location) {
                this.setState({
                    location
                });	
            }
        }
    }

    toggleDraggable = () => {
        this.setState({
            draggable: !this.state.draggable
        });
    }

    updatePosition = () => {
        const { lat, lng } = this.refs.marker.leafletElement.getLatLng();

        dataProvider(UPDATE, 'cubes/location', {
            id: this.props.record.id,
            data: {
                lat,
                lng
            }
        })
        .then(() => {
            this.props.dispatch(showNotification(`${this.props.record.deviceID} - Location updated`));
            this.setState({
                location: { lat, lng }
            });
        })
        .catch((e) => {
            this.props.dispatch(showNotification('Cannot update location', 'warning'));
        });
    }

    render() {
        const { record } = this.props;
        const { location } = this.state;

        const marker = (
            location 
            ?
            <Marker
                draggable={this.state.draggable}
                onDragend={this.updatePosition}
                position={[location.lat, location.lng]}
                icon={blueMarker}
                ref="marker"
            >
                <Popup minWidth={200} open>
                    <span>
                    <i>ICCID:</i> <b>{record.deviceID}</b>
                    <br/>
                    <i>Name:</i> {record.name}
                    <br/>
                    <i>Customer:</i> {record.customerID}
                    <br/>
                    <a href={`#/commands?filter={"q":"${record.id}"}&page=1&perPage=10&sort=created&order=DESC`}>Commands</a>
                    <br/>
                    <a href={`#/reports?filter={"q":"${record.deviceID}"}&page=1&perPage=10&sort=reportID&order=ASC`}>Reports</a>
                    <br/>
                    <br/>
                    <i>NOTE: Hold and move to change location</i>
                    </span>
                </Popup>
            </Marker>
            :
            null
        );

        return (
            <Map
                animate={true}
                length={4}
                center={location ? [location.lat, location.lng] : [40.2769179, -74.0388226]}
                minZoom={2}
                maxZoom={19}
                zoom={15}
                ref="map"
            >
                <LayersControl position="topright">
                    <BaseLayer checked name="Color">
                        <TileLayer
                            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                    </BaseLayer>
                    <BaseLayer name="Black And White">
                        <TileLayer
                            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
                            url="http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png"
                        />
                    </BaseLayer>
                    {marker}
                </LayersControl>
            </Map>
        );
    }
}

export default connect(null, (dispatch) => ({dispatch}))(CubeMap);
