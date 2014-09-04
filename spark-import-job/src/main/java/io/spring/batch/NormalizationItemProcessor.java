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

import org.springframework.batch.item.ItemProcessor;

import java.util.Map;

/**
 * Normalizes the generated Likelihood Ratio (LLR) to be within a range of 0-1 to support the currently available Mahout
 * Item recommenders.
 *
 * @author mminella
 */
public class NormalizationItemProcessor implements ItemProcessor<Similarities, Similarities> {
	@Override
	public Similarities process(Similarities item) throws Exception {

		for (Map.Entry<Integer, Double> entry : item.getSimilarites().entrySet()) {
			entry.setValue(1.0 - (1.0 / (1.0 + entry.getValue())));
		}

		return item;
	}
}
