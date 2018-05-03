/*
 * Copyright 2016 the original author or authors.
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

package org.iplatform.microservices.spark.config;

import org.iplatform.microservices.spark.runner.SparkAppRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link CommandLineRunner} implementation that will run a Spark App in cluster mode using
 * configuration properties provided.
 *
 * @author Thomas Risberg
 */
@EnableTask
@Configuration
@EnableConfigurationProperties(SparkTaskProperties.class)
public class SparkTaskConfiguration {

    @Bean
    public CommandLineRunner commandLineRunner() {
        return new SparkAppRunner();
    }

}