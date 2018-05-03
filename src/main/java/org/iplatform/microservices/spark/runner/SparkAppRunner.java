package org.iplatform.microservices.spark.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.deploy.SparkSubmit;
import org.apache.spark.deploy.master.DriverState;
import org.apache.spark.deploy.rest.CreateSubmissionRequest;
import org.apache.spark.deploy.rest.RestSubmissionClient;
import org.apache.spark.deploy.rest.SubmitRestProtocolResponse;
import org.apache.spark.deploy.yarn.Client;
import org.apache.spark.deploy.yarn.ClientArguments;
import org.iplatform.microservices.spark.config.SparkTaskProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import scala.Predef$;
import scala.collection.JavaConverters$;

public class SparkAppRunner implements CommandLineRunner {

    private final Log logger = LogFactory.getLog(SparkAppRunner.class);

    @Autowired
    private SparkTaskProperties config;
    @Autowired
	private Configuration hadoopConfiguration;
    
    private void runClient(String... args) throws Exception {
    	ArrayList<String> argList = new ArrayList<>();
        if (StringUtils.hasText(config.getAppName())) {
            argList.add("--name");
            argList.add(config.getAppName());
        }
        argList.add("--class");
        argList.add(config.getAppClass());
        argList.add("--master");
        argList.add(config.getMaster());
        argList.add("--deploy-mode");
        argList.add("client");

        argList.add(config.getAppJar());

        if (StringUtils.hasText(config.getResourceArchives())) {
            argList.add("--jars");
            argList.add(config.getResourceArchives());
        }

        argList.addAll(Arrays.asList(config.getAppArgs()));

        try {
            SparkSubmit.main(argList.toArray(new String[argList.size()]));
        } catch (Throwable t) {
            logger.error("Spark Application failed: " + t.getMessage(), t);
            throw new RuntimeException("Spark Application failed", t);
        }
    }
    
    private void runCluster(String... args) throws Exception {
    	RestSubmissionClient rsc = new RestSubmissionClient(config.getRestUrl());

        Map<String, String> sparkProps = new HashMap<>();
        sparkProps.put("spark.app.name", config.getAppName());
        sparkProps.put("spark.master", config.getMaster());
        if (StringUtils.hasText(config.getExecutorMemory())) {
            sparkProps.put("spark.executor.memory", config.getExecutorMemory());
        }
        if (StringUtils.hasText(config.getDriverMemory())) {
            sparkProps.put("spark.driver.memory", config.getDriverMemory());
        }
        if (StringUtils.hasText(config.getResourceArchives())) {
            sparkProps.put("spark.jars", config.getAppJar().trim() + "," + config.getResourceArchives().trim());
        } else {
            sparkProps.put("spark.jars", config.getAppJar());
        }
        scala.collection.immutable.Map<String, String> envMap =
                JavaConverters$.MODULE$.mapAsScalaMapConverter(new HashMap<String, String>()).asScala()
                        .toMap(Predef$.MODULE$.<scala.Tuple2<String, String>>conforms());
        scala.collection.immutable.Map<String, String> propsMap =
                JavaConverters$.MODULE$.mapAsScalaMapConverter(sparkProps).asScala()
                        .toMap(Predef$.MODULE$.<scala.Tuple2<String, String>>conforms());

        CreateSubmissionRequest csr = rsc.constructSubmitRequest(
                config.getAppJar(),
                config.getAppClass(),
                config.getAppArgs(),
                propsMap,
                envMap);

        SubmitRestProtocolResponse resp = rsc.createSubmission(csr);

        String submissionId = getJsonProperty(resp.toJson(), "submissionId");
        logger.info("Submitted Spark App with submissionId: " + submissionId);

        String appState;

        while (true) {
            Thread.sleep(config.getAppStatusPollInterval());
            SubmitRestProtocolResponse stat = rsc.requestSubmissionStatus(submissionId, false);
            appState = getJsonProperty(stat.toJson(), "driverState");
            if (!(appState.equals(DriverState.SUBMITTED().toString()) ||
                    appState.equals(DriverState.RUNNING().toString()) ||
                    appState.equals(DriverState.RELAUNCHING().toString()) ||
                    appState.equals(DriverState.UNKNOWN().toString()))) {
                System.out.println("Spark App completed with status: " + appState);
                appState = appState;
                break;
            }
        }
        if (!appState.equals(DriverState.FINISHED().toString())) {
            throw new RuntimeException("Spark App submission " + submissionId + " failed with status " + appState);
        }
    }
    
