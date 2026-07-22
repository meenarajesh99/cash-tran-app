package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferStatusRepository
        extends JpaRepository<TransferStatus, Long> {

    Optional<TransferStatus> findByTransferStatusDesc(String transferStatusDesc);

}