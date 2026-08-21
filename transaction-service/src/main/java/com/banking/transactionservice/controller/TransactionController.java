package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.InboundCreditRecordRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // Transfer money between accounts
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, idempotencyKey));
    }

    // Get transaction by ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(
                transactionService.getTransaction(transactionId));
    }

    // Get transaction history for account
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(
                transactionService.getTransactionHistory(accountNumber));
    }


    // Record an external credit received via a payment rail. Called by the
    // Interbank Service after the account is credited; every completed
    // credit/debit is a ledger entry in the bank's transactions table.
    @PostMapping("/inbound-credit")
    public ResponseEntity<TransactionResponse> recordInboundCredit(
            @Valid @RequestBody InboundCreditRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.recordInboundCredit(request));
    }

    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyTransaction(
            @PathVariable String transactionId,
            @RequestParam String otp) {
        log.info("OTP verification request — transaction: {}",
                transactionId);
        return ResponseEntity.ok(
                transactionService.verifyOTP(transactionId, otp));
    }
}
