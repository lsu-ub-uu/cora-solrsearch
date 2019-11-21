package se.uu.ub.cora.solrsearch;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.searchstorage.SearchStorage;

public class SearchStorageSpy implements SearchStorage {

	public List<String> searchTermIds = new ArrayList<>();
	public String indexTypeToReturn = "indexTypeString";

	public List<String> collectIndexTermIds = new ArrayList<>();

	@Override
	public DataGroup getSearchTerm(String searchTermId) {
		searchTermIds.add(searchTermId);

		DataGroup searchTerm = createSearchTermWithId(searchTermId);
		if ("titleSearchTerm".equals(searchTermId)) {
			searchTerm.addChild(createIndexTermWithIndexTermId("titleIndexTerm"));
			searchTerm.addChild(createSearchTermType("final"));
		}
		if ("anUnindexedTerm".equals(searchTermId)) {
			searchTerm.addChild(createIndexTermWithIndexTermId("titleIndexTerm"));
			searchTerm.addChild(createSearchTermType("final"));
		}
		if ("linkedTextSearchTerm".equals(searchTermId)) {
			searchTerm.addChild(createSearchTermType("linkedData"));
			searchTerm.addChild(createLinkedOnWithIndexTermId("linkedTextIndexTerm"));
			searchTerm.addChild(createSearchInRecordTypeWithRecordTypeId("coraText"));
			searchTerm.addChild(createIndexTermWithIndexTermId("swedishIndexTerm"));

		}
		return searchTerm;
	}

	private DataElement createLinkedOnWithIndexTermId(String indexTermId) {
		DataGroup indexTerm = new DataGroupSpy("linkedOn");
		indexTerm.addChild(new DataAtomicSpy("linkedRecordType", "collectIndexTerm"));
		indexTerm.addChild(new DataAtomicSpy("linkedRecordId", indexTermId));
		return indexTerm;
	}

	private DataElement createSearchInRecordTypeWithRecordTypeId(String recordTypeId) {
		DataGroup searchInRecordType = new DataGroupSpy("searchInRecordType");
		searchInRecordType.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		searchInRecordType.addChild(new DataAtomicSpy("linkedRecordId", recordTypeId));
		return searchInRecordType;
	}

	private DataAtomic createSearchTermType(String searchTermType) {
		return new DataAtomicSpy("searchTermType", searchTermType);
	}

	private DataGroup createIndexTermWithIndexTermId(String indexTermId) {
		DataGroup indexTerm = new DataGroupSpy("indexTerm");
		indexTerm.addChild(new DataAtomicSpy("linkedRecordType", "collectIndexTerm"));
		indexTerm.addChild(new DataAtomicSpy("linkedRecordId", indexTermId));
		return indexTerm;
	}

	private DataGroup createSearchTermWithId(String searchTermId) {
		DataGroup searchTerm = new DataGroupSpy("searchTerm");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", searchTermId));
		searchTerm.addChild(recordInfo);
		return searchTerm;
	}

	@Override
	public DataGroup getCollectIndexTerm(String collectIndexTermId) {
		collectIndexTermIds.add(collectIndexTermId);

		DataGroup titleIndexTerm = new DataGroupSpy("collectTerm");
		DataGroup extraData = new DataGroupSpy("extraData");
		titleIndexTerm.addChild(extraData);
		if ("titleIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(new DataAtomicSpy("indexFieldName", "title"));
			extraData.addChild(new DataAtomicSpy("indexType", indexTypeToReturn));
		}
		if ("linkedTextIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(new DataAtomicSpy("indexFieldName", "textId"));
			extraData.addChild(new DataAtomicSpy("indexType", "indexTypeString"));
		}
		if ("swedishIndexTerm".equals(collectIndexTermId)) {
			extraData.addChild(new DataAtomicSpy("indexFieldName", "swedish"));
			extraData.addChild(new DataAtomicSpy("indexType", "indexTypeText"));
		}

		return titleIndexTerm;
	}

}
