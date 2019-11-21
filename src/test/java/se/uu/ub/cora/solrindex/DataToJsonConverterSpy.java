package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.json.builder.JsonObjectBuilder;

public class DataToJsonConverterSpy implements DataToJsonConverter {

	@Override
	public JsonObjectBuilder toJsonObjectBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJsonCompactFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJson() {
		return "Json from DataToJsonConverterSpy";
	}

}
