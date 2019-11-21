module se.uu.ub.cora.solrsearch {
	requires transitive solr.solrj;
	requires transitive se.uu.ub.cora.search;
	requires transitive se.uu.ub.cora.searchstorage;
	requires transitive se.uu.ub.cora.data;

	exports se.uu.ub.cora.solr;
	exports se.uu.ub.cora.solrindex;
	exports se.uu.ub.cora.solrsearch;
}