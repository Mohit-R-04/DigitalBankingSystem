package com.banking.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /**
     * Transaction completed — debit sender, credit receiver.
     */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {
        try {
            // External transfers are covered by the outbound.transfer.sent
            // alert from the Interbank Service - skip the internal alerts.
            if (payload.get("rail") != null) {
                return;
            }

            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String receiverAccount = (String) payload
                    .get("receiverAccountNumber");
            String amount = payload.get("amount").toString();

            sendAlert(senderAccount, "DEBIT ALERT",
                    String.format("₹%s debited from account %s",
                            amount, senderAccount));

            sendAlert(receiverAccount, "CREDIT ALERT",
                    String.format("₹%s credited to account %s",
                            amount, receiverAccount));

        } catch (Exception e) {
            log.error("Error sending transaction notification: {}",
                    e.getMessage());
        }
    }

    /**
     * Outbound transfer sent to another bank via UPI / IMPS / NEFT.
     */
    @KafkaListener(topics = "outbound.transfer.sent")
    public void consumeOutboundTransferSent(
            @Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload.get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String rail = String.valueOf(payload.get("rail"));
            String utr = String.valueOf(payload.get("utr"));
            String beneficiaryBank = String.valueOf(payload.get("beneficiaryBank"));

            sendAlert(senderAccount, "OUTBOUND TRANSFER",
                    String.format(
                            "₹%s sent to %s via %s. UTR: %s",
                            amount,
                            "null".equals(beneficiaryBank) ? "beneficiary bank" : beneficiaryBank,
                            rail, utr));

        } catch (Exception e) {
            log.error("Error sending outbound transfer notification: {}",
                    e.getMessage());
        }
    }


    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber,
                    "🚨 ACCOUNT BLOCKED",
                    String.format(
                            "Your account %s has been blocked. " +
                                    "Reason: %s. " +
                                    "Please contact your bank immediately.",
                            accountNumber, reason));

        } catch (Exception e) {
            log.error("Error sending fraud alert: {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber,
                    "🔐 TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity detected on your account. " +
                                    "Reason: %s. " +
                                    "A transaction of ₹%s is pending verification. " +
                                    "Your OTP is: %s. Valid for 5 minutes. " +
                                    "If this wasn't you — ignore this message. " +
                                    "Transaction will be cancelled and amount refunded automatically.",
                            reason, amount, otp, transactionId, otp));

        } catch (Exception e) {
            log.error("Error sending OTP notification: {}",
                    e.getMessage());
        }
    }


    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(senderAccount, "💰 REFUND PROCESSED",
                    String.format(
                            "Your transaction of ₹%s was cancelled. " +
                                    "Reason: %s. " +
                                    "₹%s has been refunded to account %s.",
                            amount, reason, amount, senderAccount));

        } catch (Exception e) {
            log.error("Error sending refund notification: {}",
                    e.getMessage());
        }
    }

    /**
     * Inbound credit received from another bank via UPI / IMPS / NEFT.
     */
    @KafkaListener(topics = "inbound.credit.received")
    public void consumeInboundCreditReceived(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();
            String rail = String.valueOf(payload.get("rail"));
            String utr = String.valueOf(payload.get("utr"));

            sendAlert(accountNumber, "INBOUND CREDIT",
                    String.format(
                            "₹%s credited to account %s via %s. " +
                                    "UTR: %s",
                            amount, accountNumber, rail, utr));

        } catch (Exception e) {
            log.error("Error sending inbound credit notification: {}",
                    e.getMessage());
        }
    }

    /**
     * Inbound credit could not be posted.
     */
    @KafkaListener(topics = "inbound.credit.failed")
    public void consumeInboundCreditFailed(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();
            String reason = String.valueOf(payload.get("reason"));

            sendAlert(accountNumber, "INBOUND CREDIT FAILED",
                    String.format(
                            "A credit of ₹%s to account %s could not be posted. " +
                                    "Reason: %s",
                            amount, accountNumber, reason));

        } catch (Exception e) {
            log.error("Error sending inbound credit failure notification: {}",
                    e.getMessage());
        }
    }

    private void sendAlert(String accountNumber,
                           String subject,
                           String message) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("NOTIFICATION SENT");
        log.info("Account : {}", accountNumber);
        log.info("Subject : {}", subject);
        log.info("Message : {}", message);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
