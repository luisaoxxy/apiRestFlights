package com.ryanair.flights.restApi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ryanair.flights.restApi.domain.Day;
import com.ryanair.flights.restApi.domain.Flight;
import com.ryanair.flights.restApi.domain.Route;
import com.ryanair.flights.restApi.domain.TimeTable;
import com.ryanair.flights.restApi.service.InterconnectionServiceImpl;
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestApiApplicationTests {
	@Autowired
	private RestTemplate restTemplate;
	
	private List<Day> template = new ArrayList<Day>();
	
	@Before
	public void init() {
		Flight flight = null;
		List<Flight> flights = null;
		Day day = null;
		LocalDateTime now = LocalDateTime.now();
		for (int i=0; i < 5; i++) {
			flights = new ArrayList<Flight>();
			LocalTime timeAux =now.toLocalTime().withHour(0).plusHours(i);
			flight =  new Flight(String.valueOf(i), timeAux, timeAux);
			flights.add(flight);
			day = new Day(i, flights);
			template.add(day);
		}
	}
	@Test
	public void getTimeTable() {
		Map<String, String> uriParams = new HashMap<String, String>();
		uriParams.put("from", "DUB");
		uriParams.put("to", "WRO");
		uriParams.put("year", "2018");
		uriParams.put("month", "5");
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(InterconnectionServiceImpl.TIME_TABLE_URL);
		URI restUri = builder.buildAndExpand(uriParams).toUri();
		ResponseEntity<TimeTable> timeTableResponse = restTemplate.getForEntity(restUri,TimeTable.class);
		TimeTable timeTable = timeTableResponse.getBody();
		assertNotNull(timeTable);
		assertFalse(timeTable.getDays().isEmpty());
		assertTrue(timeTable.getDays().size() == 5);
		try{
			uriParams.put("month", "4");
			restUri = builder.buildAndExpand(uriParams).toUri();
			timeTableResponse = restTemplate.getForEntity(restUri,TimeTable.class);
		} catch (HttpClientErrorException ex)   {
			assertTrue(ex.getStatusCode() == HttpStatus.NOT_FOUND);
		}
	}
	
	@Test
	public void getRoutes() {
		ResponseEntity<List<Route>> routeResponse =
				restTemplate.exchange(InterconnectionServiceImpl.ROUTES_URL,HttpMethod.GET, null, new ParameterizedTypeReference<List<Route>>() {
				});
		List<Route> routes = routeResponse.getBody();
		assertNotNull(routes);
		assertFalse(routes.isEmpty());
	}
	
	@Test
	public void fromTime() {
		LocalTime timeAux = LocalTime.now().withHour(0);
		List<Flight> list = template.stream().map(Day::getFlights).flatMap(Collection::stream).filter(
				x ->  x.getDepartureTime().compareTo(timeAux) >= 0).collect(Collectors.toList());
		assertTrue(list.size() == 4);
	}
	
	@Test
	public void toTime() {
		LocalTime timeAux = LocalTime.now().withHour(0);
		List<Flight> list = template.stream().map(Day::getFlights).flatMap(Collection::stream).filter(
				x ->  x.getArrivalTime().compareTo(timeAux) <= 0).collect(Collectors.toList());
		assertTrue(list.size() == 1);
	}
}