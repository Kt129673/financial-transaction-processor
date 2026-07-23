package com.assignment.transaction.repository;

import com.assignment.transaction.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Account} entities.
 *
 * <h3>Locking Strategy</h3>
 * <p>This repository provides two ways to fetch an account:</p>
 * <ol>
 *   <li>{@link #findById(Long)} (inherited) — Standard read, no lock. Use for
 *       read-only operations like balance checks and reporting.</li>
 *   <li>{@link #findByIdWithLock(Long)} — Acquires a {@code PESSIMISTIC_WRITE}
 *       (SELECT ... FOR UPDATE) lock. Use exclusively during money transfers.</li>
 * </ol>
 *
 * <h3>Why PESSIMISTIC_WRITE instead of PESSIMISTIC_READ?</h3>
 * <ul>
 *   <li>{@code PESSIMISTIC_READ} (SELECT ... FOR SHARE) allows concurrent reads
 *       but blocks writes. Two threads could both read balance=1000, both decide
 *       there's enough funds, and both debit — causing an overdraft.</li>
 *   <li>{@code PESSIMISTIC_WRITE} (SELECT ... FOR UPDATE) blocks both reads AND
 *       writes. The second thread must wait until the first commits, ensuring it
 *       sees the updated balance. This is the ONLY safe option for financial
 *       debit/credit operations.</li>
 * </ul>
 *
 * <h3>Why not just use @Version (optimistic locking)?</h3>
 * <p>Optimistic locking detects conflicts after the fact and throws
 * {@code OptimisticLockException}. For a batch of 10,000 transactions
 * hitting the same account, this would cause massive retry storms.
 * Pessimistic locking serializes access upfront — no retries needed.
 * We keep {@code @Version} on the entity as a defense-in-depth layer.</p>
 *
 * <h3>Why a custom method instead of overriding findById?</h3>
 * <p>If we override {@code findById} with {@code @Lock}, EVERY read would
 * acquire a row-level lock — even simple balance lookups in reports.
 * A separate method gives callers an explicit choice: lock or no lock.</p>
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Fetches an account with a database-level exclusive row lock.
     *
     * <p>Translates to: {@code SELECT * FROM account WHERE id = ? FOR UPDATE}</p>
     *
     * <p><b>Must be called within a {@code @Transactional} method</b> — the lock
     * is held until the transaction commits or rolls back. Calling this outside
     * a transaction will throw an exception.</p>
     *
     * @param id the account ID to lock and retrieve
     * @return the locked Account, or empty if the ID doesn't exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
