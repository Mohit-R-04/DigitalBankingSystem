package com.banking.transactionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private String transactionId;

    @Column(nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public IdempotencyRecord(String idempotencyKey, String transactionId,
                             String requestHash, IdempotencyStatus status) {
        this.idempotencyKey = idempotencyKey;
        this.transactionId = transactionId;
        this.requestHash = requestHash;
        this.status = status;
    }
}
