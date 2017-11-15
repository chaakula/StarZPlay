package com.startzplay;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starzplay.external.provider.content.response.ExternalContentResponse;

public class ReadJSONUtil {

	public ExternalContentResponse mockResponse() {
		ObjectMapper mapper = new ObjectMapper();
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			ExternalContentResponse obj = mapper.readValue(new File(classLoader.getResource("content.json").getFile()),
					ExternalContentResponse.class);

			return obj;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
