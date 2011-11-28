package com.all.rest.beans;

import java.util.Date;
import java.util.List;

import com.all.rest.web.RestService;
import com.all.shared.model.Category;
import com.all.shared.model.Playlist;

public class TopHundredCategory implements Category {
	private Date createdOn;
	private String description;
	private final long id;
	private Date modifiedOn;
	private final String name;
	private List<Playlist> playlists = null;
	private RestService restService;

	public TopHundredCategory(long id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getId() {
		return id;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getName() {
		return name;
	}

	public void setRestService(RestService restService) {
		this.restService = restService;
	}

	public List<Playlist> getPlaylists() {
		if (playlists == null || playlists.isEmpty()) {
			synchronized (this) {
				if (playlists == null || playlists.isEmpty()) {
					playlists = restService.findPlaylistsForTopCategories(this);
				}
			}
		}
		return playlists;
	}

	@Override
	public String toString() {
		return getName();
	}

}
