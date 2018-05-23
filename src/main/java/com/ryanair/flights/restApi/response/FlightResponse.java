package com.ryanair.flights.restApi.response;

import java.util.List;

public class FlightResponse {

	private Integer stops;
	private List<Leg> legs = null;

	public FlightResponse(Integer stops,List<Leg> legs){
		this.stops = stops;
		this.legs = legs;
	}
	public Integer getStops() {
		return stops;
	}

	public void setStops(Integer stops) {
		this.stops = stops;
	}

	public List<Leg> getLegs() {
		return legs;
	}

	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}

}