package com.lbg.fundtransfer;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.AccountType;
import com.lbg.fundtransfer.domain.Beneficiary;
import com.lbg.fundtransfer.domain.Customer;
import com.lbg.fundtransfer.domain.Merchant;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.CustomerRepository;
import com.lbg.fundtransfer.repository.MerchantRepository;
import com.lbg.fundtransfer.security.PasswordEncoder;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end flow test over the real HTTP stack: login, scoped reads, a P2P
 * transfer, the insufficient-funds rejection, and an internal merchant payment.
 *
 * <p>The startup seeders are disabled under the {@code test} profile (as in the
 * Spring Boot original), so this test persists its own minimal fixtures. Bob is
 * persisted before Alice so Alice's single beneficiary gets id {@code 1}.
 */
@QuarkusTest
class FundTransferIntegrationTest {

    @Inject
    CustomerRepository customerRepository;
    @Inject
    AccountRepository accountRepository;
    @Inject
    MerchantRepository merchantRepository;
    @Inject
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            if (customerRepository.count() > 0) {
                return;
            }
            Customer bob = new Customer("bob", passwordEncoder.encode("password2"),
                    "Bob Iyer", "bob@example.com");
            bob.getAccounts().add(new Account("ACC2001", AccountType.SAVINGS, new BigDecimal("800.00"), bob));
            customerRepository.persist(bob);

            Customer alice = new Customer("alice", passwordEncoder.encode("password1"),
                    "Alice Fernandes", "alice@example.com");
            alice.getAccounts().add(new Account("ACC1001", AccountType.SAVINGS, new BigDecimal("5000.00"), alice));
            alice.getBeneficiaries().add(new Beneficiary("Bob (friend)", "ACC2001", alice));
            customerRepository.persist(alice);

            Merchant merchant = new Merchant("MCODE5001", "Tasty Bites");
            merchantRepository.persist(merchant);
            accountRepository.persist(new Account("MER5001", AccountType.CURRENT, BigDecimal.ZERO, merchant));
        });
    }

    private String loginAsAlice() {
        return given()
                .contentType("application/json")
                .body("{\"username\":\"alice\",\"password\":\"password1\"}")
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .body("token", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .extract().path("token");
    }

    @Test
    void login_returnsBearerToken() {
        loginAsAlice();
    }

    @Test
    void accounts_withoutToken_isUnauthorized() {
        given().when().get("/api/accounts").then().statusCode(401);
    }

    @Test
    void accounts_withToken_listsOwnAccounts() {
        String token = loginAsAlice();
        given().header("Authorization", "Bearer " + token)
                .when().get("/api/accounts")
                .then().statusCode(200)
                .body("accountNumber", hasItem("ACC1001"));
    }

    @Test
    void transfer_toBeneficiary_succeeds() {
        String token = loginAsAlice();
        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"sourceAccountNumber\":\"ACC1001\",\"beneficiaryId\":1,\"amount\":100.00,\"description\":\"test\"}")
                .when().post("/api/transfers")
                .then().statusCode(201)
                .body("status", equalTo("SUCCESS"))
                .body("transactionId", notNullValue());
    }

    @Test
    void transfer_withInsufficientFunds_isUnprocessable() {
        String token = loginAsAlice();
        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"sourceAccountNumber\":\"ACC1001\",\"beneficiaryId\":1,\"amount\":9999999.00}")
                .when().post("/api/transfers")
                .then().statusCode(422)
                .body("code", equalTo("FT-TXN-002"));
    }

    @Test
    void internalMerchantPayment_withServiceKey_succeeds() {
        given().header("X-Service-Key", "hungerbox-to-fundtransfer-shared-key-2026")
                .contentType("application/json")
                .body("{\"sourceAccountNumber\":\"ACC1001\",\"merchantAccountNumber\":\"MER5001\",\"amount\":1.00,\"reference\":\"ORDER-1\"}")
                .when().post("/internal/payments/merchant")
                .then().statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .body("reference", equalTo("ORDER-1"));
    }

    @Test
    void internalMerchantPayment_withoutServiceKey_isUnauthorized() {
        given().contentType("application/json")
                .body("{\"sourceAccountNumber\":\"ACC1001\",\"merchantAccountNumber\":\"MER5001\",\"amount\":1.00,\"reference\":\"ORDER-2\"}")
                .when().post("/internal/payments/merchant")
                .then().statusCode(401)
                .body("code", equalTo("FT-MER-002"));
    }
}
