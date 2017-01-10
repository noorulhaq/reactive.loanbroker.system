package com.reactive.loanbroker;

import com.google.gson.Gson;
import com.reactive.loanbroker.agent.ReactorLoanBrokerAgent;
import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.LoanBrokerHandler;
import com.reactive.loanbroker.handler.error.ServiceError;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.model.Quotation;
import com.reactive.loanbroker.service.SpringReactorLoanRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Duration;
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
    private SpringReactorLoanRequestService loanRequestService;
    private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;
    private LoanBrokerHandler loanBrokerHandler;

    @Before
    public void setup(){
        request = mock(ServerRequest.class);
        serviceLocator = mock(ReactiveBankServiceLocator.class);
        loanRequestService = mock(SpringReactorLoanRequestService.class);
        reactorLoanBrokerAgent = new ReactorLoanBrokerAgent(loanRequestService,serviceLocator);
        loanBrokerHandler = new LoanBrokerHandler(reactorLoanBrokerAgent);
    }

    @Test
    public void bestQuotationHappyScenario(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";
        Quotation expectedQuotation = new Quotation(bankServiceUrl1,20d);
        Quotation bank1Quotation = new Quotation(bankServiceUrl1, loanAmount * 0.01);
        Quotation bank2Quotation = new Quotation(bankServiceUrl2, loanAmount * 0.02);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));
        when(loanRequestService.requestForQuotation(bankServiceUrl1, loanAmount)).thenReturn(Mono.just(bank1Quotation));
        when(loanRequestService.requestForQuotation(bankServiceUrl2, loanAmount)).thenReturn(Mono.just(bank2Quotation));

        verifyQuotation(verifyHttpResponse(HttpStatus.OK),expectedQuotation);
    }

    @Test
    public void partialServiceFailure(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";
        Quotation expectedQuotation = new Quotation(bankServiceUrl1,20d);
        Quotation bank1Quotation = new Quotation(bankServiceUrl1, loanAmount * 0.01);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));
        when(loanRequestService.requestForQuotation(bankServiceUrl1, loanAmount)).thenReturn(Mono.just(bank1Quotation));
        when(loanRequestService.requestForQuotation(bankServiceUrl2, loanAmount)).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        verifyQuotation(verifyHttpResponse(HttpStatus.OK),expectedQuotation);
    }

    @Test
    public void totalServiceFailure(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));
        when(loanRequestService.requestForQuotation(bankServiceUrl1, loanAmount)).thenReturn(Mono.error(new RuntimeException("Service unavailable")));
        when(loanRequestService.requestForQuotation(bankServiceUrl2, loanAmount)).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        verifyError(verifyHttpResponse(HttpStatus.OK),ServiceError.noBankServiceAvailable());
    }

    @Test
    public void slowService(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";
        Quotation bank1Quotation = new Quotation(bankServiceUrl1, loanAmount * 0.01);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));
        when(loanRequestService.requestForQuotation(bankServiceUrl1, loanAmount)).thenReturn(Mono.just(bank1Quotation));
        when(loanRequestService.requestForQuotation(bankServiceUrl2, loanAmount)).thenReturn(Mono.delay(Duration.ofMillis(4000))
                .then(Mono.just(new Quotation("Slow Bank",loanAmount))));

        verifyError(verifyHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR),ServiceError.serviceTookMoreTime());
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
}