package com.banking.interbankservice.service;

import com.banking.interbankservice.dto.OutboundTransferRequest;
import com.banking.interbankservice.dto.OutboundTransferResponse;
import com.banking.interbankservice.model.OutboundTransferStatus;
import com.banking.interbankservice.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundTransferService {

    private static final Set<String> SUPPORTED_RAILS = Set.of("UPI", "IMPS", "NEFT");
    private static final String OUTBOUND_TRANSFER_SENT_TOPIC = "outbound.transfer.sent";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Simulates the interbank switch (NPCI / RBI) routing an outbound
     * payment message to the beneficiary's bank. The sender was already
     * debited and the bank-side ledger entry recorded by Transaction
     * Service before this call - this is the rail leg only: mint the UTR,
     * settle, and notify. The rail keeps no records of its own.
     */
    public OutboundTransferResponse send(OutboundTransferRequest request) {
        String rail = request.getRail().toUpperCase();
        log.info("Outbound transfer received - sender: {} beneficiary: {} amount: {} rail: {}",
                request.getSenderAccountNumber(),
                request.getBeneficiaryAccountNumber(),
                request.getAmount(),
                request.getRail());

        if (!SUPPORTED_RAILS.contains(rail)) {
            return mapToResponse(null, request, OutboundTransferStatus.FAILED,
                    "Unsupported rail: " + request.getRail());
        }

        String utr = UtrGenerator.generate(rail);
        publishTransferSent(request, utr);
        return mapToResponse(utr, request, OutboundTransferStatus.COMPLETED, null);
    }

    private void publishTransferSent(OutboundTransferRequest request, String utr) {
        Map<String, Object> event = new HashMap<>();
        event.put("utr", utr);
        event.put("senderAccountNumber", request.getSenderAccountNumber());
        event.put("beneficiaryAccountNumber", request.getBeneficiaryAccountNumber());
        event.put("beneficiaryBank", request.getBeneficiaryBank());
        event.put("amount", request.getAmount());
        event.put("rail", request.getRail().toUpperCase());

        kafkaTemplate.send(OUTBOUND_TRANSFER_SENT_TOPIC, utr, event);
        log.info("outbound.transfer.sent published - UTR: {}", utr);
    }

    private OutboundTransferResponse mapToResponse(String utr, OutboundTransferRequest request,
                                                   OutboundTransferStatus status, String failureReason) {
        OutboundTransferResponse response = new OutboundTransferResponse();
        response.setTransferId(UUID.randomUUID().toString());
        response.setUtr(utr);
        response.setSenderAccountNumber(request.getSenderAccountNumber());
        response.setBeneficiaryAccountNumber(request.getBeneficiaryAccountNumber());
        response.setBeneficiaryBank(request.getBeneficiaryBank());
        response.setAmount(request.getAmount());
        response.setCurrency("INR");
        response.setRail(request.getRail().toUpperCase());
        response.setStatus(status);
        response.setFailureReason(failureReason);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
