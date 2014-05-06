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
package io.spring.recommendation.service;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Minella
 */
public abstract class AbstractCachingExistsService<T> implements ExistsService<T>, ItemStream {

	private Map<String, Boolean> cache;

	protected boolean isCached(String id) {
		return cache.containsKey(id);
	}

	public void cache(String id) {
		cache.put(id, true);
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		cache = new HashMap<>();
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
	}

	@Override
	public void close() throws ItemStreamException {
		cache = null;
	}
}
