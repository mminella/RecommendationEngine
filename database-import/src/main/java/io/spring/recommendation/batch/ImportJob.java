package io.spring.recommendation.batch;

import io.spring.recommendation.batch.writer.PostItemWriter;
import io.spring.recommendation.domain.Comment;
import io.spring.recommendation.domain.Post;
import io.spring.recommendation.domain.User;
import io.spring.recommendation.domain.Vote;
import io.spring.recommendation.service.TagService;
import io.spring.recommendation.service.TagServiceImpl;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.oxm.castor.CastorMarshaller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableCaching
@EnableAutoConfiguration
@EnableBatchProcessing
public class ImportJob {

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(new ConcurrentMapCache("tag")));
		return manager;
	}

	@Bean
	@StepScope
	protected ItemStreamReader<User> usersItemReader(@Value("#{jobParameters[importDirectory]}") String importDirectory) throws IOException{
		CastorMarshaller marshaller = new CastorMarshaller();
		marshaller.setTargetClass(User.class);
		marshaller.setMappingLocation(new ClassPathResource("/userMapping.xml"));
		marshaller.afterPropertiesSet();

		StaxEventItemReader<User> itemReader = new StaxEventItemReader<>();
		itemReader.setUnmarshaller(marshaller);
		itemReader.setFragmentRootElementName("row");
		itemReader.setResource(new FileSystemResource(importDirectory + "Users.xml"));
		itemReader.setStrict(true);

		return itemReader;
	}

	@Bean
	protected ItemWriter<User> usersItemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<User> writer = new JdbcBatchItemWriter<>();

		writer.setDataSource(dataSource);
		writer.setSql("insert into users (ID, VERSION, REPUTATION, CREATION_DATE, DISPLAY_NAME, LAST_ACCESS_DATE, LOCATION, ABOUT, VIEWS, UP_VOTES, DOWN_VOTES) values (:id, 0, :reputation, :creationDate, :displayName, :lastAccessDate, :location, :about, :views, :upVotes, :downVotes)");
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<User>());
		writer.afterPropertiesSet();

		return writer;
	}

	@Bean
	protected Step step1(DataSource dataSource) throws Exception {
		return this.steps.get("step1")
				.<User, User> chunk(1000)
				.reader(usersItemReader(null))
				.writer(usersItemWriter(dataSource))
				.build();
	}

	@Bean
	protected TagService tagService() {
		TagServiceImpl service = new TagServiceImpl();

		return service;
	}

	@Bean
	@StepScope
	protected ItemStreamReader<Post> postItemReader(@Value("#{jobParameters[importDirectory]}") String importDirectory) throws IOException{
		CastorMarshaller marshaller = new CastorMarshaller();
		marshaller.setTargetClass(Post.class);
		marshaller.setMappingLocation(new ClassPathResource("/postMapping.xml"));
		marshaller.afterPropertiesSet();

		StaxEventItemReader<Post> itemReader = new StaxEventItemReader<>();
		itemReader.setUnmarshaller(marshaller);
		itemReader.setFragmentRootElementName("row");
		itemReader.setResource(new FileSystemResource(importDirectory + "Posts.xml"));
		itemReader.setStrict(true);

		return itemReader;
	}

	@Bean
	protected ItemWriter<Post> postItemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<Post> delegate = new JdbcBatchItemWriter<>();

		delegate.setDataSource(dataSource);
		delegate.setSql("insert into post (ID, VERSION, POST_TYPE, ACCEPTED_ANSWER_ID, CREATION_DATE, SCORE, VIEW_COUNT, BODY, OWNER_USER_ID, TITLE, ANSWER_COUNT, COMMENT_COUNT, FAVORITE_COUNT, PARENT_ID) values (:id, 0, :postTypeId, :acceptedAnswerId, :creationDate, :score, :viewCount, :body, :ownerUserId, :title, :answerCount, :commentCount, :favoriteCount, :parentId)");
		delegate.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Post>());
		delegate.afterPropertiesSet();

		PostItemWriter writer = new PostItemWriter();
		writer.setDelegate(delegate);
		writer.setTagService(tagService());

		return writer;
	}

	@Bean
	protected Step step2(DataSource dataSource) throws Exception {
		return this.steps.get("step2")
					   .<Post, Post> chunk(1000)
					   .reader(postItemReader(null))
					   .writer(postItemWriter(dataSource))
					   .faultTolerant()
					   .skipPolicy(new SkipPolicy() {
						   @Override
						   public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
							   if (t instanceof DataIntegrityViolationException && t.getMessage().contains("POST_USER")) {
								   return true;
							   } else {
								   return false;
							   }
						   }
					   })
					   .transactionAttribute(new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED))
					   .build();
	}

	@Bean
	@StepScope
	protected ItemStreamReader<Vote> voteItemReader(@Value("#{jobParameters[importDirectory]}") String importDirectory) throws IOException{
		CastorMarshaller marshaller = new CastorMarshaller();
		marshaller.setTargetClass(Vote.class);
		marshaller.setMappingLocation(new ClassPathResource("/votesMapping.xml"));
		marshaller.afterPropertiesSet();

		StaxEventItemReader<Vote> itemReader = new StaxEventItemReader<>();
		itemReader.setUnmarshaller(marshaller);
		itemReader.setFragmentRootElementName("row");
		itemReader.setResource(new FileSystemResource(importDirectory + "Votes.xml"));
		itemReader.setStrict(true);

		return itemReader;
	}

	@Bean
	protected ItemWriter<Vote> votesItemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<Vote> writer = new JdbcBatchItemWriter<>();

		writer.setDataSource(dataSource);
		writer.setSql("insert into VOTES (ID, VERSION, POST_ID, VOTE_TYPE, CREATION_DATE) values (:id, 0, :postId, :voteType, :creationDate)");
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Vote>());
		writer.afterPropertiesSet();

		return writer;
	}

	@Bean
	protected Step step3(DataSource dataSource) throws Exception {
		return this.steps.get("step3")
					   .<Vote, Vote> chunk(1000)
					   .reader(voteItemReader(null))
					   .writer(votesItemWriter(dataSource))
					   .faultTolerant()
					   .skipPolicy(new SkipPolicy() {
						   @Override
						   public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
							   if (t instanceof DataIntegrityViolationException && t.getMessage().contains("POST_ID")) {
								   return true;
							   } else {
								   return false;
							   }
						   }
					   })
					   .build();
	}

	@Bean
	@StepScope
	protected ItemStreamReader<Comment> commentItemReader(@Value("#{jobParameters[importDirectory]}") String importDirectory) throws IOException{
		CastorMarshaller marshaller = new CastorMarshaller();
		marshaller.setTargetClass(Comment.class);
		marshaller.setMappingLocation(new ClassPathResource("/commentMapping.xml"));
		marshaller.afterPropertiesSet();

		StaxEventItemReader<Comment> itemReader = new StaxEventItemReader<>();
		itemReader.setUnmarshaller(marshaller);
		itemReader.setFragmentRootElementName("row");
		itemReader.setResource(new FileSystemResource(importDirectory + "Comments.xml"));
		itemReader.setStrict(true);

		return itemReader;
	}

	@Bean
	protected ItemWriter<Comment> commentItemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<Comment> writer = new JdbcBatchItemWriter<>();

		writer.setDataSource(dataSource);
		writer.setSql("insert into COMMENTS (ID, VERSION, POST_ID, VALUE, CREATION_DATE, USER_ID, SCORE) values (:id, 0, :postId, :value, :creationDate, :userId, :score)");
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Comment>());
		writer.afterPropertiesSet();

		return writer;
	}

	@Bean
	protected Step step4(DataSource dataSource) throws Exception {
		return this.steps.get("step4")
					   .<Comment, Comment> chunk(1000)
					   .reader(commentItemReader(null))
					   .writer(commentItemWriter(dataSource))
					   .faultTolerant()
					   .skipPolicy(new SkipPolicy() {
						   @Override
						   public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
							   if (t instanceof DataIntegrityViolationException && t.getMessage().contains("USER_ID")) {
								   return true;
							   } else {
								   return false;
							   }
						   }
					   })
					   .build();
	}

	@Bean
	public Job job(DataSource dataSource) throws Exception {
		Flow step3Flow = new FlowBuilder<Flow>("step3Flow").start(step3(dataSource)).end();
		Flow step4Flow = new FlowBuilder<Flow>("step4Flow").start(step4(dataSource)).end();
		return this.jobs.get("import")
						.start(step1(dataSource))
						.next(step2(dataSource))
						.split(new SimpleAsyncTaskExecutor())
						.add(step3Flow, step4Flow)
						.end()
						.build();
	}

	public static void main(String[] args) throws Exception {
		System.exit(SpringApplication.exit(SpringApplication.run(
				ImportJob.class, args)));
	}
}
