-- =============================================================================
-- SEED DATA: Sample Accounts
-- =============================================================================
-- 10 accounts with varied balances and currencies.
-- These provide a realistic dataset for testing money transfers:
--   - Accounts 1-4: USD (large balances for high-volume testing)
--   - Accounts 5-7: EUR (medium balances)
--   - Accounts 8-10: GBP (mixed balances, including a low-balance account for overdraft testing)
-- =============================================================================

INSERT INTO account (id, owner_name, balance, currency) VALUES (1, 'Alice Johnson', 10000.00, 'USD');
INSERT INTO account (id, owner_name, balance, currency) VALUES (2, 'Bob Smith', 25000.00, 'USD');
INSERT INTO account (id, owner_name, balance, currency) VALUES (3, 'Charlie Brown', 50000.00, 'USD');
INSERT INTO account (id, owner_name, balance, currency) VALUES (4, 'Diana Prince', 100000.00, 'USD');
INSERT INTO account (id, owner_name, balance, currency) VALUES (5, 'Edward Norton', 15000.00, 'EUR');
INSERT INTO account (id, owner_name, balance, currency) VALUES (6, 'Fiona Apple', 30000.00, 'EUR');
INSERT INTO account (id, owner_name, balance, currency) VALUES (7, 'George Martin', 8000.00, 'EUR');
INSERT INTO account (id, owner_name, balance, currency) VALUES (8, 'Hannah Lee', 20000.00, 'GBP');
INSERT INTO account (id, owner_name, balance, currency) VALUES (9, 'Ivan Petrov', 45000.00, 'GBP');
INSERT INTO account (id, owner_name, balance, currency) VALUES (10, 'Julia Roberts', 500.00, 'GBP');

-- Reset the auto-increment counter to 11 so that programmatic inserts
-- (without explicit IDs) don't collide with seed data IDs 1-10.
ALTER TABLE account ALTER COLUMN id RESTART WITH 11;
