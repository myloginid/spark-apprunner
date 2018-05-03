#!/bin/sh

#####################
# 1. spark client
#####################

#java -jar spark-apprunner-0.0.1.jar \
#--spark.taskMode=client \
#--spark.appName=test_appname \
#--spark.master="local[1]" \
#--spark.appClass="com.boco.bomc.spark.JavaSparkPi" \
#--spark.appJar="hdfs://172.18.254.106:9000/spark/jars/spark-example-0.0.1.jar" \
#--spark.appArgs="10"

#####################
# 2. spark cluster
#####################

#java -jar spark-apprunner-0.0.1.jar \
#--spark.taskMode=cluster \
#--spark.appName=test_appname \
#--spark.master="spark://172.18.254.106:7077" \
#--spark.restUrl="spark://172.18.254.106:6066" \
#--spark.appClass="com.boco.bomc.spark.JavaSparkPi" \
#--spark.appJar="hdfs://172.18.254.106:9000/spark/jars/spark-example-0.0.1.jar" \
#--spark.appArgs="10"

#####################
# 3. spark yarn
#####################

java -jar spark-apprunner-0.0.1.jar \
--spark.taskMode=yarn \
--spark.appName=test_appname \
--spark.master="yarn" \
--yarnDeployMode="cluster" \
--yarnAMMemory="512M" \
--spark.resourceArchives="hdfs://172.18.254.106:9000/spark/2.2.1/jars" \
--spark.driverMemory="512M" \
--spark.executorMemory="512M" \
--spark.hadoopConfiguration.fs.defaultFS="hdfs://172.18.254.106:9000" \
--spark.hadoopConfiguration.mapreduce.app-submission.cross-platform="true" \
--spark.hadoopConfiguration.mapreduce.framework.name="yarn" \
--spark.hadoopConfiguration.yarn.resourcemanager.address="bomc106:8032" \
--spark.hadoopConfiguration.yarn.resourcemanager.scheduler.address="bomc106:8030" \
--spark.hadoopConfiguration.mapreduce.jobhistory.address="bomc107:10020" \
--spark.appClass="com.boco.bomc.spark.JavaSparkPi" \
--spark.appJar="hdfs://172.18.254.106:9000/spark/jars/spark-example-0.0.1.jar" \
--spark.appArgs=10



