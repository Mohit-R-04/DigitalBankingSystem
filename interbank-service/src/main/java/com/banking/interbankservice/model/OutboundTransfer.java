package com.banking.interbankservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_transfers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboundTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String utr;

    @Column(nullable = false)
    private String senderAccountNumber;

    @Column(nullable = false)
    private String beneficiaryAccountNumber;

    private String beneficiaryBank;
    private String beneficiaryIfsc;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String rail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboundTransferStatus status;

    private String failureReason;
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
