package com.reactive.loanbroker;

import com.google.gson.Gson;
import com.reactive.bank.BankController;
import com.reactive.loanbroker.agent.ReactorLoanBrokerAgent;
import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.LoanBrokerHandler;
import com.reactive.loanbroker.handler.error.ServiceError;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.model.Quotation;
import org.junit.After;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebReactive;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.test.StepVerifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by husainbasrawala on 1/9/17.
 */
public class LoanBrokerTests {


    private Gson gson = new Gson();
    private ServerRequest request;
    private ReactiveBankServiceLocator serviceLocator;
    private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;
    private LoanBrokerHandler loanBrokerHandler;
    private static final int PORT = 7776;
    private static boolean isServerStarted;

    @Before
    public void setup(){
        request = mock(ServerRequest.class);
        serviceLocator = mock(ReactiveBankServiceLocator.class);
        reactorLoanBrokerAgent = new ReactorLoanBrokerAgent(serviceLocator,WebClient.create(new ReactorClientHttpConnector()));
        loanBrokerHandler = new LoanBrokerHandler(reactorLoanBrokerAgent);
    }

    @Before
    public void startServer(){
        if(!isServerStarted) {
            AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
            ac.register(TestingConfiguration.class);
            ac.refresh();

            ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(WebHttpHandlerBuilder.webHandler(new DispatcherHandler(ac)).build());
            HttpServer httpServer = HttpServer.create(PORT);
            httpServer.newHandler(adapter).block();
            isServerStarted = true;
        }
    }


    @Test
    public void bestQuotationHappyScenario(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost:"+PORT+"/Bank-1", bankServiceUrl2 = "http://localhost:"+PORT+"/Bank-4";
        Quotation expectedQuotation = new Quotation("Bank-1",980d);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyQuotation(verifyHttpResponse(HttpStatus.OK),expectedQuotation);
    }

    @Test
    public void partialServiceFailure(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost:"+PORT+"/Bank-1", bankServiceUrl2 = "http://localhost:"+PORT+"/Bank-2";
        Quotation expectedQuotation = new Quotation("Bank-1",980d);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyQuotation(verifyHttpResponse(HttpStatus.OK),expectedQuotation);
    }

    @Test
    public void totalServiceFailure(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost:"+PORT+"/Bank-2";

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyError(verifyHttpResponse(HttpStatus.OK),ServiceError.noBankServiceAvailable());
    }

    @Test
    public void oneSlowService(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost:"+PORT+"/Bank-1", bankServiceUrl2 = "http://localhost:"+PORT+"/Bank-3";
        Quotation expectedQuotation = new Quotation("Bank-1",980d);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyQuotation(verifyHttpResponse(HttpStatus.OK),expectedQuotation);
    }

    @Test
    public void allSlowServices(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost:"+PORT+"/Bank-3", bankServiceUrl2 = "http://localhost:"+PORT+"/Bank-3";

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyError(verifyHttpResponse(HttpStatus.OK),ServiceError.noBankServiceAvailable());
    }

    @Test
    public void noServiceAvailable(){

        Double loanAmount = 2000d;
        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.empty());
        verifyError(verifyHttpResponse(HttpStatus.OK),ServiceError.noBankServiceAvailable());
    }


    @Test
    public void noLoanAmount(){

        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";

        when(request.queryParam("loanAmount")).thenReturn(Optional.empty());
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));

        verifyError(verifyHttpResponse(HttpStatus.BAD_REQUEST),ServiceError.loanAmountRequired());
    }


    /*
        Verification helper methods
     */

    private MockServerHttpResponse verifyHttpResponse(HttpStatus httpStatus){

        MockServerHttpResponse mockResponse = new MockServerHttpResponse();
        ServerWebExchange exchange = new DefaultServerWebExchange(new MockServerHttpRequest(), mockResponse, new DefaultWebSessionManager());
        Mono<ServerResponse> monoResponse = loanBrokerHandler.bestQuotation(request);

        Mono<Void> result = monoResponse.then(response -> {
            assertEquals(httpStatus, response.statusCode());
            return response.writeTo(exchange, HandlerStrategies.withDefaults());
        });

        StepVerifier.create(result).expectComplete().verify();

        return mockResponse;
    }


    private void verifyQuotation(MockServerHttpResponse mockResponse,Quotation expectedQuotation){
        verify(mockResponse,(json)-> Stream.of(gson.fromJson(json,BestQuotationResponse.class))
                .map(response -> response.getBestOffer())
                .forEach(actualQuotation -> assertEquals(expectedQuotation, actualQuotation)));
    }

    private void verifyError(MockServerHttpResponse mockResponse,ServiceError expectedError){
        verify(mockResponse,(json)-> Stream.of(gson.fromJson(json,ServiceError.class))
                .forEach(actualError -> assertEquals(expectedError, actualError)));
    }

    private void verify(MockServerHttpResponse mockResponse,Consumer<String> assertion){

        // Verify best offer
        StepVerifier.create(mockResponse.getBody())
                .consumeNextWith(dataBuffer -> {
                    byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(resultBytes);

                    System.out.println(new String(resultBytes));

                    assertion.accept(new String(resultBytes));
                })
                .expectComplete()
                .verify();
    }


    @Configuration
    @EnableWebReactive
    @SuppressWarnings("unused")
    static class TestingConfiguration {

        @Bean
        public BankController bankController() {
            return new BankController();
        }
    }

    @After
    public void tearDown(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}