package com.starzplay.rest.processor;

import org.springframework.http.ResponseEntity;

import com.starzplay.exception.AppException;

/**
 * Rest processor to handle restful web services
 * 
 * @author Chandra Sekhar Babu A
 *
 */
public interface IRestServiceProcessor {

	public <T, V> ResponseEntity<T> execute(String url, Class<T> clazz) throws AppException;
}
