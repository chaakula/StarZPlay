package com.starzplay.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.starzplay.common.util.CommonConstants;
import com.starzplay.exception.AppException;
import com.starzplay.external.provider.content.response.Entry;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.repository.IMediaRepository;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;

/**
 * Media service class
 * 
 * @author Chandra Sekhar Babu A
 *
 */
@Service
public class MediaService implements IMediaService {

	@Autowired
	IMediaRepository mediaRepo;

	@Override
	public ExternalContentResponse getMedia(FilterEnum filter, LevelEnum level) throws AppException {
		ExternalContentResponse response = mediaRepo.getMedia(filter, level);
		response = filterMedia(response, filter, level);
		return response;
	}

	/**
	 * 
	 * @param response
	 * @param filter
	 * @param level
	 * @return
	 */
	private ExternalContentResponse filterMedia(ExternalContentResponse response, FilterEnum filter, LevelEnum level) {

		if ((level == null || CommonConstants.EMPTY.equals(level.getCode()))) {
			return response;
		}
		// List<EntryViewModel> filteredList =
		// response.getEntries().parallelStream().filter(ent ->
		// ent.getPeg$contentClassification().equalsIgnoreCase(level.getCode())).collect(Collectors.toList());

		// In any case, our response should only contain 1 element in the media array.
		// so finding first element and returning
		// if we need full list we can use above commented code, which will return
		// complte matching list

		Optional<Entry> findFirst = null;
		if (level.getCode().equalsIgnoreCase(LevelEnum.censored.getCode())) {
			findFirst = response.getEntries().parallelStream()
					.filter(ent -> ent.getPeg$contentClassification().equalsIgnoreCase(level.getCode())
							&& ent.getGuid().endsWith(CommonConstants.GUID_ENDS_WITH_CHARACTER)

					).findFirst();
		} else {
			findFirst = response.getEntries().parallelStream()
					.filter(ent -> ent.getPeg$contentClassification().equalsIgnoreCase(level.getCode())).findFirst();
		}
		List<Entry> filteredList = new ArrayList<>();

		if (findFirst.isPresent()) {
			filteredList.add(findFirst.get());
		}

		response.setEntries(filteredList);
		return response;
	}

}
