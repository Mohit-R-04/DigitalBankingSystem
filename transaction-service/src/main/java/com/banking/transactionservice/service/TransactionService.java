package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.client.InterbankServiceClient;
import com.banking.transactionservice.dto.InboundCreditRecordRequest;
import com.banking.transactionservice.dto.OutboundTransferRequest;
import com.banking.transactionservice.dto.OutboundTransferResponse;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.model.IdempotencyRecord;
import com.banking.transactionservice.model.IdempotencyStatus;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.model.TransactionType;
import com.banking.transactionservice.repository.IdempotencyRecordRepository;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final InterbankServiceClient interbankServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";
    private static final String TRANSACTION_CREDIT_REQUESTED_TOPIC = "transaction.credit.requested";

    @Transactional
    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {

        log.info("SAGA START - Transfer: {} -> {} amount: {} idempotencyKey: {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount(),
                idempotencyKey);

        IdempotencyRecord lock = acquireIdempotencyLock(idempotencyKey, request);

        // Duplicate request - the money was already moved. Return the original
        // transaction instead of executing the transfer a second time.
        if (lock != null && lock.getTransactionId() != null) {
            log.info("Duplicate idempotency key {} - returning existing transaction {}",
                    idempotencyKey, lock.getTransactionId());
            return mapToResponse(transactionRepository
                    .findById(lock.getTransactionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Transaction not found: " + lock.getTransactionId())));
        }

        // SAGA STEP 1: Deduct from sender
        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),
                request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        // External transfers (rail present) are recorded as PAYMENT
        transaction.setType(request.getRail() != null && !request.getRail().isBlank()
                ? TransactionType.PAYMENT
                : TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setBeneficiaryBank(request.getBeneficiaryBank());
        transaction.setBeneficiaryIfsc(request.getBeneficiaryIfsc());
        transaction.setRail(request.getRail());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING: {}", savedTransaction.getId());

        // Link the idempotency record to the executed transaction
        if (lock != null) {
            lock.setTransactionId(savedTransaction.getId());
            lock.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyRecordRepository.save(lock);
        }

        // SAGA STEP - 2: Publish for fraud check
        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", savedTransaction.getId());
        event.put("senderAccountNumber", savedTransaction.getSenderAccountNumber());
        event.put("receiverAccountNumber", savedTransaction.getReceiverAccountNumber());
        event.put("amount", savedTransaction.getAmount());
        event.put("description", savedTransaction.getDescription());

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,
                savedTransaction.getId(), event);
        log.info("SAGA STEP 2 - TransactionInitiatedEvent published: {}",
                savedTransaction.getId());

        return mapToResponse(savedTransaction);
    }

    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId)));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

        // Return both sides of the account: money sent (sender) and money
        // received (receiver), merged into one time-ordered statement.
        List<Transaction> sent = transactionRepository
                .findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber);
        List<Transaction> received = transactionRepository
                .findByReceiverAccountNumberOrderByCreatedAtDesc(accountNumber);

        return Stream.concat(sent.stream(), received.stream())
                .sorted(Comparator.comparing(
                        Transaction::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Record an external credit received via a payment rail. The Interbank
     * Service (simulated switch) has already credited the beneficiary
     * account; this is the bank's ledger entry for that completed credit -
     * every credit/debit that completes is recorded in the transactions
     * table, internal or external.
     */
    public TransactionResponse recordInboundCredit(InboundCreditRecordRequest request) {
        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(
                request.getSenderName() != null ? request.getSenderName()
                : request.getSenderBank() != null ? request.getSenderBank()
                : "EXTERNAL");
        transaction.setReceiverAccountNumber(request.getAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.PAYMENT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReferenceNumber(request.getUtr());
        transaction.setRail(request.getRail());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setDescription(request.getDescription() != null
                ? request.getDescription()
                : "External credit via " + request.getRail()
                + (request.getSenderBank() != null
                        ? " from " + request.getSenderBank() : ""));

        Transaction saved = transactionRepository.save(transaction);
        log.info("External credit recorded in ledger - account: {} amount: {} UTR: {}",
                request.getAccountNumber(), request.getAmount(), request.getUtr());
        return mapToResponse(saved);
    }

    public TransactionResponse verifyOTP(String transactionID, String otp) {
        log.info("OTP verification for the transaction: {}", transactionID);

        Transaction transaction = transactionRepository.findById(transactionID)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found " + transactionID));

        String otpKey = "verification:otp" + transactionID;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            // OTP EXPIRED
            log.warn("OTP expired for transaction: {}", transactionID);
            compensateTransaction(transaction, "OTP expired - transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }

        if (!storedOtp.equals(otp)) {
            // BLOCK ACCOUNT AND REFUND
            log.warn("Wrong OTP - blocking account and refunding: {}", transactionID);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(transaction,
                    "Wrong OTP entered - transaction cancelled, "
                            + "account blocked for security");

            return mapToResponse(transaction);
        }

        // OTP correct - request receiver credit
        log.info("OTP verified - requesting receiver credit: {}", transactionID);
        redisTemplate.delete(otpKey);
        requestReceiverCredit(transaction);
        return mapToResponse(transaction);
    }

    private void compensateTransaction(Transaction transaction, String reason) {
        log.warn("SAGA COMPENSATION - refunding: {} amount: {}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        // CREDIT MONEY BACK TO SENDER SYNCHRONOUSLY
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason
                + " - SAGA Compensation executed, amount refunded at " + LocalDateTime.now());

        transactionRepository.save(transaction);

        // PUBLISH refund event - Notification service will alert user
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", reason);

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC,
                transaction.getId(), refundEvent);

        log.info("SAGA COMPENSATION COMPLETE - {} refunded to {}",
                transaction.getAmount(), transaction.getSenderAccountNumber());
    }

    private void blockAccountAndCompensate(Transaction transaction, String reason) {

        // Publish fraud.detected -> Account Service will block account
        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId", transaction.getId());
        fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
        fraudEvent.put("reason", reason);

        kafkaTemplate.send(FRAUD_DETECTED_TOPIC,
                transaction.getSenderAccountNumber(), fraudEvent);
        log.warn("fraud.detected published - account: {} will be blocked, Kindly contact to the bank",
                transaction.getSenderAccountNumber());

        // SAGA COMPENSATION - refund Sender
        compensateTransaction(transaction, reason);
    }

    private void requestReceiverCredit(Transaction transaction) {

        // External transfer (rail present) - the beneficiary is at another
        // bank, so the money goes through the interbank rail, not an
        // internal account credit.
        if (transaction.getRail() != null && !transaction.getRail().isBlank()) {
            sendExternalTransfer(transaction);
            return;
        }

        log.info("SAGA STEP 3 - requesting receiver credit for transaction: {}",
                transaction.getId());

        // Status stays PROCESSING / PENDING_VERIFICATION until the credit is acked
        Map<String, Object> creditRequest = new HashMap<>();
        creditRequest.put("transactionId", transaction.getId());
        creditRequest.put("senderAccountNumber", transaction.getSenderAccountNumber());
        creditRequest.put("receiverAccountNumber", transaction.getReceiverAccountNumber());
        creditRequest.put("amount", transaction.getAmount());
        creditRequest.put("description", transaction.getDescription());

        kafkaTemplate.send(TRANSACTION_CREDIT_REQUESTED_TOPIC,
                transaction.getId(), creditRequest);
        log.info("transaction.credit.requested published - waiting for credit ack: {}",
                transaction.getId());
    }

    /**
     * SAGA STEP 3 (external): hand the payment to the interbank rail. The
     * sender was already debited; the rail routes the message to the
     * beneficiary's bank and returns a UTR. Only then is the transaction
     * marked COMPLETED. If the rail rejects the payment, the saga
     * compensates by refunding the sender.
     */
    private void sendExternalTransfer(Transaction transaction) {
        log.info("SAGA STEP 3 - external transfer via {} for transaction: {}",
                transaction.getRail(), transaction.getId());

        try {
            OutboundTransferRequest request = new OutboundTransferRequest(
                    transaction.getSenderAccountNumber(),
                    transaction.getReceiverAccountNumber(),
                    transaction.getBeneficiaryBank(),
                    transaction.getBeneficiaryIfsc(),
                    transaction.getAmount(),
                    transaction.getRail(),
                    transaction.getDescription());

            OutboundTransferResponse response =
                    interbankServiceClient.sendOutboundTransfer(request);

            if ("COMPLETED".equalsIgnoreCase(response.getStatus())) {
                transaction.setReferenceNumber(response.getUtr());
                transactionRepository.save(transaction);
                log.info("External transfer sent - UTR: {} transaction: {}",
                        response.getUtr(), transaction.getId());
                markCompleted(transaction.getId());
            } else {
                compensateTransaction(transaction,
                        "External transfer failed - " + response.getFailureReason());
            }
        } catch (Exception e) {
            log.error("External transfer failed for transaction: {} - compensating",
                    transaction.getId(), e);
            compensateTransaction(transaction,
                    "External transfer failed - " + e.getMessage());
        }
    }

    public void markCompleted(String transactionID) {
        Transaction transaction = transactionRepository.findById(transactionID)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found " + transactionID));

        if (transaction.getStatus() != TransactionStatus.PROCESSING
                && transaction.getStatus() != TransactionStatus.PENDING_VERIFICATION) {
            log.warn("Transaction {} not awaiting credit - skipping completion",
                    transactionID);
            return;
        }

        // Receiver credit was acknowledged - only now mark COMPLETED
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        Map<String, Object> completedEvent = new HashMap<>();
        completedEvent.put("transactionId", transaction.getId());
        completedEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        completedEvent.put("receiverAccountNumber", transaction.getReceiverAccountNumber());
        completedEvent.put("amount", transaction.getAmount());
        completedEvent.put("description", transaction.getDescription());
        // External transfers carry the rail and UTR; Notification Service uses
        // these to skip the internal credit alert (the outbound.transfer.sent
        // event from the interbank service covers the external alert instead).
        completedEvent.put("rail", transaction.getRail());
        completedEvent.put("utr", transaction.getReferenceNumber());

        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC,
                transaction.getId(), completedEvent);

        log.info("SAGA COMPLETE - Transaction {} completed after receiver credit",
                transaction.getId());
    }

    public void handleCreditFailure(String transactionID, String reason) {
        Transaction transaction = transactionRepository.findById(transactionID)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found " + transactionID));

        if (transaction.getStatus() != TransactionStatus.PROCESSING
                && transaction.getStatus() != TransactionStatus.PENDING_VERIFICATION) {
            log.warn("Transaction {} not awaiting credit - skipping compensation",
                    transactionID);
            return;
        }

        log.warn("Receiver credit failed for transaction: {} - compensating",
                transactionID);
        compensateTransaction(transaction, "Receiver credit failed - " + reason);
    }

    public void processCleanResult(String transactionID) {

        Transaction transaction = transactionRepository.findById(transactionID)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found " + transactionID));

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            log.warn("Transaction {} not PROCESSING - skipping", transactionID);
            return;
        }

        requestReceiverCredit(transaction);
    }

    /**
     * Insert-lock pattern: the idempotency key is inserted first (unique
     * constraint in MySQL). A concurrent duplicate blocks on the insert and
     * then fails with DataIntegrityViolationException, after which the
     * existing record is loaded and the original result returned - the money
     * is never moved twice.
     */
    private IdempotencyRecord acquireIdempotencyLock(String idempotencyKey,
                                                     TransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        Optional<IdempotencyRecord> existing =
                idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (record.getCreatedAt().plusHours(IDEMPOTENCY_TTL_HOURS)
                    .isBefore(LocalDateTime.now())) {
                // TTL expired - the key may be reused
                idempotencyRecordRepository.delete(record);
            } else {
                if (!record.getRequestHash().equals(hashOf(request))) {
                    throw new RuntimeException(
                            "Idempotency key already used for a different request");
                }
                return record;
            }
        }

        try {
            return idempotencyRecordRepository.saveAndFlush(
                    new IdempotencyRecord(idempotencyKey, null,
                            hashOf(request), IdempotencyStatus.IN_PROGRESS));
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate won the race - serve the existing result
            return idempotencyRecordRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new RuntimeException(
                            "Idempotency record not found for key: " + idempotencyKey));
        }
    }

    private String hashOf(TransferRequest request) {
        return request.getSenderAccountNumber() + "|"
                + request.getReceiverAccountNumber() + "|"
                + request.getAmount() + "|"
                + (request.getDescription() == null ? "" : request.getDescription());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(
                transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(
                transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setFailureReason(transaction.getFailureReason());
        response.setBeneficiaryBank(transaction.getBeneficiaryBank());
        response.setRail(transaction.getRail());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }
}
