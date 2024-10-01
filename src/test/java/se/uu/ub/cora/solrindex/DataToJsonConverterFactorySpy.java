package se.uu.ub.cora.solrindex;

import se.uu.ub.cora.data.Convertible;
import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.data.converter.DataToJsonConverterFactory;
import se.uu.ub.cora.data.converter.ExternalUrls;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataToJsonConverterFactorySpy implements DataToJsonConverterFactory {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataToJsonConverterFactorySpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("factorUsingConvertible", DataToJsonConverterSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorUsingBaseUrlAndRecordUrlAndConvertible",
				DataToJsonConverterSpy::new);
		MRV.setDefaultReturnValuesSupplier("factorUsingConvertibleAndExternalUrls",
				DataToJsonConverterSpy::new);
	}

	@Override
	public DataToJsonConverter factorUsingConvertible(Convertible convertible) {
		return (DataToJsonConverter) MCR.addCallAndReturnFromMRV("convertible", convertible);
	}

	@Override
	public DataToJsonConverter factorUsingBaseUrlAndRecordUrlAndConvertible(String baseUrl,
			String recordUrl, Convertible convertible) {
		return (DataToJsonConverter) MCR.addCallAndReturnFromMRV("baseUrl", baseUrl, "recordUrl",
				recordUrl, "convertible", convertible);
	}

	@Override
	public DataToJsonConverter factorUsingConvertibleAndExternalUrls(Convertible convertible,
			ExternalUrls externalUrls) {
		return (DataToJsonConverter) MCR.addCallAndReturnFromMRV("convertible", convertible,
				"externalUrls", externalUrls);
	}

}
