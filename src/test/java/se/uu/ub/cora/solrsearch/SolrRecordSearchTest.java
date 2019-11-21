/*
 * Copyright 2017, 2019 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.solrsearch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.converter.JsonToDataConverterProvider;
import se.uu.ub.cora.json.parser.JsonObject;
import se.uu.ub.cora.json.parser.JsonParser;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.solrindex.SolrClientProviderSpy;
import se.uu.ub.cora.solrindex.SolrClientSpy;

public class SolrRecordSearchTest {
	private SolrClientProviderSpy solrClientProvider;
	private SearchStorageSpy searchStorage;
	private SolrRecordSearch solrSearch;
	private SolrClientSpy solrClientSpy;
	private QueryResponseSpy queryResponse;
	private List<String> emptyList = new ArrayList<>();
	private JsonToDataConverterFactorySpy jsonToDataConverterFactory;

	@BeforeMethod
	public void beforeMethod() {
		solrClientProvider = new SolrClientProviderSpy();
		searchStorage = new SearchStorageSpy();
		jsonToDataConverterFactory = new JsonToDataConverterFactorySpy();
		JsonToDataConverterProvider.setJsonToDataConverterFactory(jsonToDataConverterFactory);

		solrSearch = SolrRecordSearch.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(
				solrClientProvider, searchStorage);
		solrClientSpy = solrClientProvider.solrClientSpy;
		queryResponse = new QueryResponseSpy();
		solrClientSpy.queryResponse = queryResponse;
	}

	@Test
	public void testInit() {
		assertNotNull(solrSearch);
		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertNull(solrQueryCreated);
		assertEquals(solrSearch.getSearchStorage(), searchStorage);
	}

	@Test
	public void testGetSolrClientProvider() {
		assertEquals(solrSearch.getSolrClientProvider(), solrClientProvider);
	}

	@Test
	public void testSearchOneParameterNoRecordType() {
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("titleSearchTerm",
				"A title");

		SearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);
		assertNotNull(searchResult.listOfDataGroups);
		String resultFromSpyAsJsonFormattedString = getResultFromSpyAsJsonFormattedString();

		JsonObject jsonObject = (JsonObject) jsonToDataConverterFactory.jsonValue;
		jsonObject.toJsonFormattedString();
		assertEquals(jsonObject.toJsonFormattedString(), resultFromSpyAsJsonFormattedString);

		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertEquals(solrQueryCreated.getQuery(), "title_s:(A title)");

		assertEquals(searchStorage.searchTermIds.get(0), "titleSearchTerm");
		assertEquals(searchStorage.collectIndexTermIds.get(0), "titleIndexTerm");
		assertEquals((int) solrQueryCreated.getRows(), 100);
	}

	private String getResultFromSpyAsJsonFormattedString() {
		String resultFromSpy = (String) solrClientSpy.queryResponse.getResults().get(0)
				.getFirstValue("recordAsJson");

		JsonParser jsonParser = new OrgJsonParser();
		JsonObject jsonValue = (JsonObject) jsonParser.parseString(resultFromSpy);
		String resultFromSpyAsJsonFormattedString = jsonValue.toJsonFormattedString();
		return resultFromSpyAsJsonFormattedString;
	}

	private DataGroup createSearchIncludeDataWithSearchTermIdAndValue(String searchTermId,
			String value) {
		DataGroup searchData = createSearchDataGroupWithMinimumNecessaryParts();
		DataGroup include = searchData.getFirstGroupWithNameInData("include");
		DataGroup includePart = include.getFirstGroupWithNameInData("includePart");
		includePart.addChild(new DataAtomicSpy(searchTermId, value));
		return searchData;
	}

	private DataGroup createSearchDataGroupWithMinimumNecessaryParts() {
		DataGroup searchData = createMinimumSearchData();
		return searchData;
	}

	private DataGroup createMinimumSearchData() {
		DataGroup searchData = new DataGroupSpy("bookSearch");
		DataGroup include = new DataGroupSpy("include");
		searchData.addChild(include);
		DataGroup includePart = new DataGroupSpy("includePart");
		include.addChild(includePart);
		return searchData;
	}

	@Test
	public void testIndexTypeTextGeneratesCorrectQueryParamSuffix() {
		SolrQuery solrQueryCreated = performIncludeSearchForIndexType("indexTypeText");
		String suffix = extractCreatedParameterSuffix(solrQueryCreated);
		assertEquals(suffix, "_t");
	}

	private String extractCreatedParameterSuffix(SolrQuery solrQueryCreated) {
		String query = solrQueryCreated.getQuery();
		return query.substring(query.indexOf('_'), query.indexOf(':'));
	}

	private SolrQuery performIncludeSearchForIndexType(String indexType) {
		searchStorage.indexTypeToReturn = indexType;
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("titleSearchTerm",
				"A title");
		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		return (SolrQuery) solrClientSpy.params;
	}

	@Test
	public void testIndexTypeBooleanGeneratesCorrectQueryParamSuffix() {
		SolrQuery solrQueryCreated = performIncludeSearchForIndexType("indexTypeBoolean");
		String suffix = extractCreatedParameterSuffix(solrQueryCreated);
		assertEquals(suffix, "_b");
	}

	@Test
	public void testIndexTypeDateGeneratesCorrectQueryParamSuffix() {
		SolrQuery solrQueryCreated = performIncludeSearchForIndexType("indexTypeDate");
		String suffix = extractCreatedParameterSuffix(solrQueryCreated);
		assertEquals(suffix, "_dt");
	}

	@Test
	public void testIndexTypeNumberGeneratesCorrectQueryParamSuffix() {
		SolrQuery solrQueryCreated = performIncludeSearchForIndexType("indexTypeNumber");
		String suffix = extractCreatedParameterSuffix(solrQueryCreated);
		assertEquals(suffix, "_l");
	}

	@Test
	public void testReturnNumberOfRecordsFound() {
		DataGroup searchData = createSearchDataGroupWithMinimumNecessaryParts();

		SearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);
		assertEquals(searchResult.start, 1);
		assertEquals(searchResult.totalNumberOfMatches, 1);
	}

	@Test
	public void testReturnThreeRecords() {
		queryResponse.noOfDocumentsToReturn = 3;
		queryResponse.noOfDocumentsFound = 42;

		DataGroup searchData = createSearchDataGroupWithMinimumNecessaryParts();

		SearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);
		assertEquals(searchResult.listOfDataGroups.size(), 3);
		assertEquals(searchResult.start, 1);
		assertEquals(searchResult.totalNumberOfMatches, 42);
	}

	@Test
	public void testSearchWithLimitOnRows() {
		int rows = 2;
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("rows", String.valueOf(rows)));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals((int) ((SolrQuery) solrClientSpy.params).getStart(), 0);
		assertEquals((int) ((SolrQuery) solrClientSpy.params).getRows(), rows);
	}

	@Test
	public void testSearchWithOtherLimitOnRows() {
		int rows = 5;
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("rows", String.valueOf(rows)));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals((int) ((SolrQuery) solrClientSpy.params).getStart(), 0);
		assertEquals((int) ((SolrQuery) solrClientSpy.params).getRows(), rows);
	}

	@Test
	public void testSearchWhenRowsNotAnInt() {
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("rows", "notAnInt"));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals((int) ((SolrQuery) solrClientSpy.params).getStart(), 0);
		assertEquals((int) ((SolrQuery) solrClientSpy.params).getRows(), 100);
	}

	@Test
	public void testSearchFromStartPosition() {
		int start = 2;
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("start", String.valueOf(start)));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		SolrQuery solrQuery = (SolrQuery) solrClientSpy.params;
		assertEquals((int) solrQuery.getStart(), start - 1);
		assertEquals((int) solrQuery.getRows(), 100);
	}

	@Test
	public void testSearchFromOtherStartPosition() {
		int start = 7;
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("start", String.valueOf(start)));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals((int) ((SolrQuery) solrClientSpy.params).getStart(), start - 1);
		assertEquals((int) ((SolrQuery) solrClientSpy.params).getRows(), 100);
	}

	@Test
	public void testSearchFromStartPositionStartNotAnInt() {
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("start", "notAnInt"));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		SolrQuery solrQuery = (SolrQuery) solrClientSpy.params;
		assertEquals((int) solrQuery.getStart(), 0);
		assertEquals((int) solrQuery.getRows(), 100);
	}

	@Test
	public void testSearchFromStartPositionWithLimitOnRows() {
		int start = 42;
		int rows = 23;
		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("start", String.valueOf(start)));
		searchData.addChild(new DataAtomicSpy("rows", String.valueOf(rows)));

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals((int) ((SolrQuery) solrClientSpy.params).getStart(), start - 1);
		assertEquals((int) ((SolrQuery) solrClientSpy.params).getRows(), rows);
	}

	@Test
	public void testSearchFromStartPositionWithLimitOnRowsInABodyOfDocuments() {
		int start = 42;
		int rows = 23;
		int documentsToFind = 42341;
		queryResponse.noOfDocumentsFound = documentsToFind;
		queryResponse.noOfDocumentsToReturn = rows;

		DataGroup searchData = createMinimumSearchData();
		searchData.addChild(new DataAtomicSpy("start", String.valueOf(start)));
		searchData.addChild(new DataAtomicSpy("rows", String.valueOf(rows)));

		SearchResult result = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);

		assertEquals(result.totalNumberOfMatches, documentsToFind);
		assertEquals(result.start, 42);
		assertEquals(result.listOfDataGroups.size(), rows);
	}

	@Test(expectedExceptions = SolrSearchException.class)
	public void testSearchErrorException() {
		solrClientProvider.returnErrorThrowingClient = true;
		queryResponse.noOfDocumentsToReturn = 3;

		DataGroup searchData = createSearchDataGroupWithMinimumNecessaryParts();

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);
	}

	@Test
	public void testSearchUndefinedFieldErrorException() {
		solrClientProvider.returnErrorThrowingClient = true;
		solrClientProvider.errorMessage = "Error from server at http://localhost:8983/solr/coracore: undefined field testNewsTitleSearchTerm";

		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("anUnindexedTerm",
				"A title");

		SearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(emptyList, searchData);
		assertEquals(searchResult.listOfDataGroups.size(), 0);
	}

	@Test
	public void testSearchOneParameterOneRecordType() {
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("titleSearchTerm",
				"A title");

		List<String> recordTypeList = new ArrayList<>();
		recordTypeList.add("someRecordType");
		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypeList, searchData);

		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		String[] createdFilterQueries = solrQueryCreated.getFilterQueries();
		assertEquals(createdFilterQueries[0], "type:someRecordType");
	}

	@Test
	public void testSearchOneParameterTwoRecordType() {
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("titleSearchTerm",
				"A title");

		List<String> recordTypeList = new ArrayList<>();
		recordTypeList.add("someRecordType");
		recordTypeList.add("someOtherRecordType");
		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypeList, searchData);

		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		String[] createdFilterQueries = solrQueryCreated.getFilterQueries();
		assertEquals(createdFilterQueries[0],
				"type:someRecordType" + " OR " + "type:someOtherRecordType");
	}

	@Test
	public void testSearchOneParameterWithColonCharacterOneRecordType() {
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue("titleSearchTerm",
				"A title: with a colon character");

		List<String> recordTypeList = new ArrayList<>();
		recordTypeList.add("someRecordType");
		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypeList, searchData);

		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertEquals(solrQueryCreated.getQuery(), "title_s:(A title\\: with a colon character)");

		String[] createdFilterQueries = solrQueryCreated.getFilterQueries();
		assertEquals(createdFilterQueries[0], "type:someRecordType");
	}

	@Test
	public void testSearchLInkedDataOneParameterOneRecordType() {
		DataGroup searchData = createSearchIncludeDataWithSearchTermIdAndValue(
				"linkedTextSearchTerm", "textToSearchFor");

		List<String> recordTypeList = new ArrayList<>();
		recordTypeList.add("someRecordType");
		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypeList, searchData);

		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		String[] createdFilterQueries = solrQueryCreated.getFilterQueries();
		assertEquals(createdFilterQueries[0], "type:someRecordType");
		assertEquals(solrQueryCreated.getQuery(),
				"{!join from=ids to=textId_s}swedish_t:textToSearchFor AND type:coraText");
	}

}
