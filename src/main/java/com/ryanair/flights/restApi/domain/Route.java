package com.ryanair.flights.restApi.domain;

public class Route {
	
	private String airportFrom;
	private String airportTo;
	private String connectingAirport;
	private Boolean newRoute;
	private Boolean seasonalRoute;
	private String operator;
	private String group;
	
	/* GETTERS AND SETTERS */
	public String getAirportFrom() {
		return airportFrom;
	}
	public void setAirportFrom(String airportFrom) {
		this.airportFrom = airportFrom;
	}
	public String getAirportTo() {
		return airportTo;
	}
	public void setAirportTo(String airportTo) {
		this.airportTo = airportTo;
	}
	public String getConnectingAirport() {
		return connectingAirport;
	}
	public void setConnectingAirport(String connectingAirport) {
		this.connectingAirport = connectingAirport;
	}
	public Boolean getNewRoute() {
		return newRoute;
	}
	public void setNewRoute(Boolean newRoute) {
		this.newRoute = newRoute;
	}
	public Boolean getSeasonalRoute() {
		return seasonalRoute;
	}
	public void setSeasonalRoute(Boolean seasonalRoute) {
		this.seasonalRoute = seasonalRoute;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}

}
