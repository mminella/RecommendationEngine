package io.spring.recommendation.batch.writer;

import io.spring.recommendation.domain.Post;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
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
		for (Post post : items) {
			if(StringUtils.hasText(post.getTags())) {
				post.setTagIds(new ArrayList<Long>());

				String[] tags = post.getTags().split(">");

				for (String tag : tags) {
					String curTag = tag;

					if(tag.startsWith("<")) {
						curTag = tag.substring(1);
					}

					post.getTagIds().add(getTagId(curTag));
				}
			}
		}

		delegate.write(items);

		for (Post post : items) {
			if(!CollectionUtils.isEmpty(post.getTagIds())) {
				for (Long tagId : post.getTagIds()) {
					template.update("insert into POST_TAG (POST_ID, TAG_ID) VALUES (?, ?)", post.getId(), tagId);
				}
			}
		}
	}

	protected long getTagId(String tag) {
		long id = -1;

		try {
			id = template.queryForObject("select id from TAG where TAG = ?", Long.class, tag);
		} catch (EmptyResultDataAccessException ignore) { }

		if(id < 0) {
			template.update("insert into TAG (VERSION, TAG) VALUES (1, ?)", tag);
			id = template.queryForObject("select id from TAG where TAG = ?", Long.class, tag);
		}

		return id;
	}
}
