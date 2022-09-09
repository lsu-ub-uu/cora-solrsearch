/*
 * Copyright 2017, 2019, 2021, 2022 Uppsala University Library
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
import se.uu.ub.cora.data.collectterms.IndexTerm;
import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.data.converter.DataToJsonConverterProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.solr.SolrClientProvider;

public final class SolrRecordIndexer implements RecordIndexer {
	private SolrClientProvider solrClientProvider;
	private String id;
	private String type;
	private List<IndexTerm> indexTerms;
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
	public void indexData(List<String> ids, List<IndexTerm> indexTerms, DataGroup dataRecordGroup) {
		this.ids = new ArrayList<>(ids);
		this.indexTerms = indexTerms;
		possiblyIndexData(dataRecordGroup, true);
	}

	private void possiblyIndexData(DataGroup dataRecordGroup, boolean performExplicitCommit) {
		if (!indexTerms.isEmpty()) {
			indexDataKnownToContainDataToIndex(dataRecordGroup, performExplicitCommit);
		}
	}

	private void indexDataKnownToContainDataToIndex(DataGroup dataRecordGroup,
			boolean performExplicitCommit) {
		document = new SolrInputDocument();
		extractRecordIdentification(dataRecordGroup);
		addIdToDocument();
		addTypeToDocument();
		addIndexTerms();
		String json = convertDataGroupToJsonString(dataRecordGroup);
		document.addField("recordAsJson", json);
		sendDocumentToSolr(performExplicitCommit);
	}

	private void extractRecordIdentification(DataGroup dataRecordGroup) {
		DataGroup recordInfo = dataRecordGroup.getFirstGroupWithNameInData("recordInfo");
		id = recordInfo.getFirstAtomicValueWithNameInData("id");
		type = recordInfo.getFirstAtomicValueWithNameInData("type");
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
		for (IndexTerm collectIndexTerm : indexTerms) {
			addFieldAndValueToSolrDocument(collectIndexTerm);
		}
	}

	private void addFieldAndValueToSolrDocument(IndexTerm indexTerm) {
		document.addField(buildFieldNameUsingIndexTerm(indexTerm), indexTerm.value());
	}

	private String buildFieldNameUsingIndexTerm(IndexTerm indexTerm) {
		String suffix = chooseSuffixFromIndexType(indexTerm.indexType());
		return indexTerm.indexFieldName() + suffix;
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

	private void sendDocumentToSolr(boolean performExplicitCommit) {
		try {
			SolrClient solrClient = solrClientProvider.getSolrClient();
			solrClient.add(document);
			possiblyPerformExplicitCommit(solrClient, performExplicitCommit);
		} catch (Exception e) {
			throw SolrIndexException
					.withMessageAndException("Error while indexing record with type: " + type
							+ " and id: " + id + " " + e.getMessage(), e);
		}
	}

	private void possiblyPerformExplicitCommit(SolrClient solrClient, boolean performExplicitCommit)
			throws SolrServerException, IOException {
		if (performExplicitCommit) {
			solrClient.commit();
		}
	}

	private String convertDataGroupToJsonString(DataGroup dataGroup) {

		DataToJsonConverterFactory converterFactory = DataToJsonConverterProvider
				.createImplementingFactory();
		DataToJsonConverter dataToJsonConverter = converterFactory
				.factorUsingConvertible(dataGroup);
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

	@Override
	public void indexDataWithoutExplicitCommit(List<String> ids, List<IndexTerm> indeTerm,
			DataGroup dataRecord) {
		this.ids = new ArrayList<>(ids);
		this.indexTerms = indeTerm;
		possiblyIndexData(dataRecord, false);

	}
}
