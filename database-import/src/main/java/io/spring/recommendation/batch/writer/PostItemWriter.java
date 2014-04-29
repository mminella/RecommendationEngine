package io.spring.recommendation.batch.writer;

import io.spring.recommendation.domain.Post;
import io.spring.recommendation.service.TagService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostItemWriter implements ItemWriter<Post> {

	private ItemWriter<Post> delegate;
	private JdbcTemplate template;
	private TagService tagService;

	public void setTagService(TagService tagService) {
		this.tagService = tagService;
	}

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

					long tagId = tagService.getTagId(curTag);
					System.err.println("tag id from service = " + curTag + "|" + tagId);
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
