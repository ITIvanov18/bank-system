package com.nbu.bank_system.service;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.dto.account.AccountOpeningResponse;
import com.nbu.bank_system.dto.account.AccountStatusResponse;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountService {

    private static final String COUNTRY_CODE = "BG";
    private static final String BANK_CODE = "BNKI";
    private static final String BRANCH_CODE = "0001";
    private static final String ACCOUNT_TYPE = "10";
    private static final long MAX_ACCOUNT_SEQUENCE = 99_999_999L;

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;

    public CustomerAccountService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository,
            LoanRepository loanRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public AccountStatusResponse getAccountStatus(String authenticatedEmail) {
        Customer customer = getAuthenticatedCustomer(authenticatedEmail);
        BigDecimal outstandingDebtAmount = loanRepository.calculateOutstandingPrincipalDebt(customer.getId())
                .max(BigDecimal.ZERO);

        return findCustomerDisplayAccount(customer.getId())
                .map(account -> new AccountStatusResponse(
                        true,
                        account.getId(),
                        account.getIban(),
                        account.getBalance(),
                        outstandingDebtAmount,
                        account.getStatus()
                ))
                .orElseGet(() -> new AccountStatusResponse(false, null, null, null, outstandingDebtAmount, null));
    }

    @Transactional
    public AccountOpeningResponse openAccountForCustomer(String authenticatedEmail) {
        Customer customer = getAuthenticatedCustomer(authenticatedEmail);

        BankAccount existingAccount = bankAccountRepository.findFirstByOwnerIdOrderByIdAsc(customer.getId()).orElse(null);
        if (existingAccount != null) {
            return new AccountOpeningResponse(
                    false,
                    existingAccount.getId(),
                    existingAccount.getIban(),
                    existingAccount.getBalance(),
                    existingAccount.getStatus(),
                    "Customer already has an active bank account."
            );
        }

        String iban = generateUniqueIban();
        BankAccount createdAccount = bankAccountRepository.save(
                new BankAccount(iban, BigDecimal.ZERO, AccountStatus.ACTIVE, customer)
        );

        return new AccountOpeningResponse(
                true,
                createdAccount.getId(),
                createdAccount.getIban(),
                createdAccount.getBalance(),
                createdAccount.getStatus(),
                "Bank account created successfully."
        );
    }

    private Customer getAuthenticatedCustomer(String authenticatedEmail) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase();
        return customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));
    }

    private Optional<BankAccount> findCustomerDisplayAccount(Long customerId) {
        return bankAccountRepository.findFirstByOwnerIdAndStatusOrderByIdAsc(customerId, AccountStatus.ACTIVE)
                .or(() -> bankAccountRepository.findFirstByOwnerIdOrderByIdAsc(customerId));
    }

    private String generateUniqueIban() {
        long sequence = bankAccountRepository.count() + 1;

        while (sequence <= MAX_ACCOUNT_SEQUENCE) {
            String iban = buildBulgarianIban(sequence);
            if (!bankAccountRepository.existsByIban(iban)) {
                return iban;
            }
            sequence++;
        }

        throw new IllegalStateException("Unable to generate a unique IBAN");
    }

    private String buildBulgarianIban(long sequence) {
        String accountNumber = String.format("%08d", sequence);
        String bban = BANK_CODE + BRANCH_CODE + ACCOUNT_TYPE + accountNumber;

        String rearranged = bban + COUNTRY_CODE + "00";
        int mod97 = mod97(convertIbanLettersToNumbers(rearranged));
        int checkDigits = 98 - mod97;

        return COUNTRY_CODE + String.format("%02d", checkDigits) + bban;
    }

    private String convertIbanLettersToNumbers(String value) {
        StringBuilder numeric = new StringBuilder(value.length() * 2);

        for (char currentChar : value.toCharArray()) {
            if (Character.isLetter(currentChar)) {
                int mapped = Character.toUpperCase(currentChar) - 'A' + 10;
                numeric.append(mapped);
            } else {
                numeric.append(currentChar);
            }
        }

        return numeric.toString();
    }

    private int mod97(String numericIban) {
        int remainder = 0;

        for (char currentChar : numericIban.toCharArray()) {
            remainder = (remainder * 10 + (currentChar - '0')) % 97;
        }

        return remainder;
    }
}

