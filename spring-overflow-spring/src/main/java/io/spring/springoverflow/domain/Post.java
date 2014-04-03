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
package io.spring.springoverflow.domain;

import org.springframework.beans.support.PropertyComparator;
import org.springframework.util.CollectionUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Michael Minella
 */
@Entity
public class Post {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Version
	private long version;

	@Column(name="POST_TYPE")
	private Integer postTypeId;

	@Column(name="CREATION_DATE")
	private Date creationDate;

	@Column(name="ACCEPTED_ANSWER_ID")
	private Long acceptedAnswerId;

	@Column(name="SCORE")
	private Integer score;

	@Column(name="VIEW_COUNT")
	private int viewCount;

	@Column(name="BODY")
	private String body;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "OWNER_USER_ID")
	private User ownerUser;

	@Column(name="TITLE")
	private String title;

	@Column(name="ANSWER_COUNT")
	private int answerCount;

	@Column(name="COMMENT_COUNT")
	private int commentCount;

	@Column(name="FAVORITE_COUNT")
	private int favoriteCount;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name="PARENT_ID")
	@OrderBy("creationDate DESC")
	private Set<Post> answers;

	@OneToMany(mappedBy = "post")
	@OrderBy("creationDate ASC")
	private Set<Comment> comments = new TreeSet<>(new PropertyComparator<>("creationDate", true, true));

	@OneToMany
	@JoinTable(name = "POST_TAG",
			   joinColumns = @JoinColumn(name = "POST_ID"),
			   inverseJoinColumns = @JoinColumn(name = "TAG_ID"))
	private Set<Tag> tags = new HashSet<>();

	@OneToMany(mappedBy = "post")
	private Set<Vote> votes;

	@Transient
	private int voteCount = 0;

	@PostLoad
	public void calculateVotes() {
		if(!CollectionUtils.isEmpty(votes)) {
			for (Vote vote : votes) {
				if(vote.getVoteType() == 2) {
					voteCount++;
				} else if(vote.getVoteType() == 3) {
					voteCount--;
				}
			}
		}
	}

	public Set<Vote> getVotes() {
		return votes;
	}

	public void setVotes(Set<Vote> votes) {
		this.votes = votes;
	}

	public int getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Integer getPostTypeId() {
		return postTypeId;
	}

	public void setPostTypeId(Integer postTypeId) {
		this.postTypeId = postTypeId;
	}

	public Long getAcceptedAnswerId() {
		return acceptedAnswerId;
	}

	public void setAcceptedAnswerId(Long acceptedAnswerId) {
		this.acceptedAnswerId = acceptedAnswerId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public int getViewCount() {
		return viewCount;
	}

	public void setViewCount(int viewCount) {
		this.viewCount = viewCount;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(int answerCount) {
		this.answerCount = answerCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public int getFavoriteCount() {
		return favoriteCount;
	}

	public void setFavoriteCount(int favoriteCount) {
		this.favoriteCount = favoriteCount;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public User getOwnerUser() {
		return ownerUser;
	}

	public void setOwnerUser(User ownerUser) {
		this.ownerUser = ownerUser;
	}

	public Set<Post> getAnswers() {
		return answers;
	}

	public void setAnswers(Set<Post> answers) {
		this.answers = answers;
	}

	public Set<Comment> getComments() {
		return comments;
	}

	public void setComments(Set<Comment> comments) {
		this.comments = comments;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}
}
