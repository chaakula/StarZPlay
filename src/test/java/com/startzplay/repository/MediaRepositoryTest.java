package com.startzplay.repository;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;

import com.startzplay.ControllerTestConfig;
import com.startzplay.ReadJSONUtil;
import com.starzplay.exception.AppException;
import com.starzplay.exception.GlobalExceptionHandler;
import com.starzplay.external.provider.content.response.ExternalContentResponse;
import com.starzplay.repository.MediaRepository;
import com.starzplay.request.viewmodel.FilterEnum;
import com.starzplay.request.viewmodel.LevelEnum;
import com.starzplay.rest.processor.IRestServiceProcessor;

@ContextConfiguration(classes = { ControllerTestConfig.class })
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaRepositoryTest {

    private MockMvc mockMvc;

    @InjectMocks
    MediaRepository mediaRepo;
    
    @InjectMocks
    GlobalExceptionHandler controlerAdvice;
    
    @Mock
    private IRestServiceProcessor restProcessor;

    @Rule
	public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setUp() {
    	MockitoAnnotations.initMocks(this);
    	this.mockMvc = MockMvcBuilders.standaloneSetup(mediaRepo).setControllerAdvice(controlerAdvice).build();
        
    }

    @Test
    public void testEvenIfServiceReturnNull() throws Exception {
    	expectedException.expect(AppException.class);
    	Mockito.when(restProcessor.execute(Matchers.any(String.class), Matchers.any(Class.class))).thenReturn(null);
    	ExternalContentResponse media = mediaRepo.getMedia(FilterEnum.censoring, LevelEnum.censored);
    	Assert.notNull(media);
    }
    
    @Test
    public void testEvenIfServiceReturnSuccessResponse() throws Exception {
		Mockito.when(restProcessor.execute(Matchers.any(String.class), Matchers.any(Class.class))).thenReturn(mockResponse());
		ExternalContentResponse media = mediaRepo.getMedia(FilterEnum.censoring, LevelEnum.censored);
		assertEquals(4, media.getEntries().size());
    }

	private ResponseEntity<ExternalContentResponse> mockResponse() {
		ReadJSONUtil util = new ReadJSONUtil();
		ResponseEntity<ExternalContentResponse> resp = new ResponseEntity<ExternalContentResponse>(util.mockResponse(),HttpStatus.OK);
		return resp;
	}
    
}