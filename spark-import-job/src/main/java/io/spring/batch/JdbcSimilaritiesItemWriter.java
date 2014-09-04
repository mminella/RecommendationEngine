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

import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mminella
 */
public class JdbcSimilaritiesItemWriter implements ItemWriter<Similarities> {

	private JdbcOperations jdbcTemplate;
	private static final String SQL = "INSERT INTO taste_item_similarity VALUES(?, ?, ?)";

	public JdbcSimilaritiesItemWriter(DataSource dataSource) {
		Assert.notNull(dataSource, "A datasource is required");

		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void write(List<? extends Similarities> items) throws Exception {
		List<Object []> params = new ArrayList<Object[]>(items.size());

		for (Similarities item : items) {
			Map<Integer, Double> similarities = item.getSimilarites();

			for (Map.Entry<Integer, Double> entry : similarities.entrySet()) {
				Object [] curParams = new Object[3];

				curParams[0] = item.getTagId();
				curParams[1] = entry.getKey();
				curParams[2] = entry.getValue();

				params.add(curParams);
			}
		}

		jdbcTemplate.batchUpdate(SQL, params);
	}
}
