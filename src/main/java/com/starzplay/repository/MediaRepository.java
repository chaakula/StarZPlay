package com.starzplay.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import com.starzplay.common.util.RestURLConstants;
import com.starzplay.exception.AppException;
import com.starzplay.exception.ErrorCode;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;
import com.starzplay.rest.processor.IRestServiceProcessor;

/**
 * Media Repository class
 * @author Chandra Sekhar Babu A
 *
 */
@Repository
public class MediaRepository implements IMediaRepository {

	@Autowired
	IRestServiceProcessor restProcessor;
	
	/*
	 * Retrieve the media from external source
	 * @see com.starzplay.repository.IMediaRepository#getMedia(com.starzplay.request.viewmodel.FilterEnum, com.starzplay.request.viewmodel.LevelEnum)
	 */
	@Override
	public ExternalContentResponse getMedia(FilterEnum filter, LevelEnum level) throws AppException {

		ResponseEntity<ExternalContentResponse> response = restProcessor.execute(RestURLConstants.VENDOR_MEDIA_URL,
				ExternalContentResponse.class);
		
		if (response != null) {
			ExternalContentResponse contentResponse = response.getBody();
			return contentResponse;
		} else {
			throw new AppException(ErrorCode.ERR_WHILE_CALLING, ErrorCode.ERR_WHILE_CALLING.getMessage());
		}

	}

}
