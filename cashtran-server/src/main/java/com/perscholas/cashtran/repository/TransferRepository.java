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
               ORDER BY t.createdAt DESC
    """)
  List<Transfer> findTransfersByAccount(@Param("accountId") Long accountId);

    @Query("""
    SELECT t
    FROM Transfer t
    WHERE t.transferStatus.transferStatusDesc = 'Completed'
    AND (
        t.accountFrom.accountId = :accountId
        OR
        t.accountTo.accountId = :accountId
    )
""")
    List<Transfer> findCompletedTransfersByAccount(
            @Param("accountId") Long accountId
    );

    // Requests waiting for approval by this account (payer)
    @Query("""
    SELECT t
    FROM Transfer t
    WHERE t.transferStatus.transferStatusDesc = :status
    AND t.transferType.transferTypeDesc = 'Request'
    AND t.accountTo.accountId = :accountId
""")
    List<Transfer> findPendingReceivedTransfers(
            @Param("status") String status,
            @Param("accountId") Long accountId
    );


    // Requests created by this account (requester)
    @Query("""
    SELECT t
    FROM Transfer t
    WHERE t.transferStatus.transferStatusDesc = :status
    AND t.transferType.transferTypeDesc = 'Request'
    AND t.accountFrom.accountId = :accountId
""")
    List<Transfer> findPendingSentTransfers(
            @Param("status") String status,
            @Param("accountId") Long accountId
    );

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

  @Query(
      """
        SELECT t
        FROM Transfer t
        WHERE (t.transferStatus.transferStatusDesc = 'Completed' OR t.transferStatus.transferStatusDesc = 'Approved')
          AND (
              t.accountFrom.accountId = :accountId
              OR
              t.accountTo.accountId = :accountId
          )
        ORDER BY t.createdAt DESC
    """)
  List<Transfer> findCompletedOrApprovedTransfersByAccount(@Param("accountId") Long accountId);

}
