package com.banking.transactionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * External credit received via a payment rail (UPI / IMPS / NEFT).
 * Sent by the Interbank Service after the beneficiary account was
 * credited, so the bank records the completed credit in its own
 * transactions ledger.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboundCreditRecordRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Rail is required (UPI, IMPS or NEFT)")
    private String rail;

    @NotBlank(message = "UTR is required")
    private String utr;

    private String senderBank;
    private String senderName;
    private String description;
}
