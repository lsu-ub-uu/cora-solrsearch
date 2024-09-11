/*
 * Copyright 2017, 2019, 2021, 2022, 2024 Uppsala University Library
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

import se.uu.ub.cora.data.converter.DataToJsonConverter;
import se.uu.ub.cora.json.builder.JsonObjectBuilder;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataToJsonConverterSpy implements DataToJsonConverter {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataToJsonConverterSpy() {
		MCR.useMRV(MRV);
		// MRV.setDefaultReturnValuesSupplier("toJsonObjectBuilder", JsonObjectBuilder::new);
		MRV.setDefaultReturnValuesSupplier("factorUsingConvertible", DataToJsonConverterSpy::new);
		MRV.setDefaultReturnValuesSupplier("toJson", () -> "Json from DataToJsonConverterSpy");
	}

	@Override
	public JsonObjectBuilder toJsonObjectBuilder() {
		return (JsonObjectBuilder) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String toJsonCompactFormat() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String toJson() {
		return (String) MCR.addCallAndReturnFromMRV();
	}
}
