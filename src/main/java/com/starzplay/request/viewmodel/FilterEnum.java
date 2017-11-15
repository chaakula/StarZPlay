package com.starzplay.request.viewmodel;

/**
 * 'Filter' Request Parameter, to allow only specific values, Only allowed value
 * is "censoring".
 * 
 * @author Chandra Sekhar Babu A
 *
 */
public enum FilterEnum {
	censoring("censoring", "censoring");

	FilterEnum(String code, String value) {
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
