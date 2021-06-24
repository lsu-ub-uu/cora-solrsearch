package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactoryCreator;

public class DataToJsonConverterFactoryCreatorSpy implements DataToJsonConverterFactoryCreator {

	DataToJsonConverterFactorySpy dataToJsonConverterFactory;

	@Override
	public DataToJsonConverterFactory createFactory() {
		dataToJsonConverterFactory = new DataToJsonConverterFactorySpy();
		// TODO Auto-generated method stub
		return dataToJsonConverterFactory;
	}

}
