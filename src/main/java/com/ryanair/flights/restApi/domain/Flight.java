package com.ryanair.flights.restApi.domain;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Flight {

	private String number;
	@JsonFormat(pattern="HH:mm")
	private LocalTime departureTime;
	@JsonFormat(pattern="HH:mm")
	private LocalTime arrivalTime;

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}
	public LocalTime getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(LocalTime departureTime) {
		this.departureTime = departureTime;
	}

	public LocalTime getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(LocalTime arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

}