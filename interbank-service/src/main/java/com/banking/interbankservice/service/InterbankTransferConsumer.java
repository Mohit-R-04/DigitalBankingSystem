package com.banking.interbankservice.service;

import com.banking.interbankservice.dto.OutboundTransferRequest;
import com.banking.interbankservice.dto.OutboundTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Consumes outbound transfers cleared by the Transaction Service's fraud
 * checks (published on interbank.transfer.requested) and executes the rail
 * leg: mint the UTR and reply on interbank.transfer.completed so the
 * Transaction Service can mark the transfer COMPLETED (or compensate when
 * the rail rejects it).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterbankTransferConsumer {

    private static final String INTERBANK_TRANSFER_COMPLETED_TOPIC = "interbank.transfer.completed";

    private final OutboundTransferService outboundTransferService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "interbank.transfer.requested")
    public void consumeTransferRequested(@Payload Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");

            log.info("Interbank transfer requested - transaction: {} rail: {}",
                    transactionId, payload.get("rail"));

            OutboundTransferRequest request = new OutboundTransferRequest(
                    (String) payload.get("senderAccountNumber"),
                    (String) payload.get("beneficiaryAccountNumber"),
                    (String) payload.get("beneficiaryBank"),
                    (String) payload.get("beneficiaryIfsc"),
                    new BigDecimal(String.valueOf(payload.get("amount"))),
                    (String) payload.get("rail"),
                    (String) payload.get("description"));

            OutboundTransferResponse response = outboundTransferService.send(request);

            Map<String, Object> completed = new HashMap<>();
            completed.put("transactionId", transactionId);
            completed.put("utr", response.getUtr());
            completed.put("status", response.getStatus().name());
            completed.put("failureReason", response.getFailureReason());
            completed.put("amount", response.getAmount());
            completed.put("rail", response.getRail());

            kafkaTemplate.send(INTERBANK_TRANSFER_COMPLETED_TOPIC,
                    transactionId, completed);
            log.info("interbank.transfer.completed published - transaction: {} status: {} UTR: {}",
                    transactionId, response.getStatus(), response.getUtr());
        } catch (Exception e) {
            log.error("Error processing interbank transfer request: {}",
                    e.getMessage(), e);
        }
    }
}
