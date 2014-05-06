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

import io.spring.recommendation.domain.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * @author Michael Minella
 */
public class PostOwnerExistsService extends AbstractCachingExistsService<Post> {

	@Autowired
	private JdbcOperations jdbcTemplate;

	@Override
	public boolean exists(Post post) {
		String id = String.valueOf(post.getOwnerUserId());

		if(!isCached(id)) {
			Long count = jdbcTemplate.queryForObject("select count(*) from users where id = ?", Long.class, post.getOwnerUserId());
			if(count != null && count > 0) {
				cache(id);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
}
