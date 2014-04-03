package io.spring.recommendation.domain;

import java.util.Date;
import java.util.List;

public class Post {
	private long id;
	private Integer postTypeId;
	private Long acceptedAnswerId;
	private Date creationDate;
	private Integer score;
	private int viewCount;
	private String body;
	private long ownerUserId;
	private String title;
	private int answerCount;
	private int commentCount;
	private int favoriteCount;
	private String tags;
	private Long parentId;
	private List<Long> tagIds;

	public Post() {}

	public Post(Post origin) {
		this.id = origin.getId();
		this.postTypeId = origin.getPostTypeId();
		this.acceptedAnswerId = origin.getAcceptedAnswerId();
		this.score = origin.getScore();
		this.ownerUserId = origin.getOwnerUserId();
		this.tags = origin.getTags();
		this.body = origin.getBody();
		this.tagIds = origin.getTagIds();
		this.creationDate = origin.getCreationDate();
		this.parentId = origin.getParentId();
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

	public long getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(long ownerUserId) {
		this.ownerUserId = ownerUserId;
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

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public List<Long> getTagIds() {
		return tagIds;
	}

	public void setTagIds(List<Long> tagIds) {
		this.tagIds = tagIds;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
}
