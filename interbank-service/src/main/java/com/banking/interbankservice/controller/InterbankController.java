package com.banking.interbankservice.controller;

import com.banking.interbankservice.dto.InboundCreditRequest;
import com.banking.interbankservice.dto.InboundCreditResponse;
import com.banking.interbankservice.dto.OutboundTransferRequest;
import com.banking.interbankservice.dto.OutboundTransferResponse;
import com.banking.interbankservice.service.InboundCreditService;
import com.banking.interbankservice.service.OutboundTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interbank")
@RequiredArgsConstructor
@Slf4j
public class InterbankController {

    private final InboundCreditService inboundCreditService;
    private final OutboundTransferService outboundTransferService;

    // Simulates the interbank switch delivering an inbound credit message
    @PostMapping("/inbound-credit")
    public ResponseEntity<InboundCreditResponse> receiveInboundCredit(
            @Valid @RequestBody InboundCreditRequest request) {
        log.info("Inbound credit request received for account: {}",
                request.getAccountNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inboundCreditService.receive(request));
    }

    // Inbound credit history for an account (statement-style)
    @GetMapping("/credits/{accountNumber}")
    public ResponseEntity<List<InboundCreditResponse>> getCredits(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(
                inboundCreditService.getCredits(accountNumber));
    }

    // Simulates the interbank switch routing an outbound payment to another bank
    @PostMapping("/outbound-transfer")
    public ResponseEntity<OutboundTransferResponse> sendOutboundTransfer(
            @Valid @RequestBody OutboundTransferRequest request) {
        log.info("Outbound transfer request received from sender: {}",
                request.getSenderAccountNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outboundTransferService.send(request));
    }

    // Outbound transfer history for an account
    @GetMapping("/outbound/{accountNumber}")
    public ResponseEntity<List<OutboundTransferResponse>> getOutboundTransfers(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(
                outboundTransferService.getTransfers(accountNumber));
    }
}
