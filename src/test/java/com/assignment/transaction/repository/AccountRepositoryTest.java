package com.assignment.transaction.repository;

import com.assignment.transaction.entity.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AccountRepository}.
 *
 * <h3>Why @DataJpaTest?</h3>
 * <p>Loads only the JPA slice of the Spring context — entities, repositories,
 * and an embedded H2 database. Does NOT load controllers, services, or async
 * config. This makes tests fast (~1-2 seconds) and focused on persistence.</p>
 *
 * <h3>Test Data</h3>
 * <p>Relies on {@code data.sql} which seeds 10 accounts on startup.
 * Each test uses these pre-loaded accounts.</p>
 */
@DataJpaTest
@DisplayName("AccountRepository Tests")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Should find account by ID")
    void shouldFindAccountById() {
        // Arrange — Account 1 is seeded by data.sql (Alice Johnson, 10000.00, USD)

        // Act
        Optional<Account> account = accountRepository.findById(1L);

        // Assert
        assertThat(account).isPresent();
        assertThat(account.get().getOwnerName()).isEqualTo("Alice Johnson");
        assertThat(account.get().getCurrency()).isEqualTo("USD");
        assertThat(account.get().getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Should return empty for non-existent account")
    void shouldReturnEmptyForNonExistentAccount() {
        // Act
        Optional<Account> account = accountRepository.findById(999L);

        // Assert
        assertThat(account).isEmpty();
    }

    @Test
    @DisplayName("Should find account with pessimistic lock")
    void shouldFindAccountWithPessimisticLock() {
        // Act — uses @Lock(PESSIMISTIC_WRITE), must be in a transaction
        // @DataJpaTest wraps each test in a transaction automatically
        Optional<Account> account = accountRepository.findByIdWithLock(2L);

        // Assert
        assertThat(account).isPresent();
        assertThat(account.get().getOwnerName()).isEqualTo("Bob Smith");
        assertThat(account.get().getBalance()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("Should update account balance")
    void shouldUpdateAccountBalance() {
        // Arrange
        Account account = accountRepository.findById(1L).orElseThrow();
        BigDecimal originalBalance = account.getBalance();
        BigDecimal debitAmount = new BigDecimal("500.00");

        // Act
        account.setBalance(originalBalance.subtract(debitAmount));
        accountRepository.save(account);

        // Assert
        Account updated = accountRepository.findById(1L).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("9500.00"));
    }

    @Test
    @DisplayName("Should save new account")
    void shouldSaveNewAccount() {
        // Arrange
        Account newAccount = Account.builder()
                .ownerName("Test User")
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .build();

        // Act
        Account saved = accountRepository.save(newAccount);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOwnerName()).isEqualTo("Test User");

        // Verify it's retrievable
        Optional<Account> retrieved = accountRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
    }

    @Test
    @DisplayName("Should load all seed accounts from data.sql")
    void shouldLoadAllSeedAccounts() {
        // Act
        long count = accountRepository.count();

        // Assert — data.sql inserts 10 accounts
        assertThat(count).isEqualTo(10);
    }
}
