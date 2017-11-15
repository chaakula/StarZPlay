package com.startzplay.controller;
import org.junit.Before;
import org.junit.Test;
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
import com.starzplay.controller.MediaController;
import com.starzplay.exception.GlobalExceptionHandler;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;
import com.starzplay.response.viewmodel.AppResponse;
import com.starzplay.service.MediaService;

@ContextConfiguration(classes = { ControllerTestConfig.class })
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    MediaController mediaController;
    
    @InjectMocks
    GlobalExceptionHandler controlerAdvice;
    
    @Mock
    private MediaService mediaService;

    @Before
    public void setUp() {
    	MockitoAnnotations.initMocks(this);
    	this.mockMvc = MockMvcBuilders.standaloneSetup(mediaController).setControllerAdvice(controlerAdvice).build();
        
    }

    @Test
    public void testEvenIfServiceReturnNull() throws Exception {
    	Mockito.when(mediaService.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class))).thenReturn(null);
    	AppResponse media = mediaController.getMedia(FilterEnum.censoring, LevelEnum.censored);
    	Assert.notNull(media);
    }
    
    @Test
    public void testEvenIfServiceReturnSuccessResponse() throws Exception {
		Mockito.when(mediaService.getMedia(Matchers.any(FilterEnum.class), Matchers.any(LevelEnum.class))).thenReturn(mockResponse());
    	AppResponse media = mediaController.getMedia(FilterEnum.censoring, LevelEnum.censored);
    	Assert.notNull(media);
    }

	private ExternalContentResponse mockResponse() {
		ReadJSONUtil util = new ReadJSONUtil();
		return util.mockResponse();
	}
    
}