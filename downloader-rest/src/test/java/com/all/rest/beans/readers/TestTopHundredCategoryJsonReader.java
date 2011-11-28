package com.all.rest.beans.readers;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import com.all.rest.beans.TopHundredCategory;
import com.all.shared.json.JsonConverter;

public class TestTopHundredCategoryJsonReader {

	private static final String CAT1_JSON = "{\"createdOn\":1304018128000,\"description\":\"test category\",\"id\":1,\"modifiedOn\":1304018149000,\"name\":\"category\"}";
	public static final String JSON = "[" + CAT1_JSON + "]";

	@Test
	public void shouldConvertCategoryCorrectly() throws Exception {
		TopHundredCategory category = new TopHundredCategoryJsonReader().read(CAT1_JSON);
		assertCategory(category);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldTestReaderAsItShouldWork() throws Exception {
		JsonConverter.addJsonReader(TopHundredCategory.class, new TopHundredCategoryJsonReader());
		ArrayList collection = JsonConverter.toTypedCollection(JSON, ArrayList.class, TopHundredCategory.class);
		assertEquals(1, collection.size());
		assertCategory((TopHundredCategory) collection.get(0));
	}

	private void assertCategory(TopHundredCategory category) {
		assertEquals(1, category.getId());
		assertEquals("category", category.getName());
		assertEquals(new Date(1304018128000L), category.getCreatedOn());
		assertEquals("test category", category.getDescription());
		assertEquals(new Date(1304018149000L), category.getModifiedOn());
	}

}
