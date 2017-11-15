package com.starzplay.response.viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * App Response class, this will be exposed to UI, either success response or error response
 * @author Chandra Sekhar Babu A
 *
 */
public class AppResponse {

	@JsonProperty("success")
	Object responseViewModel;

	@JsonProperty("error")
	Object errorViewModel;

	public AppResponse() {
		super();
	}

	public AppResponse(ResponseViewModel responseViewModel, Object errorViewModel) {
		super();
		this.responseViewModel = responseViewModel;
		this.errorViewModel = errorViewModel;
	}

	public Object getResponseViewModel() {
		return responseViewModel;
	}

	public void setResponseViewModel(Object responseViewModel) {
		this.responseViewModel = responseViewModel;
	}

	public Object getErrorViewModel() {
		return errorViewModel;
	}

	public void setErrorViewModel(Object errorViewModel) {
		this.errorViewModel = errorViewModel;
	}

}
