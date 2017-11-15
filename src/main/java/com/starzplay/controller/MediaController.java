package com.starzplay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.starzplay.common.util.CommonConstants;
import com.starzplay.common.util.RestURLConstants;
import com.starzplay.exception.AppException;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;
import com.starzplay.response.viewmodel.AppResponse;
import com.starzplay.service.IMediaService;

/**
 * Media controller to the application
 * 
 * @author Chandra Sekhar Babu A
 *
 */
@RestController
public class MediaController {

	@Autowired
	IMediaService mediaService;

	/**
	 * 
	 * @param filter
	 * @param level
	 * @return
	 * @throws AppException
	 */
	@RequestMapping(value = RestURLConstants.MEDIA_URL, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public AppResponse getMedia(@RequestParam(value = CommonConstants.FILTER, required = false) FilterEnum filter,
			@RequestParam(value = CommonConstants.LEVEL, required = false) LevelEnum level) throws AppException {

		// Service call
		ExternalContentResponse mediaResponse = mediaService.getMedia(filter, level);
		
		// Prepare Response
		AppResponse response = new AppResponse();
		response.setResponseViewModel(mediaResponse);

		return response;
	}

}
