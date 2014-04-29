package io.spring.recommendation.batch.writer;

import io.spring.recommendation.domain.Post;
import org.springframework.batch.item.ItemWriter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostItemWriter implements ItemWriter<Post> {

	private ItemWriter<Post> delegate;
	private JdbcTemplate template;

	public void setDelegate(ItemWriter<Post> delegate, DataSource dataSource) {
		Assert.notNull(delegate);
		this.delegate = delegate;
		this.template = new JdbcTemplate(dataSource);
	}

	@Override
	public void write(List<? extends Post> items) throws Exception {
		final List<Tuple<Long, Long>> postTagPairings = new ArrayList<>();

		for (Post post : items) {
			if(StringUtils.hasText(post.getTags())) {
				post.setTagIds(new ArrayList<Long>());

				String[] tags = post.getTags().split(">");

				for (String tag : tags) {
					String curTag = tag;

					if(tag.startsWith("<")) {
						curTag = tag.substring(1);
					}

					long tagId = getTagId(curTag);
					post.getTagIds().add(tagId);
					postTagPairings.add(new Tuple<>(post.getId(), tagId));
				}
			}
		}

		delegate.write(items);

		template.batchUpdate("insert into POST_TAG (POST_ID, TAG_ID) VALUES (?, ?)", new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, postTagPairings.get(i).getKey());
				ps.setLong(2, postTagPairings.get(i).getValue());
			}

			@Override
			public int getBatchSize() {
				return postTagPairings.size();
			}
		});
	}

	@Cacheable("tag")
	protected long getTagId(final String tag) {
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

	private class Tuple<T,D> {
		private final T key;
		private final D value;

		public Tuple(T key, D value) {
			this.key = key;
			this.value = value;
		}

		public T getKey() {
			return key;
		}

		public D getValue() {
			return value;
		}
	}
}
