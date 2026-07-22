package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferTypeRepository
        extends JpaRepository<TransferType, Long> {

    Optional<TransferType> findByTransferTypeDesc(String transferTypeDesc);

}