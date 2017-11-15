package com.starzplay.rest.processor;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.starzplay.exception.AppException;
import com.starzplay.exception.ErrorCode;

/**
 * 
 * @author Chandra Sekhar Babu A
 *
 */
@Component
public class RestServiceProcessor implements IRestServiceProcessor {

	private Integer defaultConnectionTimeout = 10000;

	private Integer defaultReadTimeout = 60000;

	private static ObjectMapper objectMapper;

	private static List<HttpMessageConverter<?>> messageConverters;

	static {
		objectMapper = new ObjectMapper();
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		module.setPriority(JaxbAnnotationModule.Priority.SECONDARY);
		objectMapper.registerModule(module);
		objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		messageConverters = newArrayList();
		messageConverters.add(new MappingJackson2HttpMessageConverter(objectMapper));
		messageConverters.add(new FormHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());

	}

	/**
	 * 
	 * @return RestTemplate
	 */
	private RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate(messageConverters);
		HttpComponentsClientHttpRequestFactory reqFactory = new HttpComponentsClientHttpRequestFactory();
		reqFactory.setConnectTimeout(defaultConnectionTimeout);
		reqFactory.setConnectionRequestTimeout(defaultConnectionTimeout);

		reqFactory.setReadTimeout(defaultReadTimeout);

		restTemplate.setRequestFactory(reqFactory);
		restTemplate.setErrorHandler(CustomErrorHandler.getInstance());
		return restTemplate;
	}


	/*
	 * (non-Javadoc)
	 * @see com.starzplay.rest.processor.IRestServiceProcessor#execute(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T, V> ResponseEntity<T> execute(String url, Class<T> clazz) throws AppException {
		try {
			HttpHeaders headers = getHeaders();
			ResponseEntity<T> response;
			HttpEntity<V> request = new HttpEntity<>(headers);
			RestTemplate restTemplate = getRestTemplate();
			response = restTemplate.exchange(url, HttpMethod.GET, request, clazz);
			return response;
		} catch (RestClientException ex) {
			throw new AppException(ErrorCode.ERR_WHILE_CALLING, ErrorCode.ERR_WHILE_CALLING.getMessage(), ex);
		}
	}

	/**
	 * 
	 * @return
	 */
	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		Map<String, String> customHeader = new HashMap<>();
		customHeader.put("Accept", MediaType.APPLICATION_JSON_VALUE);
		customHeader.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.setAll(customHeader);
		return headers;
	}

	/**
	 * Custom Error Handler for Rest processor
	 */
	private static class CustomErrorHandler extends DefaultResponseErrorHandler {
		private static final CustomErrorHandler INSTANCE = new CustomErrorHandler();

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			String rawResponse = CharStreams.toString(new InputStreamReader(response.getBody(), Charsets.UTF_8));
			throw new RestClientException("Status Code - > " + response.getStatusCode() + ", Status Text -> "
					+ response.getStatusText() + ", Response Body ->" + rawResponse);
		}

		static CustomErrorHandler getInstance() {
			return INSTANCE;
		}
	}

}