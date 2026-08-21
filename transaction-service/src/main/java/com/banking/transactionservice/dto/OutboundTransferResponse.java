package com.banking.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboundTransferResponse {

    private String transferId;
    private String utr;
    private String senderAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryBank;
    private BigDecimal amount;
    private String currency;
    private String rail;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
}
