package com.banking.interbankservice.repository;

import com.banking.interbankservice.model.OutboundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboundTransferRepository extends JpaRepository<OutboundTransfer, String> {
    List<OutboundTransfer> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
