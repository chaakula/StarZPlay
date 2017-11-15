package com.starzplay.exception;

/**
 * Error codes, right now configured as Enum, later message can be moved to Data base
 * @author Chandra Sekhar Babu A
 *
 */
public enum ErrorCode {

	ERR_WHILE_CALLING("ERR_WHILE_CALLING", "Error while calling service"), 
	INVALID_ARGUMENT("INVALID_ARGUMENT", "Invalid Argument passed"), 
	PARSER_EXCEPTION("PARSER_EXCEPTION", "Parsing failed");

	ErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	String code;
	String message;
}
