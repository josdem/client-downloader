package com.all.rest.mc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.messengine.MessEngine;
import com.all.messengine.MessageMethod;
import com.all.rest.web.RestService;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Category;
import com.all.shared.model.Playlist;

@Service
public class TopHundredRestService {
	@Autowired
	private RestService restService;
	@Autowired
	private MessEngine messEngine;

	@MessageMethod(MessEngineConstants.TOP_HUNDRED_CATEGORY_LIST_REQUEST)
	public void getAllCategories(AllMessage<Void> message) {
		List<Category> categories = restService.findTopCategories();
		messEngine.send(new AllMessage<List<Category>>(MessEngineConstants.TOP_HUNDRED_CATEGORY_LIST_RESPONSE, categories));
	}

	@MessageMethod(MessEngineConstants.TOP_HUNDRED_RANDOM_PLAYLIST_REQUEST)
	public void getRandomPlaylist(AllMessage<Void> message) {
		Playlist playlist = restService.getRandomPlaylist();
		messEngine.send(new AllMessage<Playlist>(MessEngineConstants.TOP_HUNDRED_RANDOM_PLAYLIST_RESPONSE, playlist));
	}
}
