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
import java.util.stream.Stream;

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
	
	public static final String ROUTES_URL = "https://api.ryanair.com/core/3/routes";
	public static final String TIME_TABLE_URL = "https://api.ryanair.com/timetable/3/schedules/{from}/{to}/years/{year}/months/{month}";
	
	
	@Override
	/**Find direct flights or one stop flights that keep to @arrivalDateTime and @departureDateTime
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param arrivalDateTime
	 */
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
													dayArrival, departureFlight);
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
						//IF BOTH DATES BELONG TO SAME MONTH WE CHECK BOTH CONDITIONS(AFTER DEPARTURE AND BEFORE ARRIVAL)
						List<Flight> validFlights = new ArrayList<>();
						Stream<Flight> stream = departureDay.getFlights().stream();
						if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
							if(departureDay.getDay() == arrivalDateTime.getDayOfMonth()){
								validFlights.addAll(stream.filter(fromTime(departureDateTime).and(toTime(arrivalDateTime))).collect(Collectors.toList()));
							}else{
								validFlights.addAll(stream.filter(fromTime(departureDateTime)).collect(Collectors.toList()));
							}
						}else if(departureDay.getDay() == arrivalDateTime.getDayOfMonth()){
							validFlights.addAll(stream.filter(toTime(arrivalDateTime)).collect(Collectors.toList()));
						}else if(departureDay.getDay() > departureDateTime.getDayOfMonth() && departureDay.getDay() < arrivalDateTime.getDayOfMonth()){
							validFlights.addAll(departureDay.getFlights());
						}
						if(!validFlights.isEmpty()){
							Day validDay = new Day(departureDay.getDay(),validFlights);
							days.add(validDay);
						}
					}else{
						//IF BOTH DATES BELONG TO DIFFERENT MONTHS WE CHECK DEPARTURE CONDITION ONLY IF IS DEPARTURE MONTH
						// AND ARRIVAL CONDITION ONLY IF IS ARRIVAL MONTH
						if(count == 0){
							//CHECK AFTER DEPARTURE ONLY IF IT´S DEPARTURE DAY
							if(departureDay.getDay() == departureDateTime.getDayOfMonth()){
								List<Flight> validFlights = departureDay.getFlights().stream().filter(fromTime(departureDateTime))
										.collect(Collectors.toList());
								Day validDay = new Day(departureDay.getDay(),validFlights);
								days.add(validDay);
							}else if(departureDay.getDay() > departureDateTime.getDayOfMonth()){
								days.add(departureDay);
							}
						}else if(count == difference){
							//CHECK BEFORE ARRIVAL ONLY IF IT´S ARRIVAL DAY
							if(departureDay.getDay() == arrivalDateTime.getDayOfMonth()){
								List<Flight> validFlights = departureDay.getFlights().stream().filter(toTime(arrivalDateTime))
										.collect(Collectors.toList());
								Day validDay = new Day(departureDay.getDay(),validFlights);
								days.add(validDay);
							}else if(departureDay.getDay() < arrivalDateTime.getDayOfMonth()){
								days.add(departureDay);
							}
						}else{
							//IF CURRENT MONTH IS BETWEEN EACH MONTH DATES WE ADD ALL FLIGHTS
							days.add(departureDay);
						}
					}
				});
				if(!days.isEmpty()){
					log.info(days.size() + " days of " + departureAux.getMonth() + " have direct flights");
					result.getLegs().addAll(addFlights(departure, arrival, departureAux,days));
				}
			}
		}
		return result;
	}
	/**
	 * Get arrival flights of flight with stops
	 * @param arrivalDateTime
	 * @param difference
	 * @param i
	 * @param departureTimeAux
	 * @param arrivalDay
	 * @param departureDay
	 * @param departureFlight
	 * @return
	 */
	private List<Day> getArrivalDates(LocalDateTime arrivalDateTime, long difference, int i,
			LocalDateTime departureTimeAux, Day arrivalDay, Flight departureFlight) {
		List<Day> daysArrival = new ArrayList<Day>();
		//FLIGHT MUST DEPARTURE AT LEATS 2 HOURS LATER THAN ARRIVAL TIME OF DEPARTURE FLIGHT
		LocalDateTime departureDate = LocalDateTime.of(departureTimeAux.toLocalDate().withDayOfMonth(arrivalDay.getDay()),
				departureFlight.getArrivalTime()).plusHours(2);
		if(departureDate.getDayOfMonth() == arrivalDay.getDay()){
			log.debug("Looking for flights departing after:" + departureDate);
			if(difference == 0 || difference == i){
				List<Flight> validFlights = new ArrayList<Flight>();
				if(arrivalDay.getDay() == arrivalDateTime.getDayOfMonth()){
					//IS ARRIVAL MONTH
					// CHECK THATS ARRIVES BEFORE ARRIVALTIME
					validFlights = arrivalDay.getFlights().stream().filter(fromTime(departureDate).and(toTime(arrivalDateTime))).collect(Collectors.toList());
				}else if(arrivalDay.getDay() < arrivalDateTime.getDayOfMonth()){
					//ONLY CHECK DEPARTURE TIME AS WILL ARRIVE BEFORE ARRIVALTIME
					validFlights = arrivalDay.getFlights().stream().filter(fromTime(departureDate)).collect(Collectors.toList());
				}
				Day validDay = new Day(arrivalDay.getDay(),validFlights);
				daysArrival.add(validDay);
			}else{
				//ALL MONTH EXCEP ARRIVAL
				//ONLY CHECK DEPARTURE TIME AS WILL ARRIVE BEFORE ARRIVALTIME
				List<Flight> validFlights = arrivalDay.getFlights().stream().filter(fromTime(departureDate)).collect(Collectors.toList());
				Day validDay = new Day(arrivalDay.getDay(),validFlights);
				daysArrival.add(validDay);
			}
		}else{
			log.warn(departureDate + " is in another day than:" + arrivalDay.getDay());
		}
		return daysArrival;
	}
	
	/**
	 * Get departures flights of flight with stops
	 * @param departureDateTime
	 * @param arrivalDateTime
	 * @param difference
	 * @param i
	 * @param departureDay
	 * @return
	 */
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
	 * Parse flights found to legs
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

	/**
	 * Parse stops flights to legs
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param departureDay
	 * @param departurFlight
	 * @param arrivalDays
	 * @param stop
	 * @return
	 */
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
		log.info("Total routes found: "+ routes.size());
		return routes;
	}
	
	//PREDICATES
	/**
	 * Filter Flights with departuretime after @departureDateTime
	 * @param departureDateTime
	 * @return
	 */
	private Predicate<Flight> fromTime(LocalDateTime departureDateTime){
		return (x ->  x.getDepartureTime().compareTo(departureDateTime.toLocalTime()) >= 0);
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