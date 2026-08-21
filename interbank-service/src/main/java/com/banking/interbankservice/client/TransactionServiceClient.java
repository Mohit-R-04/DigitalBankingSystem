package com.banking.interbankservice.client;

import com.banking.interbankservice.dto.InboundCreditRecordRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service", url = "${transaction.service.url}")
public interface TransactionServiceClient {

    @PostMapping("/api/v1/transactions/inbound-credit")
    String recordInboundCredit(@RequestBody InboundCreditRecordRequest request);
}
