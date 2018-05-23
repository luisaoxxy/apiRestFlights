package com.ryanair.flights.restApi;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.ryanair.flights.restApi.domain.Route;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestApiApplicationTests {

	@Test
	public void contextLoads() {
		LocalDateTime today = LocalDateTime.now();
		LocalDateTime yesterday = today.minusDays(1);
		LocalDateTime monthLater = yesterday.plusMonths(1).withDayOfMonth(1);
		LocalDateTime twoMonthLater = yesterday.plusMonths(2).withDayOfMonth(1);
		long difference = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), monthLater);
		System.out.println(difference);
		difference = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), twoMonthLater);;
		System.out.println(difference);
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
