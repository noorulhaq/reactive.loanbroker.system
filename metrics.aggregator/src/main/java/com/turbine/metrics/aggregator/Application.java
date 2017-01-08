package com.turbine.metrics.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.turbine.stream.EnableTurbineStream;

@SpringBootApplication
@EnableEurekaClient
@EnableTurbineStream
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}

