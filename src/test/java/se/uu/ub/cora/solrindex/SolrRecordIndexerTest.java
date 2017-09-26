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

import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.solr.SolrClientProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class SolrRecordIndexerTest {
	@BeforeMethod
	public void setUp() {
	}

	@Test
	public void testCollectNoIndexDataSearchTerm() {

		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup recordIndexData = DataGroup.withNameInData("recordIndexData");
		recordIndexData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));
		recordIndexData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));

		recordIndexer.indexData(recordIndexData, DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertNull(created);
		assertEquals(solrClientSpy.committed, false);
	}

	@Test
	public void testCollectOneSearchTerm() {

		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup recordIndexData = createCollectedDataWithOneCollectedIndexDataTerm();

		DataGroup dataGroup = DataGroup.withNameInData("someDataGroup");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		recordIndexer.indexData(recordIndexData, dataGroup);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;
		String expectedJson = "{\n" + "    \"children\": [{\n" + "        \"children\": [{\n"
				+ "            \"name\": \"id\",\n" + "            \"value\": \"someId\"\n"
				+ "        }],\n" + "        \"name\": \"recordInfo\"\n" + "    }],\n"
				+ "    \"name\": \"someDataGroup\"\n" + "}" + "";

		assertEquals(created.getField("recordAsJson").getValue().toString(), expectedJson);

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("someIndexTerm").getValue().toString(), "someEnteredValue");
	}

	private DataGroup createCollectedDataWithOneCollectedIndexDataTerm() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		collectedData.addChild(DataAtomic.withNameInDataAndValue("id", "someId"));
		collectedData.addChild(DataAtomic.withNameInDataAndValue("type", "someType"));

		DataGroup indexData = DataGroup.withNameInData("index");
		collectedData.addChild(indexData);

		DataGroup indexTerm = createCollectedIndexDataTermUsingNameValueAndRepeatId("someIndexTerm",
				"someEnteredValue", "0");

		DataGroup extraData = DataGroup.withNameInData("extraData");
		extraData.addChild(DataAtomic.withNameInDataAndValue("indexFieldName", "name"));
		extraData.addChild(DataAtomic.withNameInDataAndValue("indexType", "indexTypeString"));
		indexTerm.addChild(extraData);
		indexData.addChild(indexTerm);
		return collectedData;
	}

	private DataGroup createCollectedIndexDataTermUsingNameValueAndRepeatId(String name,
			String value, String repeatId) {
		DataGroup collectTerm = DataGroup.withNameInData("collectedDataTerm");
		collectTerm.addChild(DataAtomic.withNameInDataAndValue("collectTermId", name));
		collectTerm.addChild(DataAtomic.withNameInDataAndValue("collectTermValue", value));
		collectTerm.setRepeatId(repeatId);

		List<DataAtomic> indexTypes = collectTerm.getAllDataAtomicsWithNameInData("indexType");
		indexTypes.add(
				DataAtomic.withNameInDataAndValueAndRepeatId("indexType", "indexTypeString", "0"));
		indexTypes.add(
				DataAtomic.withNameInDataAndValueAndRepeatId("indexType", "indexTypeBoolean", "1"));
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
		recordIndexer.indexData(collectedData, DataGroup.withNameInData("someDataGroup"));

		assertEquals(solrClientSpy.committed, true);
	}

	@Test
	public void testCollectTwoSearchTerm() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingNameValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);

		recordIndexer.indexData(collectedData, DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		assertEquals(created.getField("someIndexTerm").getValue().toString(), "someEnteredValue");
		assertEquals(created.getField("someOtherIndexTerm").getValue().toString(),
				"someOtherEnteredValue");
	}

	@Test
	public void testCollectTwoSearchTermWithSameName() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingNameValueAndRepeatId(
				"someIndexTerm", "someOtherEnteredValue", "1");
		collectedData.getFirstGroupWithNameInData("index").addChild(collectedIndexDataTerm);

		recordIndexer.indexData(collectedData, DataGroup.withNameInData("someDataGroup"));

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		SolrInputDocument created = solrClientSpy.document;

		assertEquals(created.getField("id").getValue().toString(), "someType_someId");
		assertEquals(created.getField("type").getValue().toString(), "someType");

		Iterator<Object> iterator = created.getField("someIndexTerm").getValues().iterator();
		assertEquals(iterator.next().toString(), "someEnteredValue");
		assertEquals(iterator.next().toString(), "someOtherEnteredValue");
	}

	@Test(expectedExceptions = SolrIndexException.class)
	public void testExceptionFromSolrClient() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		DataGroup collectedData = createCollectedDataWithOneCollectedIndexDataTerm();
		DataGroup collectedIndexDataTerm = createCollectedIndexDataTermUsingNameValueAndRepeatId(
				"someOtherIndexTerm", "someOtherEnteredValue", "1");
		collectedData.addChild(collectedIndexDataTerm);

		recordIndexer.indexData(collectedData, DataGroup.withNameInData("someDataGroup"));
	}

	@Test
	public void testDeleteFromIndex() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		SolrClientSpy solrClientSpy = ((SolrClientProviderSpy) solrClientProvider).solrClientSpy;

		recordIndexer.deleteFromIndex("someType", "someId");
		assertEquals(solrClientSpy.deletedId, "someType_someId");

		assertEquals(solrClientSpy.committed, true);
	}

	@Test(expectedExceptions = SolrIndexException.class)
	public void testDeleteFromIndexExceptionFromSolrClient() {
		SolrClientProvider solrClientProvider = new SolrClientProviderSpy();
		((SolrClientProviderSpy) solrClientProvider).returnErrorThrowingClient = true;
		RecordIndexer recordIndexer = SolrRecordIndexer
				.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);

		recordIndexer.deleteFromIndex("someType", "someId");
	}
}
