package com.banking.interbankservice.service;

import com.banking.interbankservice.client.AccountServiceClient;
import com.banking.interbankservice.client.TransactionServiceClient;
import com.banking.interbankservice.dto.InboundCreditRecordRequest;
import com.banking.interbankservice.dto.InboundCreditRequest;
import com.banking.interbankservice.dto.InboundCreditResponse;
import com.banking.interbankservice.model.InboundCreditStatus;
import com.banking.interbankservice.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboundCreditService {

    private static final String INBOUND_CREDIT_RECEIVED_TOPIC = "inbound.credit.received";
    private static final String INBOUND_CREDIT_FAILED_TOPIC = "inbound.credit.failed";

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Simulates the interbank switch (NPCI / RBI / SWIFT) delivering an
     * inbound credit message to our bank. The sender's bank has already
     * debited the sender; we credit the beneficiary account and ask
     * Transaction Service to record the completed credit in the bank's
     * own transactions ledger. The rail itself keeps no records - it
     * only mints the UTR.
     */
    public InboundCreditResponse receive(InboundCreditRequest request) {
        String utr = UtrGenerator.generate(request.getRail().name());
        log.info("Inbound credit received - account: {} amount: {} rail: {}",
                request.getAccountNumber(), request.getAmount(), request.getRail());

        try {
            // 1. Credit the beneficiary account (the money arrives)
            accountServiceClient.creditBalance(
                    request.getAccountNumber(), request.getAmount());

            // 2. Record the completed credit in the bank's transactions ledger
            transactionServiceClient.recordInboundCredit(
                    new InboundCreditRecordRequest(
                            request.getAccountNumber(), request.getAmount(),
                            request.getRail().name(), utr,
                            request.getSenderBank(), request.getSenderName()));

            publishReceived(request, utr);
            return mapToResponse(utr, request, InboundCreditStatus.COMPLETED, null);

        } catch (Exception e) {
            log.error("Inbound credit failed for account: {} - {}",
                    request.getAccountNumber(), e.getMessage());
            publishFailed(request, utr, e.getMessage());
            return mapToResponse(utr, request, InboundCreditStatus.FAILED, e.getMessage());
        }
    }

    private void publishReceived(InboundCreditRequest request, String utr) {
        Map<String, Object> event = new HashMap<>();
        event.put("utr", utr);
        event.put("accountNumber", request.getAccountNumber());
        event.put("amount", request.getAmount());
        event.put("rail", request.getRail().name());
        event.put("senderBank", request.getSenderBank());
        event.put("senderName", request.getSenderName());

        kafkaTemplate.send(INBOUND_CREDIT_RECEIVED_TOPIC, utr, event);
        log.info("inbound.credit.received published - UTR: {}", utr);
    }

    private void publishFailed(InboundCreditRequest request, String utr, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("utr", utr);
        event.put("accountNumber", request.getAccountNumber());
        event.put("amount", request.getAmount());
        event.put("rail", request.getRail().name());
        event.put("reason", reason);

        kafkaTemplate.send(INBOUND_CREDIT_FAILED_TOPIC, utr, event);
        log.warn("inbound.credit.failed published - UTR: {}", utr);
    }

    private InboundCreditResponse mapToResponse(String utr, InboundCreditRequest request,
                                                InboundCreditStatus status, String failureReason) {
        InboundCreditResponse response = new InboundCreditResponse();
        response.setCreditId(UUID.randomUUID().toString());
        response.setUtr(utr);
        response.setAccountNumber(request.getAccountNumber());
        response.setAmount(request.getAmount());
        response.setCurrency("INR");
        response.setRail(request.getRail());
        response.setSenderBank(request.getSenderBank());
        response.setSenderName(request.getSenderName());
        response.setStatus(status);
        response.setFailureReason(failureReason);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
