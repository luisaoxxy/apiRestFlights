package com.ryanair.flights.restApi.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ryanair.flights.restApi.error.RequestError;

@ControllerAdvice
@RequestMapping(produces ="application/json")
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
	/**
	 * Manage invalid arguments exceptions
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,  HttpHeaders headers, 
	  HttpStatus status, WebRequest request) {
	    List<String> errors = new ArrayList<String>();
	    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
	        errors.add(error.getField() + ": " + error.getDefaultMessage());
	    }
	    for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
	        errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
	    }
	    RequestError error = new RequestError(HttpStatus.NOT_FOUND, ex.getLocalizedMessage(), errors);
	    return new ResponseEntity<Object>(error, new HttpHeaders(), error.getStatus());
	}
	/**
	 * Manage missing parameter exceptions
	 */
	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, 
	  HttpStatus status, WebRequest request) {
	    
		String errorMessage = ex.getParameterName() + " parameter is missing and is mandatory";
	    RequestError error = new RequestError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errorMessage);
	    return new ResponseEntity<Object>(error, new HttpHeaders(), error.getStatus());
	}
	/**
	 * Manage argument mismatch type exceptions
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
	    String errorMessage = ex.getName() + " should be of type " + ex.getRequiredType().getName();
	 
	    RequestError error = new RequestError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errorMessage);
	    return new ResponseEntity<Object>(error, new HttpHeaders(), error.getStatus());
	}
	/**
	 * Manage Method not allowed exceptions
	 */
	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, 
	  HttpHeaders headers,HttpStatus status,WebRequest request) {
	    StringBuilder builder = new StringBuilder();
	    builder.append(ex.getMethod());
	    builder.append(" method is not supported for this request. Supported methods are ");
	    ex.getSupportedHttpMethods().forEach(t -> builder.append(t + " "));
	 
	    RequestError error = new RequestError(HttpStatus.METHOD_NOT_ALLOWED, 
	      ex.getLocalizedMessage(), builder.toString());
	    return new ResponseEntity<Object>(error, new HttpHeaders(), error.getStatus());
	}
	/**
	 * Manage unexpected exceptions
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ Exception.class })
	public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
		RequestError error = new RequestError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage(), "unexpected error occurred");
	    return new ResponseEntity<Object>(error, new HttpHeaders(), error.getStatus());
	}
}