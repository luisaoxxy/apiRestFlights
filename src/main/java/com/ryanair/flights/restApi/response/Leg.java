package com.ryanair.flights.restApi.response;

import java.time.LocalDateTime;

public class Leg {

	private String departureAirport;
	private String arrivalAirport;
	private LocalDateTime departureDateTime;
	private LocalDateTime arrivalDateTime;

	public Leg(String departureAirport,String arrivalAirport){
		this.departureAirport = departureAirport;
		this.arrivalAirport = arrivalAirport;
	}
	public Leg(String departureAirport,String arrivalAirport,LocalDateTime departureDateTime,LocalDateTime arrivalDateTime){
		this.departureDateTime = departureDateTime;
		this.arrivalDateTime = arrivalDateTime;
	}
	public String getDepartureAirport() {
		return departureAirport;
	}

	public void setDepartureAirport(String departureAirport) {
		this.departureAirport = departureAirport;
	}

	public String getArrivalAirport() {
		return arrivalAirport;
	}

	public void setArrivalAirport(String arrivalAirport) {
		this.arrivalAirport = arrivalAirport;
	}

	public LocalDateTime getDepartureDateTime() {
		return departureDateTime;
	}

	public void setDepartureDateTime(LocalDateTime departureDateTime) {
		this.departureDateTime = departureDateTime;
	}

	public LocalDateTime getArrivalDateTime() {
		return arrivalDateTime;
	}

	public void setArrivalDateTime(LocalDateTime arrivalDateTime) {
		this.arrivalDateTime = arrivalDateTime;
	}
}