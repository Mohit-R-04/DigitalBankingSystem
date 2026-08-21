package com.banking.interbankservice.repository;

import com.banking.interbankservice.model.InboundCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundCreditRepository extends JpaRepository<InboundCredit, String> {
    List<InboundCredit> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
