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

package se.uu.ub.cora.solrindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.data.converter.DataToJsonConverterProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.solr.SolrClientProvider;

public final class SolrRecordIndexer implements RecordIndexer {
	private static final String INDEX = "index";
	private SolrClientProvider solrClientProvider;
	private String id;
	private String type;
	private se.uu.ub.cora.data.DataGroup collectedData;
	private SolrInputDocument document;
	private List<String> ids;

	private SolrRecordIndexer(SolrClientProvider solrClientProvider) {
		this.solrClientProvider = solrClientProvider;
	}

	public static SolrRecordIndexer createSolrRecordIndexerUsingSolrClientProvider(
			SolrClientProvider solrClientProvider) {
		return new SolrRecordIndexer(solrClientProvider);
	}

	@Override
	public void indexData(List<String> ids, DataGroup collectedData, DataGroup record) {
		this.ids = new ArrayList<>(ids);
		this.collectedData = collectedData;
		if (dataGroupHasIndexTerms(collectedData)) {
			indexDataKnownToContainDataToIndex(record);
		}
	}

	private void indexDataKnownToContainDataToIndex(DataGroup record) {
		document = new SolrInputDocument();
		extractRecordIdentification();
		addIdToDocument();
		addTypeToDocument();
		addIndexTerms();
		String json = convertDataGroupToJsonString(record);
		document.addField("recordAsJson", json);
		sendDocumentToSolr();
	}

	private boolean dataGroupHasIndexTerms(DataGroup collectedData) {
		return collectedData.containsChildWithNameInData(INDEX)
				&& collectedData.getFirstGroupWithNameInData(INDEX)
						.containsChildWithNameInData("collectedDataTerm");
	}

	private void extractRecordIdentification() {
		id = collectedData.getFirstAtomicValueWithNameInData("id");
		type = collectedData.getFirstAtomicValueWithNameInData("type");
	}

	private void addIdToDocument() {
		document.addField("id", type + "_" + id);

		for (String idInIds : ids) {
			document.addField("ids", idInIds);
		}
	}

	private void addTypeToDocument() {
		document.addField("type", type);
	}

	private void addIndexTerms() {
		DataGroup collectedIndexData = collectedData.getFirstGroupWithNameInData(INDEX);
		List<DataGroup> allIndexTermGroups = collectedIndexData
				.getAllGroupsWithNameInData("collectedDataTerm");
		for (DataGroup collectIndexTerm : allIndexTermGroups) {
			addFieldAndValueToSolrDocument(collectIndexTerm);
		}
	}

	private void addFieldAndValueToSolrDocument(DataGroup collectIndexTerm) {
		document.addField(extractFieldName(collectIndexTerm),
				collectIndexTerm.getFirstAtomicValueWithNameInData("collectTermValue"));
	}

	private String extractFieldName(DataGroup collectIndexTerm) {
		DataGroup extraData = collectIndexTerm.getFirstGroupWithNameInData("extraData");
		String indexType = extraData.getFirstAtomicValueWithNameInData("indexType");

		String fieldName = extraData.getFirstAtomicValueWithNameInData("indexFieldName");
		String suffix = chooseSuffixFromIndexType(indexType);
		return fieldName + suffix;
	}

	private String chooseSuffixFromIndexType(String indexType) {
		if ("indexTypeString".equals(indexType) || "indexTypeId".equals(indexType)) {
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

	private void sendDocumentToSolr() {
		try {
			SolrClient solrClient = solrClientProvider.getSolrClient();
			solrClient.add(document);
			solrClient.commit();
		} catch (Exception e) {
			throw SolrIndexException
					.withMessageAndException("Error while indexing record with type: " + type
							+ " and id: " + id + " " + e.getMessage(), e);
		}
	}

	private String convertDataGroupToJsonString(DataGroup dataGroup) {
		DataToJsonConverter dataToJsonConverter = DataToJsonConverterProvider
				.getConverterUsingDataPart(dataGroup);
		return dataToJsonConverter.toJson();
	}

	@Override
	public void deleteFromIndex(String type, String id) {
		try {
			tryToDeleteFromIndex(type, id);
		} catch (Exception e) {
			throw SolrIndexException
					.withMessageAndException("Error while deleting index for record with type: "
							+ type + " and id: " + id + " " + e.getMessage(), e);
		}
	}

	private void tryToDeleteFromIndex(String type, String id)
			throws SolrServerException, IOException {
		SolrClient solrClient = solrClientProvider.getSolrClient();
		solrClient.deleteById(type + "_" + id);
		solrClient.commit();
	}

	public SolrClientProvider getSolrClientProvider() {
		// Needed for test
		return solrClientProvider;
	}
}
