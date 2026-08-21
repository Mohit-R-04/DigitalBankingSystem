package com.banking.interbankservice.dto;

import com.banking.interbankservice.model.InboundCreditStatus;
import com.banking.interbankservice.model.Rail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboundCreditResponse {

    private String creditId;
    private String utr;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private Rail rail;
    private String senderBank;
    private String senderName;
    private InboundCreditStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
}
