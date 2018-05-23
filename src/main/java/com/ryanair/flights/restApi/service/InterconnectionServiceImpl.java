package com.ryanair.flights.restApi.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ryanair.flights.restApi.domain.Day;
import com.ryanair.flights.restApi.domain.Flight;
import com.ryanair.flights.restApi.domain.Route;
import com.ryanair.flights.restApi.domain.TimeTable;
import com.ryanair.flights.restApi.response.FlightResponse;
import com.ryanair.flights.restApi.response.Leg;

@Service("interconnectionService")
public class InterconnectionServiceImpl implements InterconnectionService{
	
	private static final Logger log = LogManager.getLogger(InterconnectionServiceImpl.class);
	
	@Autowired
	private RestTemplate restTemplate;
	
	private static final String ROUTES_URL = "https://api.ryanair.com/core/3/routes";
	private static final String TIME_TABLE_URL = "https://api.ryanair.com/timetable/3/schedules/{from}/{to}/years/{year}/months/{month}";
	
	
	@Override
	public List<FlightResponse> searchFlights(String departure, String arrival, LocalDateTime departureDateTime,
			LocalDateTime arrivalDateTime) {
		List<FlightResponse> list = new ArrayList<FlightResponse>();
		log.info("Seraching directFlights:" + departure + "-" + arrival);
		FlightResponse result = getDirectFlights(departure, arrival, departureDateTime,arrivalDateTime);
		log.info(result.getLegs().size() + " directFlights were found");
		if(!result.getLegs().isEmpty()){
			list.add(result);
		}
		log.info("Seraching one stop flights:" + departure + "-STOP-" + arrival);
		result = getOneStopFlights(departure, arrival, departureDateTime,arrivalDateTime);
		log.info(result.getLegs().size()/2 + " flights with a stop were found");
		if(!result.getLegs().isEmpty()){
			list.add(result);
		}
		return list;
	}
	/**
	 * Find flights with one stop that keep to @arrivalDateTime and @departureDateTime
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	private FlightResponse getOneStopFlights(String departure, String arrival, LocalDateTime departureDateTime,LocalDateTime arrivalDateTime) {
		FlightResponse result = new FlightResponse(1,new ArrayList<Leg>());
		long difference = departureDateTime.withDayOfMonth(1).until(arrivalDateTime.withDayOfMonth(1), ChronoUnit.MONTHS);
		List<Route> allRoutes = getRoutes();
		List<Route> routesToDestination = allRoutes.stream().filter(toAirport(arrival)).collect(Collectors.toList());
		routesToDestination.forEach(route ->{
			boolean existRoute = allRoutes.stream().anyMatch(toAirport(route.getAirportFrom()).and(fromAirport(departure)));
			if(existRoute){
				TimeTable timeTableDeparture = getTimeTable(departure, route.getAirportFrom(), departureDateTime);
				TimeTable timeTableArraival = getTimeTable(route.getAirportFrom(),arrival, departureDateTime);
				List<Day> days = timeTableDeparture.getDays().stream().filter(afterDeparture(departureDateTime)).collect(Collectors.toList());
				days.forEach(dayIter -> {
					dayIter.getFlights().forEach(flightIter-> {
						timeTableArraival.getDays().stream().filter(afterTime(flightIter.getArrivalTime().plusHours(2)));
					});
				});
			}
		});
		return result;
	}
	
	/**
	 * Find direct flights that keep to @arrivalDateTime and @departureDateTime
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	private FlightResponse getDirectFlights(String departure, String arrival, LocalDateTime departureDateTime,LocalDateTime arrivalDateTime) {
		FlightResponse result = new FlightResponse(0,new ArrayList<Leg>());
		long difference = departureDateTime.withDayOfMonth(1).until(arrivalDateTime.withDayOfMonth(1), ChronoUnit.MONTHS);
		//CHECK IF DEPARTURE AND ARRIVAL BELONG TO SAME MONTH
		for(int i = 0; i <= difference; i++){
			TimeTable timeTable = getTimeTable(departure, arrival, departureDateTime.plus(i, ChronoUnit.MONTHS));
			if(timeTable != null){
				List<Day> days = null;
				if(difference == 0){
					//IF BOTH DATES BELONG TO SAME MONTH WE CHECK BOTH CONDITIONS(AFTER DEPARTURE AND BEFORE ARRIVAL)
					days = timeTable.getDays().stream().filter(afterDeparture(departureDateTime).and(beforeArrival(arrivalDateTime))).collect(Collectors.toList());
				}else{
					//IF BOTH DATES BELONG TO DIFFERENT MONTHS WE CHECK DEPARTURE CONDITION ONLY IF IS DEPARTURE MONTH
					// AND ARRIVAL CONDITION ONLY IF IS ARRIVAL MONTH
					if(i == 0){
						days = timeTable.getDays().stream().filter(afterDeparture(departureDateTime)).collect(Collectors.toList());
					}else if(i == difference){
						days = timeTable.getDays().stream().filter(beforeArrival(arrivalDateTime)).collect(Collectors.toList());
					}else{
						//IF MONTH IS BETWEEN EACH MONTH DATES WE ADD ALL FLGHTS
						days = timeTable.getDays();
					}
				}
				addFlights(departure, arrival, departureDateTime.plus(i, ChronoUnit.MONTHS), result.getLegs(), days);
			}
		}
		return result;
	}
	/**
	 * parse flights to legs and add them to the list
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param result
	 * @param days
	 */
	private void addFlights(String departure, String arrival, LocalDateTime departureDateTime, List<Leg> result, List<Day> days) {
		days.forEach(day->{
			Leg leg = new Leg(departure,arrival);
			day.getFlights().forEach(flight-> {
				leg.setDepartureDateTime(LocalDateTime.of(departureDateTime.getYear(),departureDateTime.getMonthValue(),day.getDay(),
						flight.getDepartureTime().getHour(),flight.getDepartureTime().getMinute()));
				leg.setArrivalDateTime(LocalDateTime.of(departureDateTime.getYear(),departureDateTime.getMonthValue(),day.getDay(),
						flight.getArrivalTime().getHour(),flight.getArrivalTime().getMinute()));
			});
			result.add(leg);
		});
	}

