package com.banking.transactionservice.client;

import com.banking.transactionservice.dto.OutboundTransferRequest;
import com.banking.transactionservice.dto.OutboundTransferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "interbank-service", url = "${interbank.service.url}")
public interface InterbankServiceClient {

    @PostMapping("/api/v1/interbank/outbound-transfer")
    OutboundTransferResponse sendOutboundTransfer(OutboundTransferRequest request);
}
