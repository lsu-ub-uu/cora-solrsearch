/*
 * Copyright 2017, 2019, 2021 Uppsala University Library
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
package se.uu.ub.cora.solr;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;

import org.apache.solr.client.solrj.SolrClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SolrClientProviderTest {

	private SolrClientProviderImp solrClientProvider;
	private String baseUrl = "http://localhost:8983/solr/stuff";

	@BeforeMethod
	public void setUp() {
		solrClientProvider = SolrClientProviderImp.usingBaseUrl(baseUrl);
	}

	@Test
	public void testInit() {
		assertNotNull(solrClientProvider);
	}

	@Test
	public void testGetBaseURL() throws Exception {
		assertEquals(solrClientProvider.getBaseURL(), baseUrl);
	}

	@Test
	public void testGetSolrClient() {
		SolrClient solrClient = solrClientProvider.getSolrClient();
		assertNotNull(solrClient);
	}

	@Test
	public void testGetSolrClientReturnsSameInstance() {
		SolrClient solrClient = solrClientProvider.getSolrClient();
		assertNotNull(solrClient);
		SolrClient solrClient2 = solrClientProvider.getSolrClient();

		assertSame(solrClient, solrClient2);
	}

	@Test
	public void testSolrClientBaseUrlSetCorrectly() {
		SolrClient solrClient = solrClientProvider.getSolrClient();
		String urlSetInClient = null;
		try {
			Field f;
			f = solrClient.getClass().getDeclaredField("baseUrl");
			f.setAccessible(true);
			urlSetInClient = (String) f.get(solrClient);
		} catch (Exception e) {
			// if exception fail test
			assertTrue(false);
		}
		assertEquals(urlSetInClient, baseUrl);
	}
}