    private void runYarn(String... args) throws Exception {
    	SparkConf sparkConf = new SparkConf();
    	sparkConf.set("spark.app.name", config.getAppName());
    	sparkConf.set("spark.submit.deployMode", config.getYarnDeployMode());
    	
    	sparkConf.set("spark.yarn.am.memory", config.getYarnAMMemory());
    	sparkConf.set("spark.driver.memory", config.getDriverMemory());
		sparkConf.set("spark.executor.memory", config.getExecutorMemory());
		
		sparkConf.set("spark.num-executors", String.valueOf(config.getNumExecutors()));
		
		sparkConf.set("spark.yarn.archive", config.getResourceArchives());
		sparkConf.set("spark.yarn.jars", config.getResourceArchives());
		
		List<String> submitArgs = new ArrayList<String>();
		/**
		if (StringUtils.hasText(config.getAppName())) {
			submitArgs.add("--name");
			submitArgs.add(config.getAppName());
		}*/
		submitArgs.add("--jar");
		submitArgs.add(config.getAppJar());
		submitArgs.add("--class");
		submitArgs.add(config.getAppClass());
		/**
		if (StringUtils.hasText(config.getResourceFiles())) {
			submitArgs.add("--files");
			submitArgs.add(config.getResourceFiles());
		}
		if (StringUtils.hasText(config.getResourceArchives())) {
			submitArgs.add("--archives");
			submitArgs.add(config.getResourceArchives());
		}
		submitArgs.add("--executor-memory");
		submitArgs.add(config.getExecutorMemory());
		submitArgs.add("--num-executors");
		submitArgs.add("" + config.getNumExecutors());
		*/
		for (String arg : config.getAppArgs()) {
			submitArgs.add("--arg");
			submitArgs.add(arg);
		}
		
		/** 初始化YARN环境 */
		Map<String, String> configuration = config.getHadoopConfiguration();
		for(String key : configuration.keySet()) {
			hadoopConfiguration.set(key, configuration.get(key));
			logger.info(String.format("#%s = %s", key, configuration.get(key)));
		}
		/**
		hadoopConfiguration.set("fs.defaultFS", "hdfs://172.18.254.106:9000");
		hadoopConfiguration.setBoolean("mapreduce.app-submission.cross-platform", true);// 配置使用跨平台提交任务  
		hadoopConfiguration.set("mapreduce.framework.name", "yarn"); // 指定使用yarn框架  
		hadoopConfiguration.set("yarn.resourcemanager.address", "bomc106:8032"); // 指定resourcemanager  
		hadoopConfiguration.set("yarn.resourcemanager.scheduler.address", "bomc106:8030");// 指定资源分配器  
		hadoopConfiguration.set("mapreduce.jobhistory.address", "bomc107:10020");// 指定historyserver  
		*/
		logger.info("Submit App with args: " + Arrays.asList(submitArgs));
		ClientArguments clientArguments = new ClientArguments(submitArgs.toArray(new String[submitArgs.size()]));
		Client client = new Client(clientArguments, hadoopConfiguration, sparkConf);
		System.setProperty("SPARK_YARN_MODE", "true");
		try {
			client.run();
		} catch (Throwable t) {
			logger.error("Spark Application failed: " + t.getMessage(), t);
			throw new RuntimeException("Spark Application failed", t);
		}
    }

    @Override
    public void run(String... args) throws Exception {
    	String taskMode = config.getTaskMode();
    	if ("client".equalsIgnoreCase(taskMode)) {
    		runClient(args);
    	} else if ("cluster".equalsIgnoreCase(taskMode)) {
    		runCluster(args);
    	} else if ("yarn".equalsIgnoreCase(taskMode)) {
    		runYarn(args);
    	} else {
    		logger.warn(String.format("Not supported, {}.", taskMode));
    	}
    }

    private String getJsonProperty(String json, String prop) {
        try {
            HashMap<String, Object> props = new ObjectMapper().readValue(json, HashMap.class);
            return props.get(prop).toString();
        } catch (IOException ioe) {
            return null;
        }
    }
}
