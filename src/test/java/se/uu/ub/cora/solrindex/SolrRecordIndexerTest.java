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
package se.uu.ub.cora.solrindex;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.converter.DataToJsonConverterProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.solrsearch.DataAtomicSpy;
import se.uu.ub.cora.solrsearch.DataGroupSpy;

public class SolrRecordIndexerTest {
	private List<String> ids = new ArrayList<>();
	private SolrClientProvider solrClientProvider;
	private SolrRecordIndexer recordIndexer;
	private DataToJsonConverterFactorySpy dataToJsonConverterFactory;

	@BeforeTest
	public void beforeTest() {
		dataToJsonConverterFactory = new DataToJsonConverterFactorySpy();
		DataToJsonConverterProvider.setDataToJsonConverterFactory(dataToJsonConverterFactory);
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
		DataGroup collectedData = new DataGroupSpy("collectedData");
		collectedData.addChild(new DataAtomicSpy("type", "someType"));
		collectedData.addChild(new DataAtomicSpy("id", "someId"));

		recordIndexer.indexData(Collections.emptyList(), collectedData,
				new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectIndexDataGroupNoCollectedDataTerm() {
		DataGroup collectedData = new DataGroupSpy("collectedData");
		collectedData.addChild(new DataAtomicSpy("type", "someType"));
		collectedData.addChild(new DataAtomicSpy("id", "someId"));

		DataGroup index = new DataGroupSpy("index");
		collectedData.addChild(index);
		recordIndexer.indexData(Collections.emptyList(), collectedData,
				new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTerm() {
		DataGroup recordIndexData = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = new DataGroupSpy("someDataGroup");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", "someId"));
		recordIndexer.indexData(ids, recordIndexData, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(dataToJsonConverterFactory.dataPart, dataGroup);

		assertEquals(created.getField("recordAsJson").getValue().toString(),
				"Json from DataToJsonConverterSpy");

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	@Test
	public void testCollectOneSearchTermTwoIds() {
		DataGroup recordIndexData = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = new DataGroupSpy("someDataGroup");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", "someId"));
		List<String> ids2 = new ArrayList<>();
		ids2.add("someType_someId");
		ids2.add("someAbstractType_someId");
		recordIndexer.indexData(ids2, recordIndexData, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("ids").getValue().toString(),
				"[someType_someId, someAbstractType_someId]");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
	}

	private DataGroup createCollectedDataWithOneCollectedIndexDataTerm() {
		DataGroup collectedData = new DataGroupSpy("collectedData");
		collectedData.addChild(new DataAtomicSpy("id", "someId"));
		collectedData.addChild(new DataAtomicSpy("type", "someType"));

		DataGroup indexData = new DataGroupSpy("index");
		collectedData.addChild(indexData);

		DataGroup indexTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someIndexTerm", "someEnteredValue", "0");
		indexData.addChild(indexTerm);
		DataGroup extraData = createIndexExtraData("title", "indexTypeString");
		indexTerm.addChild(extraData);

		return collectedData;
	}

	private DataGroup createIndexExtraData(String indexFieldName, String indexType) {
		DataGroup extraData = new DataGroupSpy("extraData");
		extraData.addChild(new DataAtomicSpy("indexFieldName", indexFieldName));
		extraData.addChild(new DataAtomicSpy("indexType", indexType));
		return extraData;
	}

	private DataGroup createCollectedIndexDataTermUsingIdAndValueAndRepeatId(String id,
			String value, String repeatId) {
		DataGroup collectTerm = new DataGroupSpy("collectedDataTerm");
		collectTerm.addChild(new DataAtomicSpy("collectTermId", id));
		collectTerm.addChild(new DataAtomicSpy("collectTermValue", value));
		collectTerm.setRepeatId(repeatId);

		return collectTerm;
	}

	@Test
	public void testIndexDataCommittedToSolr() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;
		assertEquals(solrClientSpy.committed, false);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		recordIndexer.indexData(ids, collectedData, new DataGroupSpy("someDataGroup"));

		assertEquals(solrClientSpy.committed, true);
	}

	@Test
	public void testTwoCollectedIndexDataTerms() {
		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("subTitle", "indexTypeText");
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, new DataGroupSpy("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("title_s").getValue().toString(), "someEnteredValue");
		assertEquals(created.getField("subTitle_t").getValue().toString(), "someOtherEnteredValue");
	}

	@Test
	public void testTwoCollectedDataTermsUsingSameCollectIndexTermWithDifferentValues() {
		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("title", "indexTypeString");
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, new DataGroupSpy("someDataGroup"));

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

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.addChild(collectedIndexDataTerm);

		recordIndexer.indexData(ids, collectedData, new DataGroupSpy("someDataGroup"));
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

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingIdAndValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);
		DataGroup extraData = createIndexExtraData("subTitle", indexType);
		collectedIndexDataTerm.addChild(extraData);

		recordIndexer.indexData(ids, collectedData, new DataGroupSpy("someDataGroup"));
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
}
