package com.all.rest.beans;

import java.util.Date;

import com.all.shared.model.Folder;
import com.all.shared.model.LazyPlaylist;

public class TopHundredPlaylist extends LazyPlaylist {
	private static final long serialVersionUID = 1L;

	private Date creationDate;
	private boolean expired;
	private String hashcode;
	private Date modifiedDate;
	private String name;

	public TopHundredPlaylist() {
	}

	@Override
	public String getOwner() {
		return "top";
	}

	@Override
	public Folder getParentFolder() {
		return null;
	}

	@Override
	public Date getModifiedDate() {
		return modifiedDate;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public Date getLastPlayed() {
		return modifiedDate;
	}

	@Override
	public boolean isSmartPlaylist() {
		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHashcode() {
		return hashcode;
	}

	@Override
	public boolean isNewContent() {
		return true;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public void setHashcode(String hashcode) {
		this.hashcode = hashcode;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isExpired() {
		return expired;
	}

}
