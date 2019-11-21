package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.DataPart;
import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.json.builder.JsonBuilderFactory;

public class DataToJsonConverterFactorySpy implements DataToJsonConverterFactory {

	public JsonBuilderFactory factory;
	public DataPart dataPart;

	@Override
	public DataToJsonConverter createForDataElement(JsonBuilderFactory factory, DataPart dataPart) {
		this.factory = factory;
		this.dataPart = dataPart;

		return new DataToJsonConverterSpy();
	}

}
