package com.starzplay.exception;

/**
 * Application Custom exception class
 * 
 * @author Chandra Sekhar Babu A
 *
 */
public class AppException extends Exception {

	private static final long serialVersionUID = 8212956741507797342L;
	private String customMessage;
	private ErrorCode errCode;

	public AppException(ErrorCode errorCode, String customMessage, Throwable cause) {
		super(customMessage, cause);
		this.errCode = errorCode;
		this.customMessage = customMessage;
	}

	public AppException(ErrorCode errorCode, String customMessage) {
		super(customMessage);
		this.errCode = errorCode;
		this.customMessage = customMessage;
	}

	public AppException(String errorCode, Throwable cause) {
		super(errorCode, cause);
	}

	public AppException(String errorCode, String message) {
		super(message);
		this.customMessage = message;
	}

	public AppException() {
	}

	public AppException(String errorCode) {
		super(errorCode);
		this.customMessage = errorCode;
	}

	public String getCustomMessage() {
		return customMessage;
	}

	public void setCustomMessage(String customMessage) {
		this.customMessage = customMessage;
	}

	public ErrorCode getErrrCode() {
		return errCode;
	}

	public void setErrorCode(ErrorCode errorCode) {
		this.errCode = errorCode;
	}

}
