package com.reactive.loanbroker;

import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.LoanBrokerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.DelegatingWebReactiveConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.ipc.netty.http.server.HttpServer;
import java.util.concurrent.CountDownLatch;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@SpringBootApplication(scanBasePackages = "com.reactive")
@EnableEurekaClient
@EnableCircuitBreaker
@Import(DelegatingWebReactiveConfiguration.class)
public class Application {

	@Value("${server.port}")
	private int port = 8090;

	@Autowired
	@Qualifier("reactorLoanBrokerAgent")
	private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;


	@Autowired
	private ApplicationContext context;

	public static void main(String[] args) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		new SpringApplicationBuilder(Application.class).web(false).run(args);
		latch.await();
	}

	@Bean
	public HttpServer reactorServer(){
		HttpHandler handler = routingHandler();
		//HttpHandler handler = dispatcherHandler(context);
		ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
		HttpServer httpServer = HttpServer.create(port);
		httpServer.newHandler(adapter).block();
		return httpServer;
	}

	public HttpHandler dispatcherHandler(ApplicationContext context) {
		return DispatcherHandler.toHttpHandler(context);
	}

	public HttpHandler routingHandler() {
		LoanBrokerHandler loanBrokerHandler = new LoanBrokerHandler(reactorLoanBrokerAgent);
		return RouterFunctions.toHttpHandler(route(GET("/reactor/quotation"), loanBrokerHandler::bestQuotation));
	}

	@Bean
	public AsyncRestTemplate restTemplate(){
		return new AsyncRestTemplate(new Netty4ClientHttpRequestFactory());
	}

	@Bean
	public WebClient webClient(){
		return WebClient.create(new ReactorClientHttpConnector());
	}

}
