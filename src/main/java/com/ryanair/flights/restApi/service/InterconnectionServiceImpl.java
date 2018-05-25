package com.ryanair.flights.restApi.service;

import java.net.URI;
import java.time.LocalDate;
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
		routesToDestination.forEach(arrivalRoute ->{
			String stop = arrivalRoute.getAirportFrom();
			boolean existRoute = allRoutes.stream().anyMatch(fromAirport(departure).and(toAirport(stop)));
			if(existRoute){
				//CHECK TIME TABLES ONLY IF ONE STOP ROUTE EXISTS
				for(int i = 0; i <= difference; i++){
					LocalDateTime departureTimeAux = departureDateTime.plus(i, ChronoUnit.MONTHS);
					TimeTable timeTableDeparture = getTimeTable(departure, stop, departureTimeAux);
					TimeTable timeTableArraival = getTimeTable(stop,arrival, departureTimeAux);
					if(timeTableDeparture != null && timeTableArraival != null){
						timeTableDeparture.getDays().forEach(dayAux -> {
							List<Day> daysDeparture = getDepartureDates(departureDateTime, arrivalDateTime, difference, /*i*/1,	dayAux);
						if(!daysDeparture.isEmpty()){
							log.info(daysDeparture.size() + " days of " + departureTimeAux.getMonth() + " has flights departing after " + departureDateTime);
							//LOOK FOR ARRIVAL TIME INTO ARRIVAL AIRPORT
							final Integer count = new Integer(/*i*/1);
							daysDeparture.forEach(departureDay -> {
								log.info(departureDay.getFlights().size() + " flights for day " + departureDay.getDay());
								departureDay.getFlights().forEach(departureFlight ->{
									timeTableArraival.getDays().forEach(dayArrival->{
										if(dayArrival.getDay() == departureDay.getDay()){
											List<Day> daysArrival = getArrivalDates(arrivalDateTime, difference, count, departureTimeAux,
													dayArrival, departureDay, departureFlight);
											if(!daysArrival.isEmpty()){
												log.info(daysArrival.size() + " days of " + departureTimeAux.getMonth() + " has flights departing after " + departureFlight.getArrivalTime().plusHours(2));
												result.getLegs().addAll(addOneStopFlights(departure, arrival, departureDateTime,departureDay,
														departureFlight,daysArrival,stop));
											}
										}
									});
								});
							});
						}
						});
					}
				}
			}
		});
		return result;
	}
	private List<Day> getArrivalDates(LocalDateTime arrivalDateTime, long difference, int i,
			LocalDateTime departureTimeAux, Day arrivalDay, Day departureDay, Flight departureFlight) {
		List<Day> daysArrival = new ArrayList<Day>();
		LocalDateTime departureDate = LocalDateTime.of(departureTimeAux.toLocalDate().withDayOfMonth(departureDay.getDay()),
				departureFlight.getArrivalTime()).plusHours(2);
		log.info("Looking for flights departing after:" + departureDate);
		if(difference == 0 || i == difference){
			//ARRIVAL MONTH
			if(arrivalDay.getDay() == arrivalDateTime.getDayOfMonth()){
				List<Flight> validFlights = arrivalDay.getFlights().stream().filter(fromTime(departureDate).and(
						toTime(arrivalDateTime))).collect(Collectors.toList());
				Day validDay = new Day(arrivalDay.getDay(),validFlights);
				daysArrival.add(validDay);
			}else if(arrivalDay.getDay() < arrivalDateTime.getDayOfMonth()){
				List<Flight> validFlights = arrivalDay.getFlights().stream().filter(fromTime(departureDate)).collect(Collectors.toList());
				Day validDay = new Day(arrivalDay.getDay(),validFlights);
				daysArrival.add(validDay);
			}
		}else{
			//ALL MONTH EXCEP ARRIVAL
			List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDate)).collect(Collectors.toList());
			Day validDay = new Day(arrivalDay.getDay(),validFlights);
			daysArrival.add(validDay);
		}
		return daysArrival;
	}
	private List<Day> getDepartureDates(LocalDateTime departureDateTime, LocalDateTime arrivalDateTime, long difference,
			int i, Day departureDay) {
		List<Day> daysDeparture = new ArrayList<Day>();
		//THE SAME MONTH FOR DEPARTURE AND ARRIVAL
		if(difference == 0){
			if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
				List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDateTime).and(
						toTime(arrivalDateTime.minusHours(2)))).collect(Collectors.toList());
				Day validDay = new Day(departureDay.getDay(),validFlights);
				daysDeparture.add(validDay);
			}else if(departureDay.getDay() > departureDateTime.getDayOfMonth()){
				daysDeparture.add(departureDay);
			}
		}else if(i == 0){
			//DEPARTURE MONTH
			if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
				List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDateTime))
						.collect(Collectors.toList());
				Day validDay = new Day(departureDay.getDay(),validFlights);
				daysDeparture.add(validDay);
			}else if(departureDay.getDay() > departureDateTime.getDayOfMonth()){
				daysDeparture.add(departureDay);
			}
		}else if(i == difference){
			//ARRIVAL MONTH
			if(departureDay.getDay() == arrivalDateTime.getDayOfMonth()){
				List<Flight> validFlights = departureDay.getFlights().stream().filter(toTime(arrivalDateTime.minusHours(2)))
						.collect(Collectors.toList());
				Day validDay = new Day(departureDay.getDay(),validFlights);
				daysDeparture.add(validDay);
			}else if(departureDay.getDay() < arrivalDateTime.getDayOfMonth()){
				daysDeparture.add(departureDay);
			}
		}else{
			//MONTHS BETWEEN ARRIVAL AND DEPARTURE
			daysDeparture.add(departureDay);
		}
		return daysDeparture;
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
			LocalDateTime departureAux = departureDateTime.plus(i, ChronoUnit.MONTHS);
			TimeTable timeTable = getTimeTable(departure, arrival, departureAux);
			if(timeTable != null){
				List<Day> days = new ArrayList<Day>();
				final Integer count = new Integer(i);
				timeTable.getDays().forEach(departureDay -> {
					if(difference == 0){
						if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
							List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDateTime).and(toTime(arrivalDateTime)))
									.collect(Collectors.toList());
							Day validDay = new Day(departureDay.getDay(),validFlights);
							days.add(validDay);
						}else if(departureDay.getDay() > departureDateTime.getDayOfMonth()){
							days.add(departureDay);
						}
						//IF BOTH DATES BELONG TO SAME MONTH WE CHECK BOTH CONDITIONS(AFTER DEPARTURE AND BEFORE ARRIVAL)
						//days = timeTable.getDays().stream().filter(afterDeparture(departureDateTime).and(beforeArrival(arrivalDateTime)))
						//	.collect(Collectors.toList());
						//days = timeTable.getDays().stream().filter(x ->  x.getDay() == departureDateTime.getDayOfMonth()).map(x -> x.getFlights())      //Stream<Set<String>>
						//      .flatMap(x -> x.stream()).filter(x ->  x.getDepartureTime().compareTo(departureDateTime.toLocalTime()) >= 0).collect(Collectors.toList());
					}else{
						//IF BOTH DATES BELONG TO DIFFERENT MONTHS WE CHECK DEPARTURE CONDITION ONLY IF IS DEPARTURE MONTH
						// AND ARRIVAL CONDITION ONLY IF IS ARRIVAL MONTH
						if(count == 0){
							if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
								List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDateTime))
										.collect(Collectors.toList());
								Day validDay = new Day(departureDay.getDay(),validFlights);
								days.add(validDay);
							}else if(departureDay.getDay() > departureDateTime.getDayOfMonth()){
								days.add(departureDay);
							}
						}else if(count == difference){
							if(departureDay.getDay() == arrivalDateTime.getDayOfMonth()){
								List<Flight> validFlights = departureDay.getFlights().stream().filter(toTime(arrivalDateTime))
										.collect(Collectors.toList());
								Day validDay = new Day(departureDay.getDay(),validFlights);
								days.add(validDay);
							}else if(departureDay.getDay() < arrivalDateTime.getDayOfMonth()){
								days.add(departureDay);
							}
						}else{
							//IF MONTH IS BETWEEN EACH MONTH DATES WE ADD ALL FLGHTS
							days.addAll(timeTable.getDays());
						}
					}
					if(!days.isEmpty()){
						log.info(days.size() + " days of " + departureAux.getMonth() + " have direct flights");
						result.getLegs().addAll(addFlights(departure, arrival, departureAux,days));
					}
				});
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
	private List<Leg> addFlights(String departure, String arrival, LocalDateTime departureDateTime, List<Day> days) {
		List<Leg> result = new ArrayList<Leg>();
		days.forEach(day->{
			LocalDate flightDepartureDate = departureDateTime.toLocalDate().withDayOfMonth(day.getDay());
			day.getFlights().forEach(flight-> {
				Leg leg = new Leg(departure,arrival);
				leg.setDepartureDateTime(LocalDateTime.of(flightDepartureDate,flight.getDepartureTime()));
				leg.setArrivalDateTime(LocalDateTime.of(flightDepartureDate,flight.getArrivalTime()));
				result.add(leg);
			});
		});
		return result;
	}

	private List<Leg> addOneStopFlights(String departure, String arrival, LocalDateTime departureDateTime, 
			Day departureDay, Flight departurFlight ,List<Day> arrivalDays,String stop) {
		List<Leg> result = new ArrayList<Leg>();
		LocalDate flightDepartureDate = departureDateTime.toLocalDate().withDayOfMonth(departureDay.getDay());
		arrivalDays.forEach(dayArrival->{
			Leg legFrom = new Leg(departure,stop);
			legFrom.setDepartureDateTime(LocalDateTime.of(flightDepartureDate, departurFlight.getDepartureTime()));
			legFrom.setArrivalDateTime(LocalDateTime.of(flightDepartureDate, departurFlight.getArrivalTime()));
			Leg legTo = new Leg(stop,arrival);
			LocalDate flightArrivalDate = departureDateTime.toLocalDate().withDayOfMonth(dayArrival.getDay());
			dayArrival.getFlights().forEach(flightArrival-> {
				result.add(legFrom);
				legTo.setDepartureDateTime(LocalDateTime.of(flightArrivalDate, flightArrival.getDepartureTime()));
				legTo.setArrivalDateTime(LocalDateTime.of(flightArrivalDate,flightArrival.getArrivalTime()));
				result.add(legTo);
			});
		});
		return result;
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