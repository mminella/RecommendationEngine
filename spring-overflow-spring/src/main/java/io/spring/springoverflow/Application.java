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
package io.spring.springoverflow;

import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLBooleanPrefJDBCDataModel;
import org.apache.mahout.cf.taste.impl.recommender.AllSimilarItemsCandidateItemsStrategy;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.jdbc.MySQLJDBCInMemoryItemSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Michael Minella
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

	@Bean
	ItemBasedRecommender recommender(DataSource dataSource) throws Exception {
		DataModel dataModel = new MySQLBooleanPrefJDBCDataModel(dataSource);
		ItemSimilarity similarity = new MySQLJDBCInMemoryItemSimilarity(dataSource);
		AllSimilarItemsCandidateItemsStrategy candidateItemsStrategy =
				new AllSimilarItemsCandidateItemsStrategy(similarity);
		return new GenericItemBasedRecommender(dataModel, similarity, candidateItemsStrategy, candidateItemsStrategy);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
