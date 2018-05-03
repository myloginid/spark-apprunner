package org.iplatform.microservices.spark;

import org.iplatform.microservices.spark.config.SparkTaskConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ SparkTaskConfiguration.class })
public class SparkDemoTaskApplication {
	public static void main(String[] args) {
		SpringApplication.run(SparkDemoTaskApplication.class, args);
	}
}
