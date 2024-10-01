/*
 * Copyright 2017, 2021, 2022, 2024 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.data.converter.DataToJsonConverterProvider;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.solr.SolrClientProvider;

public class SolrRecordIndexerTest {
	private static final String RECORD_TYPE = "someRecordType";
	private static final String RECORD_ID = "someRecordId";
	// private List<String> ids = new ArrayList<>();
	private SolrClientProvider solrClientProvider;
	private SolrRecordIndexer recordIndexer;
	private DataToJsonConverterFactoryCreatorSpy dataToJsonConverterFactoryCreator;
	private DataToJsonConverterSpy dataToJsonConverterSpy;
	private DataToJsonConverterFactorySpy dataToJsonConverterFactory;
	private DataRecordGroup dataRecordGroup;

	@BeforeMethod
	public void beforeTest() {
		setUpConverters();
		solrClientProvider = new SolrClientProviderSpy();
		recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		dataRecordGroup = createDefaultDataGroup();
	}

	private void setUpConverters() {
		dataToJsonConverterFactory = new DataToJsonConverterFactorySpy();
		dataToJsonConverterSpy = new DataToJsonConverterSpy();
		dataToJsonConverterFactoryCreator = new DataToJsonConverterFactoryCreatorSpy();
		dataToJsonConverterFactoryCreator.MRV.setDefaultReturnValuesSupplier("createFactory",
				() -> dataToJsonConverterFactory);
		dataToJsonConverterFactory.MRV.setDefaultReturnValuesSupplier("factorUsingConvertible",
				() -> dataToJsonConverterSpy);
		DataToJsonConverterProvider
				.setDataToJsonConverterFactoryCreator(dataToJsonConverterFactoryCreator);
	}

	@Test
	public void testGetSolrClientProvider() {
		assertEquals(recordIndexer.onlyForTestGetSolrClientProvider(), solrClientProvider);
	}

	@Test
	public void testCollectNoIndexDataGroupNoCollectedDataTerm() {
		List<IndexTerm> indexTerms = createEmptyIndexTermList();

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	private List<IndexTerm> createEmptyIndexTermList() {
		return Collections.emptyList();
	}

	@Test
	public void testCollectIndexDataGroupNoCollectedDataTerm() {
		List<IndexTerm> indexTerms = createEmptyIndexTermList();
		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, new DataRecordGroupSpy());

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTerm() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);

		dataToJsonConverterFactory.MCR.assertCalledParameters("factorUsingConvertible",
				dataRecordGroup);

		assertCorrectDocumentWhenOneSearchTerm();
	}

	private void assertCorrectDocumentWhenOneSearchTerm() {
		SolrInputDocument created = getCreatedDocument();
		assertEquals(created.getField("recordAsJson").getValue().toString(),
				"Json from DataToJsonConverterSpy");

		assertEquals(created.getField("id").getValue().toString(), "someRecordType_someRecordId");
		assertEquals(created.getField("type").getValue().toString(), "someRecordType");
		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	private SolrInputDocument getCreatedDocument() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrInputDocument created = solrClientSpy.document;
		return created;
	}

	private DataRecordGroup createDefaultDataGroup() {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getType", () -> "someType");
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "someId");
		return dataRecordGroup;
	}

	private List<IndexTerm> createCollectedDataWithOneCollectedIndexDataTerm() {
		List<IndexTerm> indexTerms = new ArrayList<>();
		IndexTerm indexTerm = new IndexTerm("someIndexTerm", "someEnteredValue", "title",
				"indexTypeString");
		indexTerms.add(indexTerm);
		return indexTerms;
	}

	@Test
	public void testIndexDataCommittedToSolr() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		assertEquals(solrClientSpy.committed, false);

		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);

		assertEquals(solrClientSpy.committed, true);
	}

	@Test
	public void testTwoCollectedIndexDataTerms() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		IndexTerm indexTerm = new IndexTerm("someOtherIndexTerm", "someOtherEnteredValue",
				"subTitle", "indexTypeText");
		indexTerms.add(indexTerm);

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someRecordType_someRecordId");
		assertEquals(created.getField("type").getValue().toString(), "someRecordType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
		assertEquals(created.getField("subTitle_t").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testTwoCollectedDataTermsUsingSameCollectIndexTermWithDifferentValues() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		IndexTerm indexTerm = new IndexTerm("someIndexTerm", "someOtherEnteredValue", "title",
				"indexTypeString");
		indexTerms.add(indexTerm);

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someRecordType_someRecordId");
		assertEquals(created.getField("type").getValue().toString(), "someRecordType");

		Iterator<Object> iterator = created.getField("title_s").getValues().iterator();
		assertEquals(iterator.next().toString(), "someEnteredValue");
		assertEquals(iterator.next().toString(), "someOtherEnteredValue");
	}

	@Test(expectedExceptions = SolrIndexException.class)
	public void testExceptionFromSolrClient() {
		setUpIndexCallThatThrowsError();
	}

	private void setUpIndexCallThatThrowsError() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);
	}

	@Test
	public void testExceptionFromSolrClientContainsOriginalException() {
		try {
			setUpIndexCallThatThrowsError();
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof SolrExceptionSpy);
		}
	}

	@Test
	public void testDeleteFromIndex() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		recordIndexer.deleteFromIndex("someType", "someId");

		assertEquals(solrClientSpy.deletedId, "someType_someId");
		assertEquals(solrClientSpy.committed, true);
	}

	@Test(expectedExceptions = SolrIndexException.class, expectedExceptionsMessageRegExp = ""
			+ "Error while deleting index for record with type: someType and id: someId"
			+ " something went wrong")
	public void testDeleteFromIndexExceptionFromSolrClient() {
		setUpDeleteToThrowError();
	}

	private void setUpDeleteToThrowError() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		recordIndexer.deleteFromIndex("someType", "someId");
	}

	@Test
	public void testDeleteFromIndexExceptionFromSolrClientContainsOriginalException() {
		try {
			setUpDeleteToThrowError();
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof SolrExceptionSpy);
		}
	}

	@Test
	public void testBooleanIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeBoolean");

		assertEquals(created.getField("subTitle_b").getValue().toString(), "someOtherEnteredValue");
	}

	private SolrInputDocument createTestDataForIndexType(String indexType) {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		IndexTerm indexTerm = new IndexTerm("someOtherIndexTerm", "someOtherEnteredValue",
				"subTitle", indexType);
		indexTerms.add(indexTerm);

		recordIndexer.indexData(RECORD_TYPE, RECORD_ID, indexTerms, dataRecordGroup);
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrInputDocument created = solrClientSpy.document;
		return created;
	}

	@Test
	public void testDateIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeDate");
		assertEquals(created.getField("subTitle_dt").getValue().toString(),
				"someOtherEnteredValue");
	}

	@Test
	public void testNumberIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeNumber");
		assertEquals(created.getField("subTitle_l").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testIdIndexType() {
		SolrInputDocument created = createTestDataForIndexType("indexTypeId");
		assertEquals(created.getField("subTitle_s").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testCollectNoIndexDataGroupNoCollectedDataTermWhenNoExplicitCommit() {
		List<IndexTerm> indexTerms = createEmptyIndexTermList();

		recordIndexer.indexDataWithoutExplicitCommit(RECORD_TYPE, RECORD_ID, indexTerms,
				dataRecordGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTermWhenNoExplicitCommit() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();

		recordIndexer.indexDataWithoutExplicitCommit(RECORD_TYPE, RECORD_ID, indexTerms,
				dataRecordGroup);

		dataToJsonConverterFactory.MCR.assertCalledParameters("factorUsingConvertible",
				dataRecordGroup);
		assertCorrectDocumentWhenOneSearchTerm();
	}

	@Test
	public void testIndexDataNOTCommittedToSolrWhenNoExplicitCommit() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		assertEquals(solrClientSpy.committed, false);
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();

		recordIndexer.indexDataWithoutExplicitCommit(RECORD_TYPE, RECORD_ID, indexTerms,
				dataRecordGroup);

		assertEquals(solrClientSpy.committed, false);
	}
}
