package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactoryCreator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataToJsonConverterFactoryCreatorSpy implements DataToJsonConverterFactoryCreator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataToJsonConverterFactoryCreatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("createFactory", DataToJsonConverterFactorySpy::new);
	}

	DataToJsonConverterFactorySpy dataToJsonConverterFactory;

	@Override
	public DataToJsonConverterFactory createFactory() {
		return (DataToJsonConverterFactory) MCR.addCallAndReturnFromMRV();
	}
}
