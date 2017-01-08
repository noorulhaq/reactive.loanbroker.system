package com.reactive.bank;


import com.reactive.bank.model.Quotation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import javax.naming.ServiceUnavailableException;
import java.time.Duration;

@RestController
public class BankController {


    @RequestMapping("/{bank}/health")
    public Mono<Void> healthCheck(){
        return Mono.empty();
    }


    @RequestMapping("/{bank}/quotation") // bank name format is bank-[1-9]
    public Mono<Quotation> quotation( final @PathVariable("bank") String bank, final @RequestParam(value="loanAmount", required=true) Double loanAmount){
        char bankIndex = bank.charAt(5);

        double pseudoInterestRate = bankIndex=='5' ? 0.001d : ((double)bankIndex)/100d;

        if(bankIndex=='2')
            return Mono.error(new ServiceUnavailableException("bank-"+bankIndex+" service is unavailable"));

        if(bankIndex=='3')
            return Mono.delay(Duration.ofMillis(2000)).then(Mono.just(new Quotation("Bank-"+bankIndex,loanAmount)));

        return  Mono.just(new Quotation("Bank-"+bankIndex,loanAmount * pseudoInterestRate));
    }

}
