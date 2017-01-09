package com.reactive.loanbroker;

import com.google.gson.Gson;
import com.reactive.loanbroker.agent.ReactorLoanBrokerAgent;
import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.LoanBrokerHandler;
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
import java.util.Optional;
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
    private MockServerHttpResponse mockResponse = new MockServerHttpResponse();
    private ServerWebExchange exchange = new DefaultServerWebExchange(new MockServerHttpRequest(), mockResponse, new DefaultWebSessionManager());
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

        verify(expectedQuotation);
    }

    @Test
    public void bestQuotationServiceFailure(){

        Double loanAmount = 2000d;
        String bankServiceUrl1 = "http://localhost/Bank-1", bankServiceUrl2 = "http://localhost/Bank-2";
        Quotation expectedQuotation = new Quotation("Pseudo-Bank",Double.MAX_VALUE);
        Quotation bank1Quotation = new Quotation(bankServiceUrl1, loanAmount * 0.01);

        when(request.queryParam("loanAmount")).thenReturn(Optional.of(loanAmount.toString()));
        when(serviceLocator.banksURL()).thenReturn(Flux.just(bankServiceUrl1, bankServiceUrl2));
        when(loanRequestService.requestForQuotation(bankServiceUrl1, loanAmount)).thenReturn(Mono.just(bank1Quotation));
        when(loanRequestService.requestForQuotation(bankServiceUrl2, loanAmount)).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        verify(expectedQuotation);
    }


    private void verify(Quotation expectedQuotation){

        Mono<ServerResponse> monoResponse = loanBrokerHandler.bestQuotation(request);

        Mono<Void> result = monoResponse.then(response -> {
            assertEquals(HttpStatus.OK, response.statusCode());
            return response.writeTo(exchange, HandlerStrategies.withDefaults());
        });

        // Verify response received and is 200.OK
        StepVerifier.create(result).expectComplete().verify();

        // Verify best offer
        StepVerifier.create(mockResponse.getBody())
                .consumeNextWith(dataBuffer -> {
                    byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(resultBytes);
                    Stream.of(gson.fromJson(new String(resultBytes),BestQuotationResponse.class))
                            .map(response -> response.getBestOffer())
                            .forEach(actualQuotation -> assertEquals(expectedQuotation, actualQuotation));
                })
                .expectComplete()
                .verify();
    }
}