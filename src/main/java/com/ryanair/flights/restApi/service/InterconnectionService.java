package com.ryanair.flights.restApi.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ryanair.flights.restApi.response.FlightResponse;

public interface InterconnectionService {

	/**
	 *  Find direct flights for specific parameters
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	public List<FlightResponse> searchFlights(String departure, String arrival,LocalDateTime departureDateTime,LocalDateTime arrivalDateTime);
}