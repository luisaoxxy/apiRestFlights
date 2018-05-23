package com.ryanair.flights.restApi.domain;

import java.util.List;

public class TimeTable {

	private Integer month;
	private List<Day> days = null;

	public Integer getMonth() {
		return month;
	}

	public void setMonth(Integer month) {
		this.month = month;
	}

	public List<Day> getDays() {
		return days;
	}

	public void setDays(List<Day> days) {
		this.days = days;
	}

}
