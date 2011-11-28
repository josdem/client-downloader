package com.all.rest.mc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.all.messengine.Message;
import com.all.messengine.MessageListener;
import com.all.messengine.impl.StubMessEngine;
import com.all.messengine.support.MessEngineConfigurator;
import com.all.rest.web.RestService;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Category;

public class TestRestTopPlaylistService {
	@Mock
	private RestService restService;

	@Spy
	private StubMessEngine messEngine = new StubMessEngine();

	@InjectMocks
	private TopHundredRestService topService = new TopHundredRestService();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldSetTheMessengineListeningMessages() throws Exception {
		new MessEngineConfigurator(messEngine).setupMessEngine(topService);
		verify(messEngine).addMessageListener(eq(MessEngineConstants.TOP_HUNDRED_CATEGORY_LIST_REQUEST),
				any(MessageListener.class));
	}

	@Test
	public void shouldGetPlaylistsForCategory() throws Exception {
		shouldSetTheMessengineListeningMessages();

		List<Category> list = new ArrayList<Category>();
		when(restService.findTopCategories()).thenReturn(list);
		messEngine.send(new AllMessage<Void>(MessEngineConstants.TOP_HUNDRED_CATEGORY_LIST_REQUEST, null));
		Message<?> message = messEngine.getCurrentMessage();
		assertEquals(list, message.getBody());
		assertEquals(MessEngineConstants.TOP_HUNDRED_CATEGORY_LIST_RESPONSE, message.getType());
	}

}
