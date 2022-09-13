/*
 * Copyright 2017, 2021, 2022 Uppsala University Library
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
import java.util.function.Supplier;

import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.data.converter.DataToJsonConverterProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.solrsearch.DataAtomicSpy;
import se.uu.ub.cora.solrsearch.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;

public class SolrRecordIndexerTest {
	private List<String> ids = new ArrayList<>();
	private SolrClientProvider solrClientProvider;
	private SolrRecordIndexer recordIndexer;
	private DataToJsonConverterFactoryCreatorSpy dataToJsonConverterFactoryCreator;

	@BeforeMethod
	public void beforeTest() {
		dataToJsonConverterFactoryCreator = new DataToJsonConverterFactoryCreatorSpy();
		DataToJsonConverterProvider
				.setDataToJsonConverterFactoryCreator(dataToJsonConverterFactoryCreator);
		ids = new ArrayList<>();
		ids.add("someType_someId");
		solrClientProvider = new SolrClientProviderSpy();
		recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);
	}

	@Test
	public void testGetSolrClientProvider() {
		assertEquals(recordIndexer.getSolrClientProvider(), solrClientProvider);
	}

	@Test
	public void testCollectNoIndexDataGroupNoCollectedDataTerm() {
		List<IndexTerm> indexTerms = createEmptyIndexTermList();

		recordIndexer.indexData(Collections.emptyList(), indexTerms,
				new DataGroupSpy("someDataGroup"));

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
		recordIndexer.indexData(Collections.emptyList(), indexTerms,
				new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTerm() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup dataGroup = createDefaultDataGroup();

		recordIndexer.indexData(ids, indexTerms, dataGroup);

		assertEquals(dataToJsonConverterFactoryCreator.dataToJsonConverterFactory.dataGroup,
				dataGroup);

		assertCorrectDocumentWhenOneSearchTerm();
	}

	private void assertCorrectDocumentWhenOneSearchTerm() {
		SolrInputDocument created = getCreatedDocument();
		assertEquals(created.getField("recordAsJson").getValue().toString(),
				"Json from DataToJsonConverterSpy");

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");
		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	private SolrInputDocument getCreatedDocument() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		SolrInputDocument created = solrClientSpy.document;
		return created;
	}

	private DataGroup createDefaultDataGroup() {
		DataGroup dataGroup = new DataGroupSpy("someDataGroup");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", "someId"));
		DataRecordLinkSpy typeLink = new DataRecordLinkSpy();
		recordInfo.addChild(typeLink);
		typeLink.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> "type");
		typeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> "someType");

		// recordInfo.addChild(new DataAtomicSpy("type", "someType"));
		return dataGroup;
	}

	@Test
	public void testCollectOneSearchTermTwoIds() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = createDefaultDataGroup();
		List<String> ids2 = new ArrayList<>();
		ids2.add("someType_someId");
		ids2.add("someAbstractType_someId");
		recordIndexer.indexData(ids2, indexTerms, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(),
				"[someType_someId, someAbstractType_someId]");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
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
		recordIndexer.indexData(ids, indexTerms, createDefaultDataGroup());

		assertEquals(solrClientSpy.committed, true);
	}

	@Test
	public void testTwoCollectedIndexDataTerms() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		IndexTerm indexTerm = new IndexTerm("someOtherIndexTerm", "someOtherEnteredValue",
				"subTitle", "indexTypeText");
		indexTerms.add(indexTerm);

		recordIndexer.indexData(ids, indexTerms, createDefaultDataGroup());

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
		assertEquals(created.getField("subTitle_t").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testTwoCollectedDataTermsUsingSameCollectIndexTermWithDifferentValues() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		IndexTerm indexTerm = new IndexTerm("someIndexTerm", "someOtherEnteredValue", "title",
				"indexTypeString");
		indexTerms.add(indexTerm);

		recordIndexer.indexData(ids, indexTerms, createDefaultDataGroup());

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

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

		recordIndexer.indexData(ids, indexTerms, createDefaultDataGroup());
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

		recordIndexer.indexData(ids, indexTerms, createDefaultDataGroup());
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

		recordIndexer.indexDataWithoutExplicitCommit(Collections.emptyList(), indexTerms,
				new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectIndexDataGroupNoCollectedDataTermWhenNoExplicitCommit() {
		List<IndexTerm> indexTerms = createEmptyIndexTermList();

		recordIndexer.indexDataWithoutExplicitCommit(Collections.emptyList(), indexTerms,
				new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTermWhenNoExplicitCommit() {
		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup dataGroup = createDefaultDataGroup();

		recordIndexer.indexDataWithoutExplicitCommit(ids, indexTerms, dataGroup);

		assertEquals(dataToJsonConverterFactoryCreator.dataToJsonConverterFactory.dataGroup,
				dataGroup);
		assertCorrectDocumentWhenOneSearchTerm();
	}

	@Test
	public void testIndexDataNOTCommittedToSolrWhenNoExplicitCommit() {
		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		assertEquals(solrClientSpy.committed, false);

		List<IndexTerm> indexTerms = createCollectedDataWithOneCollectedIndexDataTerm();
		recordIndexer.indexDataWithoutExplicitCommit(ids, indexTerms, createDefaultDataGroup());

		assertEquals(solrClientSpy.committed, false);
	}

}
