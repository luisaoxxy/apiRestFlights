package com.ryanair.flights.restApi.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ryanair.flights.restApi.response.FlightResponse;
import com.ryanair.flights.restApi.service.InterconnectionService;

@RestController
@RequestMapping(value = "/ryainAirFlights", produces = "application/json")
public class InterconnectionController {

	private static final Logger log = LogManager.getLogger(InterconnectionController.class);
	
	@Autowired
	private InterconnectionService interconnectionService;
	
	@RequestMapping(value = "/interconnections", params = { "departure", "arrival", "departureDateTime",
			"arrivalDateTime" }, method = RequestMethod.GET)
	public ResponseEntity<List<FlightResponse>> getInterconnections(@RequestParam String departure, @RequestParam String arrival,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam("departureDateTime") LocalDateTime departureDateTime,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam("arrivalDateTime") LocalDateTime arrivalDateTime) {
		log.info("FROM: " + departure + "--" + departureDateTime + " TO: " + arrival + "--" + arrivalDateTime);
		List<FlightResponse> response = interconnectionService.searchFlights(departure, arrival, departureDateTime, arrivalDateTime);
		if(response == null){
			return new ResponseEntity<List<FlightResponse>>(HttpStatus.NO_CONTENT);
		}else{
			return new ResponseEntity<List<FlightResponse>>(response,HttpStatus.OK);
		}
	}
}