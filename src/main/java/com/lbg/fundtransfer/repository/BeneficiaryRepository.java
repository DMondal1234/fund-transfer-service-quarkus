package com.lbg.fundtransfer.repository;

import com.lbg.fundtransfer.domain.Beneficiary;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Panache repository for {@link Beneficiary}.
 */
@ApplicationScoped
public class BeneficiaryRepository implements PanacheRepository<Beneficiary> {

    public List<Beneficiary> findByCustomerUsername(String username) {
        return list("customer.username", username);
    }

    public Optional<Beneficiary> findByIdAndCustomerUsername(Long id, String username) {
        return find("id = ?1 and customer.username = ?2", id, username).firstResultOptional();
    }

    public boolean existsByCustomerUsernameAndAccountNumber(String username, String accountNumber) {
        return count("customer.username = ?1 and accountNumber = ?2", username, accountNumber) > 0;
    }
}
