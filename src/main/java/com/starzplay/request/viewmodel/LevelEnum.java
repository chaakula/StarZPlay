package com.starzplay.request.viewmodel;

/**
 * Level Request Parameter, to allow only specific values.
 * Allowed values are "censored" and "uncensored".
 * @author Chandra Sekhar Babu A
 *
 */
public enum LevelEnum {
	
	censored("censored", "censored"), uncensored("uncensored", "uncensored");

	LevelEnum(String code, String value) {
		this.code = code;
		this.value = value;
	}

	public String getCode() {
		return code;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setCode(String code) {
		this.code = code;
	}

	String code;
	String value;
}
