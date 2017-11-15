package com.starzplay.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.starzplay.response.viewmodel.AppResponse;

/**
 * ControllerAdvice class to handle to Exception handler for restful web services
 * @author Chandra Sekhar Babu A
 *
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Illegal argument exceptions handler
	 * @param invalidException
	 * @return
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<AppResponse> illegalArgumentExceptionHandler(IllegalArgumentException invalidException) {
		LOGGER.error(invalidException.getMessage(), invalidException);
		return new ResponseEntity<>(getResponse(invalidException.getMessage(), invalidException.getLocalizedMessage(), invalidException), HttpStatus.OK);
	}

	/**
	 * Conversion failed exception, param values validation failed will come here or any conversion failed exceptions will handle in this method
	 * @param convException
	 * @return
	 */
	@ExceptionHandler(ConversionFailedException.class)
	public ResponseEntity<AppResponse> conversionFailedExceptionHandler(ConversionFailedException convException) {
		LOGGER.error(convException.getMessage(), convException);
		return new ResponseEntity<>(getResponse(convException.getMessage(), convException.getLocalizedMessage(), convException), HttpStatus.OK);
	}
	
	/**
	 * App exception handler
	 * @param appException
	 * @return
	 */
	@ExceptionHandler(AppException.class)
	public ResponseEntity<AppResponse> appExceptionHandler(AppException appException) {
		LOGGER.error(appException.getMessage(), appException);
		return new ResponseEntity<>(getResponse(appException.getMessage(), appException.getLocalizedMessage(), appException), HttpStatus.OK);
	}

	/**
	 * Exception handler
	 * @param exception
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<AppResponse> exceptionHandler(Exception exception) {
		LOGGER.error(exception.getMessage(), exception);
		return new ResponseEntity<>(getResponse(exception.getMessage(), exception.getLocalizedMessage(), exception), HttpStatus.OK);
	}

	private AppResponse getResponse(String message, String localizedMessage, Exception ex) {
		AppResponse response = new AppResponse();
		response.setErrorViewModel(ErrorCode.INVALID_ARGUMENT);
		return response;
	}

}
