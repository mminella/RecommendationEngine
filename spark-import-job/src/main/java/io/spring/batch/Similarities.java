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

import java.util.Map;

/**
 * @author mminella
 */
public class Similarities {
	private int tagId;
	private Map<Integer, Double> similarites;

	public Similarities(int tagId, Map<Integer, Double> similarites) {
		this.tagId = tagId;
		this.similarites = similarites;
	}

	public int getTagId() {
		return tagId;
	}

	public Map<Integer, Double> getSimilarites() {
		return similarites;
	}
}
