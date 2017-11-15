package com.starzplay.repository;

import com.starzplay.exception.AppException;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;

/**
 * Media Repository Interface
 * @author Chandra Sekhar Babu A
 *
 */
public interface IMediaRepository {

	public ExternalContentResponse getMedia(FilterEnum filter, LevelEnum level) throws AppException;
}
