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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.converter.JsonToDataConverter;
import se.uu.ub.cora.data.converter.JsonToDataConverterProvider;
import se.uu.ub.cora.json.parser.JsonParser;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.searchstorage.SearchStorage;
import se.uu.ub.cora.solr.SolrClientProvider;

public final class SolrRecordSearch implements RecordSearch {

	private static final int DEFAULT_START = 1;
	private static final String START_STRING = "start";
	private static final int DEFAULT_NUMBER_OF_ROWS_TO_RETURN = 100;
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private SolrClientProvider solrClientProvider;
	private SearchStorage searchStorage;
	private SolrQuery solrQuery;
	private SolrClient solrClient;
	private int start;

	private SolrRecordSearch(SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		this.solrClientProvider = solrClientProvider;
		this.searchStorage = searchStorage;
	}

	public static SolrRecordSearch createSolrRecordSearchUsingSolrClientProviderAndSearchStorage(
			SolrClientProvider solrClientProvider, SearchStorage searchStorage) {
		return new SolrRecordSearch(solrClientProvider, searchStorage);
	}

	@Override
	public SearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> recordTypes, DataGroup searchData) {
		try {
			return tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(recordTypes,
					searchData);
		} catch (Exception e) {
			return handleErrors(e);
		}
	}

	private SearchResult handleErrors(Exception e) {
		if (isUndefinedFieldError(e)) {
			return createEmptySearchResult();
		}
		throw SolrSearchException.withMessage("Error searching for records: " + e.getMessage());
	}

	private SearchResult tryToSearchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> recordTypes, DataGroup searchData)
			throws SolrServerException, IOException {
		solrClient = solrClientProvider.getSolrClient();

		solrQuery = new SolrQuery();
		int rows = getNumberOfRowsToRequest(searchData);
		solrQuery.setRows(rows);
		start = getStartRowToRequest(searchData);
		solrQuery.setStart(start - 1);
		addRecordTypesToFilterQuery(recordTypes);
		addSearchTermsToQuery(searchData);
		return searchInSolr();
	}

	private int getNumberOfRowsToRequest(DataGroup searchData) {
		if (searchData.containsChildWithNameInData("rows")) {
			return getRowsAsIntOrDefault(searchData);
		}
		return DEFAULT_NUMBER_OF_ROWS_TO_RETURN;

	}

	private int getRowsAsIntOrDefault(DataGroup searchData) {
		try {
			return Integer.parseInt(searchData.getFirstAtomicValueWithNameInData("rows"));
		} catch (NumberFormatException e) {
			return DEFAULT_NUMBER_OF_ROWS_TO_RETURN;
		}
	}

	private int getStartRowToRequest(DataGroup searchData) {
		if (searchData.containsChildWithNameInData(START_STRING)) {
			return getStartValueAsIntOrDefault(searchData);
		}
		return DEFAULT_START;
	}

	private int getStartValueAsIntOrDefault(DataGroup searchData) {
		try {
			return Integer.parseInt(searchData.getFirstAtomicValueWithNameInData(START_STRING));
		} catch (NumberFormatException e) {
			return DEFAULT_START;
		}
	}

	private void addRecordTypesToFilterQuery(List<String> recordTypes) {
		List<String> recordTypesWithType = addTypeToRecordTypes(recordTypes);
		String filterQuery = String.join(" OR ", recordTypesWithType);
		solrQuery.addFilterQuery(filterQuery);
	}

	private List<String> addTypeToRecordTypes(List<String> recordTypes) {
		List<String> recordTypesWithType = new ArrayList<>(recordTypes.size());
		for (String recordType : recordTypes) {
			recordTypesWithType.add("type:" + recordType);
		}
		return recordTypesWithType;
	}

	private void addSearchTermsToQuery(DataGroup searchData) {
		List<DataChild> childElementsFromSearchData = getChildElementsFromIncludePartOfSearch(
				searchData);

		List<String> queryParts = new ArrayList<>(childElementsFromSearchData.size());

		for (DataChild childElementFromSearch : childElementsFromSearchData) {
			queryParts.add(addSearchDataToQuery((DataAtomic) childElementFromSearch));
		}
		setSolrQuery(queryParts);
	}

	private void setSolrQuery(List<String> queryParts) {
		String query = String.join(" AND ", queryParts);
		solrQuery.set("q", query);
	}

	private String addSearchDataToQuery(DataAtomic childElementFromSearch) {
		DataGroup searchTerm = searchStorage.getSearchTerm(childElementFromSearch.getNameInData());
		String indexFieldName = extractIndexFieldName(searchTerm);
		String query = null;

		if (searchTypeIsLinkedData(searchTerm)) {
			query = createQueryForLinkedData(childElementFromSearch, searchTerm, indexFieldName);
		} else {
			query = createQueryForFinal(childElementFromSearch, indexFieldName);
		}
		return query;
	}

	private String createQueryForLinkedData(DataAtomic childElementFromSearchAsAtomic,
			DataGroup searchTerm, String indexFieldName) {
		String linkedOnIndexFieldName = getLinkedOnIndexFieldNameFromStorageUsingSearchTerm(
				searchTerm);
		String query = "{!join from=ids to=" + linkedOnIndexFieldName + "}" + indexFieldName + ":"
				+ childElementFromSearchAsAtomic.getValue();
		query += " AND type:" + searchTerm.getFirstGroupWithNameInData("searchInRecordType")
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		return query;
	}

	private String createQueryForFinal(DataAtomic childElementFromSearchAsAtomic,
			String indexFieldName) {
		String searchStringWithParenthesis = getEscapedSearchStringFromChildSurroundedWithParenthesis(
				childElementFromSearchAsAtomic);
		return indexFieldName + ":" + searchStringWithParenthesis;
	}

	private String getEscapedSearchStringFromChildSurroundedWithParenthesis(
			DataAtomic childElementFromSearchAsAtomic) {
		String value = childElementFromSearchAsAtomic.getValue();
		String escapedValue = getEscapedValue(value);
		return "(" + escapedValue + ")";
	}

	private String getEscapedValue(String value) {
		return value.replace(":", "\\:");
	}

	private String getLinkedOnIndexFieldNameFromStorageUsingSearchTerm(DataGroup searchTerm) {
		String linkedOn = getLinkedOnFromSearchTermDataGroup(searchTerm);
		DataGroup collectIndexTerm = searchStorage.getCollectIndexTerm(linkedOn);
		return extractFieldName(collectIndexTerm);
	}

	private boolean searchTypeIsLinkedData(DataGroup searchTerm) {
		return "linkedData".equals(searchTerm.getFirstAtomicValueWithNameInData("searchTermType"));
	}

	private String extractIndexFieldName(DataGroup searchTerm) {
		String id = getIndexTermIdFromSearchTermDataGroup(searchTerm);
		DataGroup collectIndexTerm = searchStorage.getCollectIndexTerm(id);
		return extractFieldName(collectIndexTerm);
	}

	private String extractFieldName(DataGroup collectIndexTerm) {
		DataGroup extraData = collectIndexTerm.getFirstGroupWithNameInData("extraData");
		String indexType = extraData.getFirstAtomicValueWithNameInData("indexType");

		String fieldName = extraData.getFirstAtomicValueWithNameInData("indexFieldName");
		String suffix = chooseSuffixFromIndexType(indexType);
		return fieldName + suffix;
	}

	private String chooseSuffixFromIndexType(String indexType) {
		if ("indexTypeString".equals(indexType)) {
			return "_s";
		} else if ("indexTypeBoolean".equals(indexType)) {
			return "_b";
		} else if ("indexTypeDate".equals(indexType)) {
			return "_dt";
		} else if ("indexTypeNumber".equals(indexType)) {
			return "_l";
		} else {
			return "_t";
		}
	}

	private List<DataChild> getChildElementsFromIncludePartOfSearch(DataGroup searchData) {
		DataGroup include = searchData.getFirstGroupWithNameInData("include");
		DataGroup includePart = include.getFirstGroupWithNameInData("includePart");
		return includePart.getChildren();
	}

	private String getIndexTermIdFromSearchTermDataGroup(DataGroup searchTerm) {
		DataGroup indexTerm = searchTerm.getFirstGroupWithNameInData("indexTerm");
		return indexTerm.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private String getLinkedOnFromSearchTermDataGroup(DataGroup searchTerm) {
		DataGroup indexTerm = searchTerm.getFirstGroupWithNameInData("linkedOn");
		return indexTerm.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private SearchResult searchInSolr() throws SolrServerException, IOException {
		SolrDocumentList results = getSolrDocumentsFromSolr();
		return createSearchResultFromSolrResults(results);
	}

	private SolrDocumentList getSolrDocumentsFromSolr() throws SolrServerException, IOException {
		QueryResponse response = solrClient.query(solrQuery);

		return response.getResults();
	}

	private SearchResult createSearchResultFromSolrResults(SolrDocumentList results) {
		SearchResult searchResult = createEmptySearchResult();
		searchResult.start = start;
		searchResult.totalNumberOfMatches = results.getNumFound();
		convertAndAddJsonResultsToSearchResult(searchResult, results);
		return searchResult;
	}

	private void convertAndAddJsonResultsToSearchResult(SearchResult searchResult,
			Iterable<SolrDocument> results) {
		for (SolrDocument solrDocument : results) {
			convertAndAddJsonResultToSearchResult(searchResult, solrDocument);
		}
	}

	private void convertAndAddJsonResultToSearchResult(SearchResult searchResult,
			SolrDocument solrDocument) {
		String recordAsJson = (String) solrDocument.getFirstValue("recordAsJson");
		DataGroup dataGroup = convertJsonStringToDataGroup(recordAsJson);
		searchResult.listOfDataGroups.add(dataGroup);
	}

	private DataGroup convertJsonStringToDataGroup(String jsonRecord) {
		JsonParser jsonParser = new OrgJsonParser();
		JsonValue jsonValue = jsonParser.parseString(jsonRecord);
		JsonToDataConverter jsonToDataConverter = JsonToDataConverterProvider
				.getConverterUsingJsonObject(jsonValue);
		return (DataGroup) jsonToDataConverter.toInstance();
	}

	private boolean isUndefinedFieldError(Exception e) {
		return e.getMessage().contains("undefined field");
	}

	private SearchResult createEmptySearchResult() {
		SearchResult searchResult = new SearchResult();
		searchResult.listOfDataGroups = new ArrayList<>();
		return searchResult;
	}

	public SearchStorage getSearchStorage() {
		return searchStorage;
	}

	public SolrClientProvider getSolrClientProvider() {
		// Needed for test
		return solrClientProvider;
	}

}
