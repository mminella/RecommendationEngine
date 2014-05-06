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
package io.spring.recommendation.batch.processor;

import io.spring.recommendation.service.ExistsService;
import org.springframework.batch.item.ItemProcessor;

/**
 * @author Michael Minella
 */
public class ExistsItemProcessor<T> implements ItemProcessor<T, T>{

	private ExistsService service;

	public ExistsItemProcessor(ExistsService service) {
		this.service = service;
	}

	@Override
	public T process(T item) throws Exception {

		if(service.exists(item)) {
			return item;
		} else {
			return null;
		}
	}
}
