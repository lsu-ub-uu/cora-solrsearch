package se.uu.ub.cora.solrsearch;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.searchstorage.SearchStorage;

public class SearchStorageSpy implements SearchStorage {

	public List<String> searchTermIds = new ArrayList<>();

	@Override
	public DataGroup getSearchTerm(String searchTermId) {
		searchTermIds.add(searchTermId);
		DataGroup searchTerm = DataGroup.withNameInData("searchTerm");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", searchTermId));
		searchTerm.addChild(recordInfo);

		DataGroup indexTerm = DataGroup.withNameInData("indexTerm");
		indexTerm.addChild(
				DataAtomic.withNameInDataAndValue("linkedRecordType", "collectIndexTerm"));
		indexTerm.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "titleIndexTerm"));
		searchTerm.addChild(indexTerm);

		return searchTerm;
	}

}