package com.banking.interbankservice.service;

import com.banking.interbankservice.dto.OutboundTransferRequest;
import com.banking.interbankservice.dto.OutboundTransferResponse;
import com.banking.interbankservice.model.OutboundTransfer;
import com.banking.interbankservice.model.OutboundTransferStatus;
import com.banking.interbankservice.repository.OutboundTransferRepository;
import com.banking.interbankservice.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundTransferService {

    private static final Set<String> SUPPORTED_RAILS = Set.of("UPI", "IMPS", "NEFT");
    private static final String OUTBOUND_TRANSFER_SENT_TOPIC = "outbound.transfer.sent";

    private final OutboundTransferRepository outboundTransferRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Simulates the interbank switch (NPCI / RBI) delivering an outbound
     * payment message to the beneficiary's bank. The sender was already
     * debited by Transaction Service - this is the rail leg only: the
     * switch routes the message, returns a UTR, and the beneficiary bank
     * credits its customer.
     */
    @Transactional
    public OutboundTransferResponse send(OutboundTransferRequest request) {
        log.info("Outbound transfer received - sender: {} beneficiary: {} amount: {} rail: {}",
                request.getSenderAccountNumber(),
                request.getBeneficiaryAccountNumber(),
                request.getAmount(),
                request.getRail());

        String rail = request.getRail().toUpperCase();

        OutboundTransfer transfer = new OutboundTransfer();
        transfer.setSenderAccountNumber(request.getSenderAccountNumber());
        transfer.setBeneficiaryAccountNumber(request.getBeneficiaryAccountNumber());
        transfer.setBeneficiaryBank(request.getBeneficiaryBank());
        transfer.setBeneficiaryIfsc(request.getBeneficiaryIfsc());
        transfer.setAmount(request.getAmount());
        transfer.setCurrency("INR");
        transfer.setRail(rail);
        transfer.setDescription(request.getDescription());

        if (!SUPPORTED_RAILS.contains(rail)) {
            transfer.setStatus(OutboundTransferStatus.FAILED);
            transfer.setFailureReason("Unsupported rail: " + request.getRail());
            OutboundTransfer failed = outboundTransferRepository.save(transfer);
            return mapToResponse(failed);
        }

        transfer.setUtr(UtrGenerator.generate(rail));
        transfer.setStatus(OutboundTransferStatus.RECEIVED);
        OutboundTransfer saved = outboundTransferRepository.save(transfer);

        // The switch routes the payment and settles it - simulate completion
        saved.setStatus(OutboundTransferStatus.COMPLETED);
        outboundTransferRepository.save(saved);

        publishTransferSent(saved);
        return mapToResponse(saved);
    }

    public List<OutboundTransferResponse> getTransfers(String accountNumber) {
        return outboundTransferRepository
                .findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void publishTransferSent(OutboundTransfer transfer) {
        Map<String, Object> event = new HashMap<>();
        event.put("transferId", transfer.getId());
        event.put("utr", transfer.getUtr());
        event.put("senderAccountNumber", transfer.getSenderAccountNumber());
        event.put("beneficiaryAccountNumber", transfer.getBeneficiaryAccountNumber());
        event.put("beneficiaryBank", transfer.getBeneficiaryBank());
        event.put("amount", transfer.getAmount());
        event.put("rail", transfer.getRail());

        kafkaTemplate.send(OUTBOUND_TRANSFER_SENT_TOPIC,
                transfer.getId(), event);
        log.info("outbound.transfer.sent published for transfer: {}", transfer.getId());
    }

    private OutboundTransferResponse mapToResponse(OutboundTransfer transfer) {
        OutboundTransferResponse response = new OutboundTransferResponse();
        response.setTransferId(transfer.getId());
        response.setUtr(transfer.getUtr());
        response.setSenderAccountNumber(transfer.getSenderAccountNumber());
        response.setBeneficiaryAccountNumber(transfer.getBeneficiaryAccountNumber());
        response.setBeneficiaryBank(transfer.getBeneficiaryBank());
        response.setAmount(transfer.getAmount());
        response.setCurrency(transfer.getCurrency());
        response.setRail(transfer.getRail());
        response.setStatus(transfer.getStatus());
        response.setFailureReason(transfer.getFailureReason());
        response.setCreatedAt(transfer.getCreatedAt());
        return response;
    }
}
