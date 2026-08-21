package com.banking.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboundTransferRequest {

    private String senderAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryBank;
    private String beneficiaryIfsc;
    private BigDecimal amount;
    private String rail;
    private String description;
}
