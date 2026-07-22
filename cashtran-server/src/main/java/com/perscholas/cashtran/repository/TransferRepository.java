package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

  @Query(
      """
        SELECT t
        FROM Transfer t
        WHERE t.accountFrom.accountId = :accountId
           OR t.accountTo.accountId = :accountId
    """)
  List<Transfer> findTransfersByAccount(@Param("accountId") Long accountId);

  @Query(
      """
        SELECT t
        FROM Transfer t
        WHERE t.transferStatus.transferStatusDesc = :status
          AND t.accountTo.accountId = :accountId
    """)
  List<Transfer> findPendingTransfers(
      @Param("status") String status, @Param("accountId") Long accountId);

  @Query(
      """
        SELECT t
        FROM Transfer t
        WHERE t.transferStatus.transferStatusDesc = :status
          AND (
              t.accountFrom.accountId = :accountId
              OR
              t.accountTo.accountId = :accountId
          )
    """)
  List<Transfer> findTransfersByStatusAndAccount(
      @Param("status") String status, @Param("accountId") Long accountId);
}
