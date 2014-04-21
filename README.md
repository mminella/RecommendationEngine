### Recommendation Engine
---
This project is intended to implement a simple recommendation engine for use with StackOverflow data.  The code and instructions here will allow you to:

1. Import the StackExchange data.
2. Ingest the data into Hadoop.
3. Preprocess the data to calculate the association between post tags.
4. Export the results from Hadoop back into the database.
5. Run the website and see the recommendations.

A number of projects from the Spring portfolio were used to construct this demo.  You can find more information about them on [spring.io](http://spring.io).

The idea for this demo and help with some of the code came from the blog post [Deploying a massively scalable recommender system with Apache Mahout](http://ssc.io/deploying-a-massively-scalable-recommender-system-with-apache-mahout/).

#### Prerequisites for running this project
---
1. **Hadoop installed** - I used Hadoop 1.2.1 but any version supported by [Spring for Apache Hadoop](http://projects.spring.io/spring-hadoop/) should work.
2. **Spring XD installed** - This has been tested against the current SNAPSHOT of Spring XD as of the writing of this document.  *It needs the code in Spring XD's PR 729 [PR 729](https://github.com/spring-projects/spring-xd/pull/729) or equivelant to work*.
3. **A database** - The SQL script provided and the POM file's dependencies are set up for MySQL, but there is nothing code specific that would prevent you from updating the scripts and dropping in a new driver.
4. **The StackOverflow Data** - The data used in this project comes from the quarterly dump of StackExcahnge's data.  You can find a link to the torrent to download it here: [StackExchange Data](https://archive.org/details/stackexchange) and information about the schema of the data here: [StackExchange data schema](http://meta.stackoverflow.com/questions/2677/database-schema-documentation-for-the-public-data-dump-and-sede).

#### Potential cleanup before process
---
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

#### Import of StackOverflow data
---
Before running the website or the job, you'll need to download and import the data.  The data is provided via XML files that will be imported via a [Spring Batch](http://spring.io/projects/spring-batch) job.

1. Build the project by running `$ mvn package` from the root of the repository (same place this file is located).
2. Execute the job via the command `$ java -jar database-import/target/database-import-1.0-SNAPSHOT.jar importDirectory=<IMPORT_LOCATION>`.  <IMPORT_LOCATION> is the location of the StackOverflow XML files.  This directory should contain at least the Votes.xml, Users.xml, Posts.xml, PostHistory.xml, and Comments.xml (no other data is used for this demo).  This will create the database tables used by the website as well as import the XML data into them.
3. With the data imported, you can either run the website and take a look around or begin processing the data for the recommendation engine.

**Troubleshooting**

*Unable to connect to the database* - Update the values in the application.properties file located in database-import/src/main/resources to be correct for your database instance.

#### Ingest data into Hadoop
---
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
    xd:> job create hdfsImport --definition "jdbchdfs --sql='select pp.owner_user_id, t.id, coalesce(pp.score, 1) from tag t inner join post_tag tp on t.id = tp.tag_id left outer join post p on tp.post_id = p.id inner join post pp on pp.parent_id = p.id where pp.post_type = 2 order by pp.owner_user_id'"
    ```
5. If your version of Spring XD does not support creating and deploying jobs in one step, deploy the job:

    ```
    xd:> job deploy hdfsImport
    ```
6. Launch your job via

    ```
    xd:> job launch hdfsImport
    ```
7. Verify the output by checking HDFS for the output file:

    ```
    $ hadoop fs -ls /xd/hdfsImport
    ```

#### Execute Mahout ItemSimilarityJob
---
If you execute the maven build before, you already will have the packaged job to be deployed onto Spring XD.  If not, execute a maven package (`$ mvn package`) from either the root of the project (the same directory this file is located in) or the root of the recommender job module (recommender-job).

1. Copy the zip file (recommender-recommender.zip) from recommender-job/target to the `<XD_HOME>/modules/jobs` directory.
2. From the Spring XD shell, create the new job

    ```
    xd:> job create mahout --definition "recommender"
    ```
3. From the XD shell, deploy the job (or add `--deploy` to the previous step):

    ```
    xd:> job deploy mahout
    ```
4. From the XD shell, launch the job:

    ```
    xd:> job launch mahout
    ```
5. Verify the output:

    ```
    $ hadoop fs -ls /xd/hdfsImport/postsResults
    ```

#### Import job results into db
---
Once the data has been preprocessed on Hadoop, we'll move the results into the database for use by the website using Spring XD.

1. From the Spring XD shell, create a the new job.  It's important to note that in order for this job to work correctly, the code in Spring XD [PR 729](https://github.com/spring-projects/spring-xd/pull/729) be included or equivilent.  Without this, the hdfsjdbc job won't support tab delimited files.

    ```
    xd:> job create hdfsExport --definition "hdfsjdbc --resources=/xd/hdfsImport/postsResults/part-r-* --names=item_id_a,item_id_b,similarity --tableName=taste_item_similarity --delimiter=\t"
    ```
2. From the XD shell, deploy the job (or add `--deploy` to the previous step):

    ```
    xd:> job deploy hdfsExport
    ```
3. From the XD shell, launch the job:

    ```
    xd:> job launch hdfsExport
    ```
4. Verify the output.  Note, the output from the below command is *not* from the full import.  Your count will be different.  Just confirm that records are there:

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
---
With the offline data processed, we can launch the website, browse the questions and answers, as well as see the recommendations in action.

1. From the root of this project (assuming you've run the `$ mvn package` previously), run `$ java -jar spring-overflow-spring/target/spring-overflow-spring-1.0-SNAPSHOT.war`.  This application is packaged as a WAR file and can be deployed to Tomcat, but should also work as a Spring Boot executable JAR file.
2. Open a browser and navigate to [http://localhost:8080](http://localhost:8080).
3. From here, you can navigate around viewing questions, answers, and comments.
4. To answer a question and see the recommendations, you'll need to login.  Click the Login link in the upper right hand corner.  Enter any username from the database.  The password is hard coded to be password.
5. Once you are logged in, you will be able to navigate to a question and answer it.  Once you submit your answer, up to three additional questions will be recommended on the question page at the top.

