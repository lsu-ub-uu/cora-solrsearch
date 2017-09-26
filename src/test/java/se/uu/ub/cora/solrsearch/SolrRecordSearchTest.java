/*
 * Copyright 2017 Uppsala University Library
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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.searchstorage.SearchStorage;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.solrindex.SolrClientProviderSpy;
import se.uu.ub.cora.solrindex.SolrClientSpy;
import se.uu.ub.cora.spider.data.SpiderSearchResult;

public class SolrRecordSearchTest {
	@Test
	public void testInit() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SearchStorage searchStorage = new SearchStorageSpy();
		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(solrClientProvider,
						searchStorage);
		assertNotNull(solrSearch);
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertNull(solrQueryCreated);
		assertEquals(solrSearch.getSearchStorage(), searchStorage);
	}

	@Test
	public void testSearchOneParameterNoRecordType() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SearchStorageSpy searchStorageSpy = new SearchStorageSpy();
		QueryResponse queryResponse = new QueryResponseSpy();
		((SolrClientProviderSpy) solrClientProvider).solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(solrClientProvider,
						searchStorageSpy);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = createSearchDataGroup();

		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertNotNull(searchResult.listOfDataGroups);
		DataGroup firstResult = searchResult.listOfDataGroups.get(0);
		assertEquals(firstResult.getNameInData(), "book");

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrQuery solrQueryCreated = (SolrQuery) solrClientSpy.params;
		assertEquals(solrQueryCreated.getQuery(), "titleIndexTerm:A title");

		assertEquals(searchStorageSpy.searchTermIds.get(0), "titleSearchTerm");
	}

	private DataGroup createSearchDataGroup() {
		DataGroup searchData = DataGroup.withNameInData("bookSearch");
		DataGroup include = DataGroup.withNameInData("include");
		searchData.addChild(include);
		DataGroup includePart = DataGroup.withNameInData("includePart");
		include.addChild(includePart);
		includePart.addChild(DataAtomic.withNameInDataAndValue("titleSearchTerm", "A title"));
		return searchData;
	}

	@Test
	public void testReturnThreeRecords() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		SearchStorageSpy searchStorageSpy = new SearchStorageSpy();
		QueryResponseSpy queryResponse = new QueryResponseSpy();
		queryResponse.noOfDocumentsToReturn = 3;
		((SolrClientProviderSpy) solrClientProvider).solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(solrClientProvider,
						searchStorageSpy);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = createSearchDataGroup();

		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertEquals(searchResult.listOfDataGroups.size(), 3);
	}

	@Test(expectedExceptions = SolrSearchException.class)
	public void testSearchErrorException() {
		SolrClientProviderSpy solrClientProvider = new SolrClientProviderSpy();
		SearchStorageSpy searchStorageSpy = new SearchStorageSpy();
		solrClientProvider.returnErrorThrowingClient = true;
		QueryResponseSpy queryResponse = new QueryResponseSpy();
		queryResponse.noOfDocumentsToReturn = 3;
		solrClientProvider.solrClientSpy.queryResponse = queryResponse;

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(solrClientProvider,
						searchStorageSpy);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = createSearchDataGroup();

		solrSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
	}

	@Test
	public void testSearchUndefinedFieldErrorException() {
		SolrClientProviderSpy solrClientProvider = new SolrClientProviderSpy();
		solrClientProvider.returnErrorThrowingClient = true;
		solrClientProvider.errorMessage = "Error from server at http://localhost:8983/solr/coracore: undefined field testNewsTitleSearchTerm";
		SearchStorageSpy searchStorageSpy = new SearchStorageSpy();

		SolrRecordSearch solrSearch = SolrRecordSearch
				.createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(solrClientProvider,
						searchStorageSpy);
		List<String> recordTypes = new ArrayList<>();
		DataGroup searchData = DataGroup.withNameInData("bookSearch");
		DataGroup include = DataGroup.withNameInData("include");
		searchData.addChild(include);
		DataGroup includePart = DataGroup.withNameInData("includePart");
		include.addChild(includePart);
		includePart.addChild(DataAtomic.withNameInDataAndValue("anUnindexedTerm", "A title"));

		SpiderSearchResult searchResult = solrSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes, searchData);
		assertEquals(searchResult.listOfDataGroups.size(), 0);
	}

}
