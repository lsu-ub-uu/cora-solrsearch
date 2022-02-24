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

import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordIndexerFactory;
import se.uu.ub.cora.solr.SolrClientProviderImp;

public class SolrRecordIndexerFactory implements RecordIndexerFactory {

	private Map<String, SolrClientProviderImp> solrClientProviders = new HashMap<>();

	@Override
	public RecordIndexer factor(String solrUrl) {
		SolrClientProviderImp solrClientProvider = solrClientProviders.computeIfAbsent(solrUrl,
				SolrClientProviderImp::usingBaseUrl);
		return SolrRecordIndexer.createSolrRecordIndexerUsingSolrClientProvider(solrClientProvider);
	}
}
