package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.Convertible;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.json.builder.JsonBuilderFactory;

public class DataToJsonConverterFactorySpy implements DataToJsonConverterFactory {

	public JsonBuilderFactory factory;
	public DataGroup dataGroup;

	@Override
	public DataToJsonConverter factorUsingConvertible(Convertible convertible) {
		this.dataGroup = (DataGroup) convertible;

		return new DataToJsonConverterSpy();
	}

	@Override
	public DataToJsonConverter factorUsingBaseUrlAndConvertible(String baseUrl,
			Convertible convertible) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataToJsonConverter factorUsingBaseUrlAndRecordUrlAndConvertible(String baseUrl,
			String recordUrl, Convertible convertible) {
		// TODO Auto-generated method stub
		return null;
	}

}
