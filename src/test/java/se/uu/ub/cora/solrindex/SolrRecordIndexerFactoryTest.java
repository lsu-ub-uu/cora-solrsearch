/*
 * Copyright 2021, 2022 Uppsala University Library
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
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.search.RecordIndexerFactory;
import se.uu.ub.cora.solr.SolrClientProviderImp;

public class SolrRecordIndexerFactoryTest {
	private RecordIndexerFactory solrIndexerFactory;
	private String defaultSolrUrl = "someSolrUrl";

	@BeforeMethod
	public void setup() {
		solrIndexerFactory = new SolrRecordIndexerFactory();
	}

	@Test
	public void testFactor() {
		SolrRecordIndexer recordIndexer = (SolrRecordIndexer) solrIndexerFactory
				.factor(defaultSolrUrl);
		SolrClientProviderImp solrClientProvider = (SolrClientProviderImp) recordIndexer
				.getSolrClientProvider();

		assertEquals(solrClientProvider.getBaseURL(), defaultSolrUrl);
	}

	@Test
	public void testFactorSameUrlShouldUseSameInstanceOfSolrClientProvider() throws Exception {
		SolrRecordIndexer recordIndexer = (SolrRecordIndexer) solrIndexerFactory
				.factor(defaultSolrUrl);
		SolrClientProviderImp solrClientProvider = (SolrClientProviderImp) recordIndexer
				.getSolrClientProvider();
		SolrRecordIndexer recordIndexer2 = (SolrRecordIndexer) solrIndexerFactory
				.factor(defaultSolrUrl);
		SolrClientProviderImp solrClientProvider2 = (SolrClientProviderImp) recordIndexer2
				.getSolrClientProvider();

		assertSame(solrClientProvider, solrClientProvider2);
	}

	@Test
	public void testFactorDifferentUrlShouldUseSameInstanceOfSolrClientProvider() throws Exception {
		SolrRecordIndexer recordIndexer = (SolrRecordIndexer) solrIndexerFactory
				.factor(defaultSolrUrl);
		SolrClientProviderImp solrClientProvider = (SolrClientProviderImp) recordIndexer
				.getSolrClientProvider();
		SolrRecordIndexer recordIndexer2 = (SolrRecordIndexer) solrIndexerFactory
				.factor("someOtherSolrUrl");
		SolrClientProviderImp solrClientProvider2 = (SolrClientProviderImp) recordIndexer2
				.getSolrClientProvider();

		assertNotSame(solrClientProvider, solrClientProvider2);
	}

}
