package com.starzplay.service;

import com.starzplay.exception.AppException;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;

/**
 * Media Service interface
 * @author Chandra Sekhar Babu A
 *
 */
public interface IMediaService {

	public ExternalContentResponse getMedia(FilterEnum filter, LevelEnum level) throws AppException;
}
