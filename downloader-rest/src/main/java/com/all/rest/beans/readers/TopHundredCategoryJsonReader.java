package com.all.rest.beans.readers;

import java.util.Date;

import net.sf.json.JSONObject;

import com.all.rest.beans.TopHundredCategory;
import com.all.shared.json.readers.JsonReader;

public class TopHundredCategoryJsonReader implements JsonReader<TopHundredCategory> {

	@Override
	public TopHundredCategory read(String json) {
		JSONObject jsonModel = JSONObject.fromObject(json);
		String name = jsonModel.getString("name");
		long id = jsonModel.getLong("id");
		TopHundredCategory category = new TopHundredCategory(id, name);
		category.setCreatedOn(new Date(jsonModel.getLong("createdOn")));
		category.setDescription(jsonModel.getString("description"));
		category.setModifiedOn(new Date(jsonModel.getLong("modifiedOn")));
		return category;
	}

}
