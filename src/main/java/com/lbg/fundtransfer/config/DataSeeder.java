package com.lbg.fundtransfer.config;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.AccountType;
import com.lbg.fundtransfer.domain.Beneficiary;
import com.lbg.fundtransfer.domain.Customer;
import com.lbg.fundtransfer.repository.CustomerRepository;
import com.lbg.fundtransfer.security.PasswordEncoder;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds 20 customers with a mix of account balances and beneficiary links so
 * the API can be exercised across many scenarios (rich/poor/zero balances,
 * single vs multiple accounts, customers with and without beneficiaries).
 *
 * <p>Honours the use-case assumption that customers already exist. Skipped under
 * the "test" profile so tests own their fixtures (Quarkus
 * {@link UnlessBuildProfile}, replacing Spring's {@code @Profile("!test")}).
 *
 * <p><b>Deterministic IDs:</b> customers are persisted in a fixed order. {@code bob}
 * is saved first (no beneficiaries), then {@code alice}, so Alice's single
 * beneficiary (Bob &rarr; ACC2001) is always <b>beneficiary id 1</b> — which the
 * bundled Postman collection relies on.
 *
 * <p>Login: {@code alice}/{@code password1}, {@code bob}/{@code password2}, and
 * every other customer uses {@code password123}. See the README for the full table.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class DataSeeder {
    private static final Logger log = Logger.getLogger(DataSeeder.class);

    private static final String DEFAULT_PASSWORD = "password123";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(CustomerRepository customerRepository, PasswordEncoder encoder) {
        this.customerRepository = customerRepository;
        this.encoder = encoder;
    }

    @Transactional
    void seed(@Observes StartupEvent event) {
        if (customerRepository.count() > 0) {
            return;
        }
        for (Customer customer : buildCustomers()) {
            customerRepository.persist(customer);
        }
        log.infof("Seeded %d customers", customerRepository.count());
    }

    private List<Customer> buildCustomers() {
        // --- Customer 2: Bob (saved FIRST, no beneficiaries) ---
        Customer bob = customer("bob", "password2", "Bob Iyer", "bob@example.com");
        account(bob, "ACC2001", AccountType.SAVINGS, "800.00");

        // --- Customer 1: Alice (saved SECOND -> her beneficiary is id 1) ---
        Customer alice = customer("alice", "password1", "Alice Fernandes", "alice@example.com");
        account(alice, "ACC1001", AccountType.SAVINGS, "5000.00");
        account(alice, "ACC1002", AccountType.CURRENT, "1500.00");
        beneficiary(alice, "Bob (friend)", "ACC2001");

        // --- Customers 3..20 ---
        Customer carol = customer("carol", DEFAULT_PASSWORD, "Carol Menon", "carol@example.com");
        account(carol, "ACC3001", AccountType.SAVINGS, "12000.00");
        account(carol, "ACC3002", AccountType.CURRENT, "300.00");
        beneficiary(carol, "Dave", "ACC4001");
        beneficiary(carol, "Eve", "ACC5001");

        Customer dave = customer("dave", DEFAULT_PASSWORD, "Dave Kapoor", "dave@example.com");
        account(dave, "ACC4001", AccountType.SAVINGS, "50.00"); // low balance -> insufficient-funds tests
        beneficiary(dave, "Alice", "ACC1001");

        Customer eve = customer("eve", DEFAULT_PASSWORD, "Eve Nair", "eve@example.com");
        account(eve, "ACC5001", AccountType.CURRENT, "25000.00");

        Customer frank = customer("frank", DEFAULT_PASSWORD, "Frank DSouza", "frank@example.com");
        account(frank, "ACC6001", AccountType.SAVINGS, "3000.00");
        beneficiary(frank, "Grace", "ACC7001");

        Customer grace = customer("grace", DEFAULT_PASSWORD, "Grace Pillai", "grace@example.com");
        account(grace, "ACC7001", AccountType.SAVINGS, "7500.00");
        account(grace, "ACC7002", AccountType.CURRENT, "2000.00");

        Customer heidi = customer("heidi", DEFAULT_PASSWORD, "Heidi Rao", "heidi@example.com");
        account(heidi, "ACC8001", AccountType.CURRENT, "100.00");
        beneficiary(heidi, "Ivan", "ACC9001");

        Customer ivan = customer("ivan", DEFAULT_PASSWORD, "Ivan Sharma", "ivan@example.com");
        account(ivan, "ACC9001", AccountType.SAVINGS, "640.00");

        Customer judy = customer("judy", DEFAULT_PASSWORD, "Judy Thomas", "judy@example.com");
        account(judy, "ACC10001", AccountType.SAVINGS, "15000.00");
        beneficiary(judy, "Mallory", "ACC13001");

        Customer karl = customer("karl", DEFAULT_PASSWORD, "Karl Verma", "karl@example.com");
        account(karl, "ACC11001", AccountType.CURRENT, "0.00"); // zero balance

        Customer laura = customer("laura", DEFAULT_PASSWORD, "Laura Gomes", "laura@example.com");
        account(laura, "ACC12001", AccountType.SAVINGS, "9000.00");
        account(laura, "ACC12002", AccountType.SAVINGS, "500.00");

        Customer mallory = customer("mallory", DEFAULT_PASSWORD, "Mallory Fernandes", "mallory@example.com");
        account(mallory, "ACC13001", AccountType.SAVINGS, "250.00");

        Customer niaj = customer("niaj", DEFAULT_PASSWORD, "Niaj Bose", "niaj@example.com");
        account(niaj, "ACC14001", AccountType.CURRENT, "4200.00");
        beneficiary(niaj, "Olivia", "ACC15001");

        Customer olivia = customer("olivia", DEFAULT_PASSWORD, "Olivia DCosta", "olivia@example.com");
        account(olivia, "ACC15001", AccountType.SAVINGS, "33000.00");

        Customer peggy = customer("peggy", DEFAULT_PASSWORD, "Peggy Shetty", "peggy@example.com");
        account(peggy, "ACC16001", AccountType.SAVINGS, "600.00");

        Customer quentin = customer("quentin", DEFAULT_PASSWORD, "Quentin Reddy", "quentin@example.com");
        account(quentin, "ACC17001", AccountType.CURRENT, "1800.00");
        beneficiary(quentin, "Rupert", "ACC18001");

        Customer rupert = customer("rupert", DEFAULT_PASSWORD, "Rupert Joseph", "rupert@example.com");
        account(rupert, "ACC18001", AccountType.SAVINGS, "2750.00");

        Customer sybil = customer("sybil", DEFAULT_PASSWORD, "Sybil Anand", "sybil@example.com");
        account(sybil, "ACC19001", AccountType.SAVINGS, "8800.00");

        Customer trent = customer("trent", DEFAULT_PASSWORD, "Trent Mathew", "trent@example.com");
        account(trent, "ACC20001", AccountType.CURRENT, "46000.00");
        account(trent, "ACC20002", AccountType.SAVINGS, "1200.00");
        beneficiary(trent, "Alice", "ACC1001");
        beneficiary(trent, "Bob", "ACC2001");

        // Save order matters: bob then alice (keeps Alice's beneficiary = id 1), then the rest.
        return List.of(
                bob, alice, carol, dave, eve, frank, grace, heidi, ivan, judy,
                karl, laura, mallory, niaj, olivia, peggy, quentin, rupert, sybil, trent);
    }

    // --- helpers ---

    private Customer customer(String username, String rawPassword, String fullName, String email) {
        return new Customer(username, encoder.encode(rawPassword), fullName, email);
    }

    private static void account(Customer owner, String number, AccountType type, String balance) {
        owner.getAccounts().add(new Account(number, type, new BigDecimal(balance), owner));
    }

    private static void beneficiary(Customer owner, String nickname, String accountNumber) {
        owner.getBeneficiaries().add(new Beneficiary(nickname, accountNumber, owner));
    }
}