	/**
	 * Gets time table for specific year and month 
	 */
	private TimeTable getTimeTable(String departure, String arrival, LocalDateTime departureDateTime) {
		Map<String, String> uriParams = new HashMap<String, String>();
		uriParams.put("from", departure);
		uriParams.put("to", arrival);
		uriParams.put("year", String.valueOf(departureDateTime.getYear()));
		uriParams.put("month", String.valueOf(departureDateTime.getMonthValue()));
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(TIME_TABLE_URL);
		try{
			URI restUri = builder.buildAndExpand(uriParams).toUri();
			log.info("Calling " + restUri);
			ResponseEntity<TimeTable> timeTableResponse = restTemplate.getForEntity(restUri,TimeTable.class);
			TimeTable timeTable = timeTableResponse.getBody();
			log.debug("Days: "+ timeTable.getDays().size());
			return timeTable;
		} catch (HttpClientErrorException ex)   {
			if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
		        throw ex;
		    }else{
		    	log.info("No flights were found");
		    	return null;
		    }
		}
	}
	
	/**
	 * Get all routes available
	 * @return
	 */
	private List<Route> getRoutes(){
		ResponseEntity<List<Route>> routeResponse =
				restTemplate.exchange(ROUTES_URL,HttpMethod.GET, null, new ParameterizedTypeReference<List<Route>>() {
				});
		List<Route> routes = routeResponse.getBody();
		log.info("Total: "+ routes.size());
		return routes;
	}
	
	//PREDICATES
	/**
	 * Filter Days with flights after departureDateTime
	 * @param departureDateTime
	 * @return
	 */
	private Predicate<Day> afterDeparture(LocalDateTime departureDateTime){
		return (x ->  x.getDay() == departureDateTime.getDayOfMonth() && x.getFlights().stream().anyMatch(fromTime(departureDateTime))
				|| x.getDay() > departureDateTime.getDayOfMonth());
	}
	/**
	 * Filter Flights with departuretime after @departureDateTime
	 * @param departureDateTime
	 * @return
	 */
	private Predicate<Flight> fromTime(LocalDateTime departureDateTime){
		return (x ->  x.getDepartureTime().compareTo(departureDateTime.toLocalTime()) >= 0);
	}
	/**
	 * Filter Days with flights before @arrivalDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	private Predicate<Day> beforeArrival(LocalDateTime arrivalDateTime){
		return (x ->  x.getDay() == arrivalDateTime.getDayOfMonth() && x.getFlights().stream().anyMatch(toTime(arrivalDateTime))
				|| x.getDay() < arrivalDateTime.getDayOfMonth());
	}
	/**
	 * Filter Flights with arrivalTime before @arrivalDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	private Predicate<Flight> toTime(LocalDateTime arrivalDateTime){
		return (x ->  x.getArrivalTime().compareTo(arrivalDateTime.toLocalTime()) <= 0);
	}
	/**
	 * Filter routes with out connection and arriving to @arrivalAirport
	 * @param arrivalAirport
	 * @return
	 */
	private Predicate<Route> toAirport(String arrivalAirport){
		return (x ->  x.getAirportTo().equals(arrivalAirport) && x.getConnectingAirport() == null);
	}
	/**
	 * Filter routes with out connection and arriving to @departureAirport
	 * @param departureAirport
	 * @return
	 */
	private Predicate<Route> fromAirport(String departureAirport){
		return (x ->  x.getAirportFrom().equals(departureAirport) && x.getConnectingAirport() == null);
	}
}