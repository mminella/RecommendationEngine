package io.spring.recommendation.service;

import org.springframework.cache.annotation.Cacheable;

/**
 * @author Michael Minella
 */
public interface TagService {
	@Cacheable("tag")
	long getTagId(final String tag);
}
