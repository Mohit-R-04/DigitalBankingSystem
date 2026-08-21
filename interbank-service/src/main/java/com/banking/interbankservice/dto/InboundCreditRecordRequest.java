package com.banking.interbankservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload sent to Transaction Service so the bank records a completed
 * external credit in its own transactions ledger. The rail itself keeps
 * no records - it only mints the UTR.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboundCreditRecordRequest {

    private String accountNumber;
    private BigDecimal amount;
    private String rail;
    private String utr;
    private String senderBank;
    private String senderName;
}
