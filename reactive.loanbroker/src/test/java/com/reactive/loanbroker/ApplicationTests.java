package com.reactive.loanbroker;

import com.reactive.loanbroker.agent.ReactorLoanBrokerAgent;
import com.reactive.loanbroker.agent.RxLoanBrokerAgent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	@Autowired
	private ReactorLoanBrokerAgent reactorLoanBrokerAgent;


	@Autowired
	private RxLoanBrokerAgent rxloanBrokerAgent;

	@Test
	public void bestQuotationTest() {
		double loanAmount  = 1000d;
		StepVerifier.create(reactorLoanBrokerAgent.findBestQuotation(loanAmount))
				.expectNextMatches(bestQuotation -> {
					System.err.println(bestQuotation.getRequestedLoanAmount()+"+++++++"+loanAmount);
					return bestQuotation.getRequestedLoanAmount().doubleValue() == loanAmount;
				})
				.expectComplete()
				.verify();

	}


	@Test
	public void bulkBestQuotationTest() {

			int numberOfRequests = 20;

			StepVerifier.create(Flux.range(1,numberOfRequests).doOnNext( (i) -> {

				double loanAmount = 1000d + i;
				StepVerifier.create(reactorLoanBrokerAgent.findBestQuotation(loanAmount)).expectNextMatches(bestQuotation -> {
					System.err.println(bestQuotation.getRequestedLoanAmount()+"+++++++"+loanAmount);
					return bestQuotation.getRequestedLoanAmount().doubleValue() == loanAmount;
				}).expectComplete().verify();

				System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");

			})).expectNextCount(numberOfRequests).expectComplete().verify();
	}


//	@Test
//	public void bestQuotationRxTest() {
//
//		StepVerifier.create(Mono.from(rxloanBrokerAgent.findBestQuotation(1000d).singleOrError().toFlowable()))
//				.expectNextMatches(bestQuotation -> bestQuotation.getOffer().doubleValue() == 1000d*0.001)
//				.expectComplete()
//				.verify();
//
//	}
//
//	@Test
//	public void bulkBankQuotationTest() {
//
//		for (int i=1;i<40;i++) {
//			double loanAmount  = 1000d+i;
//			StepVerifier.create(loanBrokerAgent.requestForQuotation("bank-3",loanAmount))
//					.expectNextMatches(bestQuotation -> {
//								System.err.println(bestQuotation+"+++++++"+loanAmount);
//								return bestQuotation.getOffer().doubleValue() == loanAmount;
//							}
//					)
//					.thenCancel()
//					.verify();
//			System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
//		}
//
//	}



}
