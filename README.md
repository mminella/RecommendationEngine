## Recommendation Engine
---
This project is intended to implement a simple recommendation engine for use with StackOverflow data.  The code and instructions here will allow you to:

1. Import the StackExchange data.
2. Ingest the data into Hadoop.
3. Preprocess the data to calculate the association between post tags.
4. Export the results from Hadoop back into the database.
5. Run the website and see the recommendations.

A number of projects from the Spring portfolio were used to construct this demo.  You can find more information about them on [spring.io](http://spring.io).

The idea for this demo and help with some of the code came from the blog post [Deploying a massively scalable recommender system with Apache Mahout](http://ssc.io/deploying-a-massively-scalable-recommender-system-with-apache-mahout/).  Further help and insight was provided by Pat Ferrel [http://occamsmachete.com/ml/](http://occamsmachete.com/ml/).

### Prerequisites for running this project

1. **Hadoop installed** - I used Hadoop 1.2.1 but any version supported by [Spring for Apache Hadoop](http://projects.spring.io/spring-hadoop/) should work.  
2. **Spring XD installed** - This project works best on the latest release of Spring XD (1.0.0.RELEASE as of this writing).  
3. **A database** - The SQL script provided and the POM file's dependencies are set up for MySQL, but there is nothing code specific that would prevent you from updating the scripts and dropping in a new driver.  
4. **The StackOverflow Data** - The data used in this project comes from the quarterly dump of StackExcahnge's data.  You can find a link to the torrent to download it here: [StackExchange Data](https://archive.org/details/stackexchange) and information about the schema of the data here: [StackExchange data schema](http://meta.stackoverflow.com/questions/2677/database-schema-documentation-for-the-public-data-dump-and-sede).  
5. **Configure the database connection** - The below instructions assume you have updated the two application.properties files in this project to point to your MySql instance.  The location of those two properties files are: database-import/src/main/resources and spring-overflow-spring/src/main/resources.  
6. **Version of Mahout that runs with Hadoop 2.x** - Mahout 0.9 currently only works with Hadoop 1.2 yet Spring XD requires Hadoop 2.x or higher.  To run this project, you'll need to checkout and build the latest version of Mahout (assuming you aren't using their 1.0 release).  You can find information on building Mahout here: [Build Mahout](http://stackoverflow.com/questions/18767843/how-can-i-compile-using-mahout-for-hadoop-2-0/24745296#24745296)  
   '$ mvn clean install -Dhadoop2 -Dhadoop2.version=2.2.0 -DskipTests=true'

### Potential cleanup before process

If you've run this project before, you'll need to do a bit of cleanup.  Specifically, you'll want to drop and recreate the database as well as clean up the directories in Hadoop.  This demo is *not* developed for back to back runs and will fail if this cleanup is not performed between them.

1. Remove old Hadoop data by deleting the two directories used: `$ hadoop fs -rmr /xd/hdfsImport`
2. Remove old temp directory used by Mahout during it's processing: `$ hadoop fs -rmr temp`
3. From your database, drop and recreate the database (the tables will be recrated when you run the import job): 

    ```
    mysql> drop database recommendation_test;
    mysql> create database recommendation_test;    
    ```
    
**Troubleshooting**

*Unable to delete directories due to Hadoop being in safe mode* - Execute the command: `$ hadoop dfsadmin -safemode leave`  
*Unable to connect to Hadoop: `$ hadoop/sbin/hadoop-daemon.sh start namenode -format`

#### Import of StackOverflow data

Before running the website or the job, you'll need to download and import the data.  The data is provided via XML files that will be imported via a [Spring Batch](http://spring.io/projects/spring-batch) job.

1. Build the project by running `$ mvn package` from the root of the repository (same place this file is located).
2. Execute the job via the command `$ java -jar database-import/target/database-import-1.0-SNAPSHOT.jar importDirectory=<IMPORT_LOCATION>`.  <IMPORT_LOCATION> is the location of the StackOverflow XML files.  This directory should contain at least the Votes.xml, Users.xml, Posts.xml, PostHistory.xml, and Comments.xml (no other data is used for this demo).  This will create the database tables used by the website as well as import the XML data into them.
3. With the data imported, you can either run the website and take a look around or begin processing the data for the recommendation engine.

**Troubleshooting**

*Unable to connect to the database* - Update the values in the application.properties file located in database-import/src/main/resources to be correct for your database instance.

### MapReduce Version
---
For the processing of the data by Mahout, there are two versions.  The first is via MapReduce.  The second is via Spark.  This section walks you through the MapReduce version.

#### Ingest data into Hadoop

To injest the data from the database into HDFS so that it can be processed on Hadoop, we'll use [Spring XD](http://spring.io/projects/spring-xd).  Spring XD provides a number of pre-packaged Spring Batch jobs that can be used to do common tasks.  In this case, we'll be using the jdbchdfs job to pipe data from a database to HDFS.

1. Start Hadoop (see the instructions for your installation for information on this).
2. Start Spring XD
	1. For singlenode mode, execute: `$ ./xd-singlenode` from the bin directory of Spring XD.
	2. For more information and for executing Spring XD in other modes, see the Spring XD documentation here: [Spring XD Reference Documentation](http://docs.spring.io/spring-xd)
3. Start the Spring XD Shell
	1. From the bin of the Spring XD Shell installation, execute: `$ ./xd-shell`
	2. For more information on executing the Spring XD Shell, see the Spring XD documentation.
4. From the Spring XD Shell, create your job.  Note, newer versions of Spring XD allow you to create and deploy in one step by adding the `--deploy` option.  Do this if your version supports it. 

    ```
    xd:> job create hdfsImport --definition "jdbchdfs --sql='select pp.owner_user_id, t.id, coalesce(pp.score, 1) from tag t inner join post_tag tp on t.id = tp.tag_id left outer join post p on tp.post_id = p.id inner join post pp on pp.parent_id = p.id where pp.post_type = 2 order by pp.owner_user_id'" --deploy
    ```
5. Launch your job via

    ```
    xd:> job launch hdfsImport
    ```
6. Verify the output by checking HDFS for the output file:

    ```
    $ hadoop fs -ls /xd/hdfsImport
    ```

#### Execute Mahout ItemSimilarityJob

If you execute the maven build before, you already will have the packaged job to be deployed onto Spring XD.  If not, execute a maven package (`$ mvn package`) from either the root of the project (the same directory this file is located in) or the root of the recommender job module (recommender-job).

1. Copy the zip file (recommender-recommender.zip) from recommender-job/target to the `<XD_HOME>/modules/jobs` directory.
2. From the Spring XD shell, create the new job

    ```
    xd:> job create mahout --definition "recommender" --deploy
    ```
3. From the XD shell, launch the job:

    ```
    xd:> job launch mahout
    ```
4. Verify the output:

    ```
    $ hadoop fs -ls /xd/hdfsImport/postsResults
    ```

#### Import job results into db
Once the data has been preprocessed on Hadoop, we'll move the results into the database for use by the website using Spring XD.

1. From the Spring XD shell, create a the new job. 

    ```
    xd:> job create hdfsExport --definition "hdfsjdbc --resources=/xd/hdfsImport/postsResults/part-r-* --names=item_id_a,item_id_b,similarity --tableName=taste_item_similarity --delimiter='\t'" --deploy
    ```
2. From the XD shell, launch the job:

    ```
    xd:> job launch hdfsExport
    ```
3. Verify the output.  Note, the output from the below command is *not* from the full import.  Your count will be different.  Just confirm that records are there:

    ```
    mysql> select count(*) from taste_item_similarity;
    +----------+
    | count(*) |
    +----------+
    |    81217 |
    +----------+
    1 row in set (0.14 sec)
     ```
 
### Spark Version
---
Mahout is "all in" on Spark.  They have tagged all of their algorithms implemented via MapReduce as legacy.  In it's place, they have chosen Spark as the platform of choice to run their algorithms on.  As of the writing of this README, while the Spark implementations can recreate the generation of the model, the model is different and Mahout does not provide a Recommender that uses it.  Their vision is to use a search engine like Solr to provide the recommendations.  While they are developing a Recommender as well, in the future, we'll need to massage the data to use the existing Recommenders.

#### Ingest data into Hadoop

The Spark version of the item-similarity job takes the same input as the MapReduce version so there is no difference here.  See the above instructions for the ingestion piece.


#### Execute Mahout Spark ItemSimilarityJob

If you execute the maven build before, you already will have the packaged job to be deployed onto Spring XD.  If not, execute a maven package (`$ mvn package`) from either the root of the project (the same directory this file is located in) or the root of the spark recommender job module (spark-recommender-job).

1. Copy the zip file (spark-recommender-spark-recommender.zip) from spark-recommender-job/target to the `<XD_HOME>/modules/jobs` directory.
2. From the Spring XD shell, create the new job

    ```
    xd:> job create mahout-spark --definition "spark-recommender" --deploy
    ```
3. From the XD shell, launch the job:

    ```
    xd:> job launch mahout-spark
    ```
4. Verify the output:

    ```
    $ hadoop fs -ls /xd/hdfsImport/results
    ```
    
#### Import job results into db
Once the data has been preprocessed on Hadoop, we'll move the results into the database for use by the website using Spring XD and Spring Batch.  Unfortunately, due to the new 

1. Copy the zip file (spark-import-job-spark-import-job.zip) from spark-import-job/target to the `<XD_HOME>/modules/jobs` directory.
2. From the Spring XD shell, create the new job

    ```
    xd:> job create import-spark --definition "spark-import-job" --deploy
    ```
3. From the XD shell, launch the job:

    ```
    xd:> job launch import-spark
    ```
4. Verify the output:

    ```
    mysql> select count(*) from taste_item_similarity;
    +----------+
    | count(*) |
    +----------+
    |    81217 |
    +----------+
    1 row in set (0.14 sec)
     ```



#### Launch the website
With the offline data processed, we can launch the website, browse the questions and answers, as well as see the recommendations in action.

1. From the root of this project (assuming you've run the `$ mvn package` previously), run `$ java -jar spring-overflow-spring/target/spring-overflow-spring-1.0-SNAPSHOT.war`.  This application is packaged as a WAR file and can be deployed to Tomcat, but should also work as a Spring Boot executable JAR file.
2. Open a browser and navigate to [http://localhost:8080](http://localhost:8080).
3. From here, you can navigate around viewing questions, answers, and comments.
4. To answer a question and see the recommendations, you'll need to login.  Click the Login link in the upper right hand corner.  Enter any username from the database.  The password is hard coded to be password.
5. Once you are logged in, you will be able to navigate to a question and answer it.  Once you submit your answer, up to three additional questions will be recommended on the question page at the top.

