package com.reactive.bank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.DelegatingWebReactiveConfiguration;
import reactor.ipc.netty.http.server.HttpServer;
import java.util.concurrent.CountDownLatch;


@SpringBootApplication(scanBasePackages = "com.reactive")
@EnableEurekaClient
@Import(DelegatingWebReactiveConfiguration.class)
public class Application {

	@Value("${server.port}")
	private int port = 8090;

	@Autowired
	private ApplicationContext context;

	public static void main(String[] args) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		new SpringApplicationBuilder(Application.class).web(false).run(args);
		latch.await();
	}

	@Bean
	public HttpServer reactorServer(){
		HttpHandler handler = DispatcherHandler.toHttpHandler(context);
		ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
		HttpServer httpServer = HttpServer.create(port);
		httpServer.newHandler(adapter).block();
		return httpServer;
	}
}