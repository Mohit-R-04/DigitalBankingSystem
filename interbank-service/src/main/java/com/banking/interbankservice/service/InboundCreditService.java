package com.banking.interbankservice.service;

import com.banking.interbankservice.client.AccountServiceClient;
import com.banking.interbankservice.dto.InboundCreditRequest;
import com.banking.interbankservice.dto.InboundCreditResponse;
import com.banking.interbankservice.model.InboundCredit;
import com.banking.interbankservice.model.InboundCreditStatus;
import com.banking.interbankservice.model.Rail;
import com.banking.interbankservice.repository.InboundCreditRepository;
import com.banking.interbankservice.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboundCreditService {

    private final InboundCreditRepository inboundCreditRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INBOUND_CREDIT_RECEIVED_TOPIC = "inbound.credit.received";
    private static final String INBOUND_CREDIT_FAILED_TOPIC = "inbound.credit.failed";

    /**
     * Simulates the interbank switch (NPCI / RBI / SWIFT) delivering an
     * inbound credit message to our bank. The sender's bank has already
     * debited the sender; we just credit the beneficiary account and keep
     * the UTR for reconciliation.
     */
    @Transactional
    public InboundCreditResponse receive(InboundCreditRequest request) {
        log.info("Inbound credit received - account: {} amount: {} rail: {}",
                request.getAccountNumber(), request.getAmount(), request.getRail());

        String utr = UtrGenerator.generate(request.getRail().name());

        InboundCredit credit = new InboundCredit();
        credit.setUtr(utr);
        credit.setAccountNumber(request.getAccountNumber());
        credit.setAmount(request.getAmount());
        credit.setCurrency("INR");
        credit.setRail(request.getRail());
        credit.setSenderBank(request.getSenderBank());
        credit.setSenderName(request.getSenderName());
        credit.setStatus(InboundCreditStatus.RECEIVED);

        InboundCredit saved = inboundCreditRepository.save(credit);

        try {
            accountServiceClient.creditBalance(
                    request.getAccountNumber(),
                    request.getAmount());
            log.info("Account {} credited with {} (UTR: {})",
                    request.getAccountNumber(), request.getAmount(), utr);

            saved.setStatus(InboundCreditStatus.COMPLETED);
            inboundCreditRepository.save(saved);

            publishReceived(saved);
            return mapToResponse(saved);

        } catch (Exception e) {
            log.error("Inbound credit failed for account: {} - {}",
                    request.getAccountNumber(), e.getMessage());

            saved.setStatus(InboundCreditStatus.FAILED);
            saved.setFailureReason(e.getMessage());
            inboundCreditRepository.save(saved);

            publishFailed(saved);
            return mapToResponse(saved);
        }
    }

    public List<InboundCreditResponse> getCredits(String accountNumber) {
        return inboundCreditRepository
                .findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void publishReceived(InboundCredit credit) {
        Map<String, Object> event = new HashMap<>();
        event.put("creditId", credit.getId());
        event.put("utr", credit.getUtr());
        event.put("accountNumber", credit.getAccountNumber());
        event.put("amount", credit.getAmount());
        event.put("rail", credit.getRail().name());
        event.put("senderBank", credit.getSenderBank());
        event.put("senderName", credit.getSenderName());

        kafkaTemplate.send(INBOUND_CREDIT_RECEIVED_TOPIC, credit.getId(), event);
        log.info("inbound.credit.received published for credit: {}", credit.getId());
    }

    private void publishFailed(InboundCredit credit) {
        Map<String, Object> event = new HashMap<>();
        event.put("creditId", credit.getId());
        event.put("utr", credit.getUtr());
        event.put("accountNumber", credit.getAccountNumber());
        event.put("amount", credit.getAmount());
        event.put("rail", credit.getRail().name());
        event.put("reason", credit.getFailureReason());

        kafkaTemplate.send(INBOUND_CREDIT_FAILED_TOPIC, credit.getId(), event);
        log.warn("inbound.credit.failed published for credit: {}", credit.getId());
    }

    private InboundCreditResponse mapToResponse(InboundCredit credit) {
        InboundCreditResponse response = new InboundCreditResponse();
        response.setCreditId(credit.getId());
        response.setUtr(credit.getUtr());
        response.setAccountNumber(credit.getAccountNumber());
        response.setAmount(credit.getAmount());
        response.setCurrency(credit.getCurrency());
        response.setRail(credit.getRail());
        response.setSenderBank(credit.getSenderBank());
        response.setSenderName(credit.getSenderName());
        response.setStatus(credit.getStatus());
        response.setFailureReason(credit.getFailureReason());
        response.setCreatedAt(credit.getCreatedAt());
        return response;
    }
}
