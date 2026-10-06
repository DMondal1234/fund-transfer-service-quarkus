package com.lbg.fundtransfer.config;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.AccountType;
import com.lbg.fundtransfer.domain.Merchant;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.MerchantRepository;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

/**
 * Seeds merchant settlement accounts so external services (HungerBox) can credit
 * vendors. The merchant account numbers here (MER5001..MER5010) match the ten
 * HungerBox vendors' settlementAccountNumber values one-to-one. Additive: leaves
 * the existing customer seeding untouched. Skipped under the "test" profile.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class MerchantDataSeeder {

    private static final Logger log = Logger.getLogger(MerchantDataSeeder.class);

    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;

    public MerchantDataSeeder(MerchantRepository merchantRepository, AccountRepository accountRepository) {
        this.merchantRepository = merchantRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    void seedMerchants(@Observes StartupEvent event) {
        if (merchantRepository.count() > 0) {
            return;
        }
        createMerchant("MCODE5001", "Tasty Bites", "MER5001");
        createMerchant("MCODE5002", "Spice Hub", "MER5002");
        createMerchant("MCODE5003", "Green Bowl", "MER5003");
        createMerchant("MCODE5004", "Wok & Roll", "MER5004");
        createMerchant("MCODE5005", "Pizza Corner", "MER5005");
        createMerchant("MCODE5006", "Burger Junction", "MER5006");
        createMerchant("MCODE5007", "Biryani House", "MER5007");
        createMerchant("MCODE5008", "Chaat Street", "MER5008");
        createMerchant("MCODE5009", "Brew & Bean", "MER5009");
        createMerchant("MCODE5010", "Sweet Tooth", "MER5010");
        log.infof("Seeded %d merchant accounts", merchantRepository.count());
    }

    private void createMerchant(String merchantCode, String businessName, String accountNumber) {
        Merchant merchant = new Merchant(merchantCode, businessName);
        merchantRepository.persist(merchant);
        accountRepository.persist(new Account(accountNumber, AccountType.CURRENT, BigDecimal.ZERO, merchant));
    }
}
