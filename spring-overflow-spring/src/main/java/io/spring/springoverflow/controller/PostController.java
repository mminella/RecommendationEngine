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
package io.spring.springoverflow.controller;

import io.spring.springoverflow.domain.PageWrapper;
import io.spring.springoverflow.domain.Post;
import io.spring.springoverflow.domain.PostRepository;
import io.spring.springoverflow.domain.Tag;
import io.spring.springoverflow.domain.User;
import io.spring.springoverflow.domain.UserRepository;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Minella
 */
@Controller
public class PostController {

	@Autowired
	ItemBasedRecommender recommender;

	@Autowired
	PostRepository postRepository;

	@Autowired
	UserRepository userRepository;

	@RequestMapping("/")
	public String list(Pageable pageable, Model model) {
		Page<Post> curPage = postRepository.findAllByPostTypeIdOrderByCreationDateDesc(1, pageable);

		PageWrapper<Post> page = new PageWrapper<>(curPage, "/");
		model.addAttribute("page", page);
		return "post/index";
	}

	@RequestMapping("/post/{id}")
	public String show(@PathVariable long id, Model model) {
		Post curPost = postRepository.findOne(id);

		curPost.setViewCount(curPost.getViewCount() + 1);
		postRepository.save(curPost);

		model.addAttribute("post", curPost);
		if(!model.containsAttribute("recommendations")) {
			model.addAttribute("recommendations", new ArrayList<Post>());
		}

		return "post/show";
	}

	@RequestMapping(value = "/post/save", method = RequestMethod.POST)
	public String save(@RequestParam(value = "postId", required = true) long parentPostId,
					   @RequestParam(value = "body", required = true) String body, Model model) throws Exception {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();

		User currentUser = userRepository.findByDisplayName(username);
		long userId = currentUser.getId();

		Post post = new Post();
		post.setBody(body);
		post.setCreationDate(new Date());
		post.setOwnerUser(currentUser);
		post.setPostTypeId(2);

		Post parentPost = postRepository.findOne(parentPostId);
		parentPost.getAnswers().add(post);
		parentPost.setAnswerCount(parentPost.getAnswerCount() + 1);

		postRepository.save(parentPost);

		List<RecommendedItem> items;
		List<Post> matchingPosts = new ArrayList<>();

		// Recommendations by Question's Tags
		matchingPosts.addAll(recommendPostsByItemsTags(parentPost));

		// Recommendations by User's History
		matchingPosts.addAll(recommendPostsByUsers(currentUser));

		// Recommendations By Both
		matchingPosts.addAll(recommendPostsByUsers(currentUser));
		matchingPosts.addAll(recommendPostsByItemsTags(parentPost));

		if(matchingPosts.size() < 3) {
			items = recommendItems(parentPost.getTags());

			for (RecommendedItem item : items) {
				List<Post> posts = postRepository.findByTagId(item.getItemID(), new PageRequest(0, 3));

				matchingPosts.addAll(posts);
			}
		}

		Set<Post> recommendedPosts = new HashSet<>(3);

		if(matchingPosts.size() > 3) {
			Collections.shuffle(matchingPosts);
			recommendedPosts.addAll(matchingPosts.subList(0, 3));
		} else {
			recommendedPosts.addAll(matchingPosts);
		}

		model.addAttribute("post", parentPost);
		model.addAttribute("recommendations", recommendedPosts);

		return "post/show";
	}

	private List<Post> recommendPostsByItemsTags(Post parentPost) throws Exception {
		List<RecommendedItem> items = new ArrayList<>();

		for (Tag tag : parentPost.getTags()) {
			items.addAll(recommender.mostSimilarItems(tag.getId(),10));
		}

		List<Post> matchingPosts = new ArrayList<>();

		for (RecommendedItem item : items) {
			List<Post> posts = postRepository.findByTagId(item.getItemID(), new PageRequest(0, 3));

			matchingPosts.addAll(posts);
		}

		return matchingPosts;
	}

	private List<Post> recommendPostsByUsers(User user) throws Exception {
		List<RecommendedItem> items = recommender.recommend(user.getId(), 10);
		List<Post> matchingPosts = new ArrayList<>();

		for (RecommendedItem item : items) {
			List<Post> posts = postRepository.findByTagId(item.getItemID(), new PageRequest(0, 3));

			matchingPosts.addAll(posts);
		}

		return matchingPosts;
	}

	private List<RecommendedItem> recommendItems(Set<Tag> tags) throws Exception {
		List<RecommendedItem> items = new ArrayList<>();

		for (Tag tag : tags) {
			items.addAll(recommender.mostSimilarItems(tag.getId(),10));
		}

		return items;
	}
}
