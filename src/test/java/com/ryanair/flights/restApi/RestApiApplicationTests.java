package com.ryanair.flights.restApi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.ryanair.flights.restApi.domain.Day;
import com.ryanair.flights.restApi.domain.Flight;
import com.ryanair.flights.restApi.domain.Route;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestApiApplicationTests {

	@Test
	public void contextLoads() {
		List<Day> days = new ArrayList<Day>();
		Day d1 = new Day(1,new ArrayList<Flight>());
		d1.getFlights().add(new Flight("10",null,null));
		d1.getFlights().add(new Flight("20",null,null));
		days.add(d1);
		Day d2 = new Day(2,new ArrayList<Flight>());
		d2.getFlights().add(new Flight("10",null,null));
		d2.getFlights().add(new Flight("20",null,null));
		days.add(d2);
		List<Day> aux = days.stream().filter(x-> x.getDay() == 1).
			map(f -> new Day(f.getDay(), 
					f.getFlights().stream().filter(y-> y.getNumber().equals("10")).collect(Collectors.toList())))
			.collect(Collectors.toList());
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Route>> rateResponse =
		        restTemplate.exchange("https://api.ryanair.com/core/3/routes",
		                    HttpMethod.GET, null, new ParameterizedTypeReference<List<Route>>() {
		            });
		List<Route> routes = rateResponse.getBody();
		System.out.println("Total"+ routes.size());
	}
	public static void main(String[] args){
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Route>> rateResponse =
		        restTemplate.exchange("https://api.ryanair.com/core/3/routes",
		                    HttpMethod.GET, null, new ParameterizedTypeReference<List<Route>>() {
		            });
		List<Route> routes = rateResponse.getBody();
		System.out.println("Total"+ routes.size());
	}

}
