package com.ryanair.flights.restApi.domain;

import java.util.List;

public class Day {

	private Integer day;
	private List<Flight> flights = null;

	public Integer getDay() {
		return day;
	}

	public void setDay(Integer day) {
		this.day = day;
	}

	public List<Flight> getFlights() {
		return flights;
	}

	public void setFlights(List<Flight> flights) {
		this.flights = flights;
	}

}