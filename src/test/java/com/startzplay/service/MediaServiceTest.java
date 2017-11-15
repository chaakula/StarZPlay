package com.startzplay.service;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;

import com.startzplay.ControllerTestConfig;
import com.startzplay.ReadJSONUtil;
import com.starzplay.exception.GlobalExceptionHandler;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.repository.MediaRepository;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;
import com.starzplay.service.MediaService;

@ContextConfiguration(classes = { ControllerTestConfig.class })
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaServiceTest {

	private MockMvc mockMvc;

	@InjectMocks
	MediaService mediaService;

	@InjectMocks
	GlobalExceptionHandler controlerAdvice;

	@Mock
	private MediaRepository mediaRepo;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(mediaService).setControllerAdvice(controlerAdvice).build();

	}

	@Test
	public void testEvenIfServiceReturnNull() throws Exception {
		expectedException.expect(Exception.class);
		Mockito.when(mediaRepo.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class)))
				.thenReturn(null);
		ExternalContentResponse media = mediaService.getMedia(FilterEnum.censoring, LevelEnum.censored);
	}

	@Test
	public void testEvenIfServiceReturnSuccessResponseLevelCensored() throws Exception {
		Mockito.when(mediaRepo.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class)))
				.thenReturn(mockResponse());
		ExternalContentResponse media = mediaService.getMedia(FilterEnum.censoring, LevelEnum.censored);
		assertEquals(0, media.getEntries().size());
	}

	@Test
	public void testEvenIfServiceReturnSuccessResponseLevelUnCensored() throws Exception {
		Mockito.when(mediaRepo.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class)))
				.thenReturn(mockResponse());
		ExternalContentResponse media = mediaService.getMedia(FilterEnum.censoring, LevelEnum.uncensored);
		assertEquals(1, media.getEntries().size());
	}

	@Test
	public void testEvenIfServiceReturnSuccessResponseLevelEmpty() throws Exception {
		Mockito.when(mediaRepo.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class)))
				.thenReturn(mockResponse());
		ExternalContentResponse media = mediaService.getMedia(FilterEnum.censoring, null);
		assertEquals(4, media.getEntries().size());
	}

	private ExternalContentResponse mockResponse() {
		ReadJSONUtil util = new ReadJSONUtil();
		return util.mockResponse();
	}

}