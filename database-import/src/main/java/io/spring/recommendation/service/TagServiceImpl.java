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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Michael Minella
 */
public class TagServiceImpl implements TagService {

	@Autowired
	private JdbcOperations template;

	@Override
	@Cacheable("tag")
	public long getTagId(final String tag) {
		long id = -1;

		try {
			id = template.queryForObject("select id from TAG where TAG = ?", Long.class, tag);
		} catch (EmptyResultDataAccessException ignore) { }

		if(id < 0) {
			KeyHolder keyHolder = new GeneratedKeyHolder();

			template.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					PreparedStatement ps = con.prepareStatement("insert into TAG (VERSION, TAG) VALUES (1, ?)", new String[] {"ID"});

					ps.setString(1, tag);

					return ps;
				}
			}, keyHolder);

			id = keyHolder.getKey().longValue();
		}

		return id;
	}
}
