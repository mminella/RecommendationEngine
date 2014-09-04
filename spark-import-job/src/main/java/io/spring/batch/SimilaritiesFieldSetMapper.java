/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.batch;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mminella
 */
public class SimilaritiesFieldSetMapper implements FieldSetMapper {
	@Override
	public Object mapFieldSet(FieldSet fieldSet) throws BindException {
		int tagId = fieldSet.readInt(0);

		String [] values = fieldSet.getValues();
		Map<Integer, Double> similarites = new HashMap<Integer, Double>();

		for(int i = 1; i < values.length; i++) {
			String [] curValues = values[i].split(":");

			similarites.put(Integer.valueOf(curValues[0]), Double.valueOf(curValues[1]));
		}

		return new Similarities(tagId, similarites);
	}
}
