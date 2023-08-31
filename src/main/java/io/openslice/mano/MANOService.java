/*-
 * ========================LICENSE_START=================================
 * io.openslice.bugzilla
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
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
 * =========================LICENSE_END==================================
 */
package io.openslice.mano;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;


/**
 * @author ichatzis
 *
 * based on
 * https://github.com/apache/camel/tree/master/examples/camel-example-spring-boot-activemq
 * https://github.com/indrabasak/spring-consul-example 
 */
// This is equivalent to @Configuration, @EnableAutoConfiguration, and @ComponentScan
@SpringBootApplication
// This annotation is used for consul
//@EnableRetry
// This is from spring-cloud to allow local configuration application
// @EnableAutoConfiguration annotation tells Spring Boot to "guess" how you will want to configure Spring, 
// based on the jar dependencies that you have added. For example, If HSQLDB is on your classpath, and you 
// have not manually configured any database connection beans, then Spring will auto-configure an in-memory database. 
@EnableAutoConfiguration
// This is enabled by default
@EnableConfigurationProperties
@ComponentScan(basePackages = { 
		"io.openslice.mano",
		"io.openslice.centrallog.client"	})
public class MANOService {
	public static void main(String[] args) {		
		SpringApplication.run( MANOService.class, args);	
	}
}