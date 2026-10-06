package com.lbg.fundtransfer.repository;

import com.lbg.fundtransfer.domain.Merchant;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Panache repository for {@link Merchant}.
 */
@ApplicationScoped
public class MerchantRepository implements PanacheRepository<Merchant> {

    public Optional<Merchant> findByMerchantCode(String merchantCode) {
        return find("merchantCode", merchantCode).firstResultOptional();
    }
}
