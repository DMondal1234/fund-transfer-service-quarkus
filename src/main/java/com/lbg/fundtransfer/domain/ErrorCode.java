package com.lbg.fundtransfer.domain;

/**
 * Application-specific error codes. Each constant bundles:
 *   - a stable machine-readable code (e.g. "FT-ACC-001") that clients can switch
 *     on and that never changes even if the human message is reworded,
 *   - the HTTP status (code + reason phrase) the error maps to,
 *   - a default human-readable message.
 *
 * Naming scheme: FT-&lt;DOMAIN&gt;-&lt;NNN&gt;
 *   AUTH auth/login, VAL validation, ACC accounts, BEN beneficiaries,
 *   CUS customers, TXN transfers/transactions, MER merchant, SYS system/unexpected.
 *
 * <p>Status is held as a plain {@code int} + reason phrase (rather than a
 * framework status enum) so codes such as 422 Unprocessable Entity are
 * represented uniformly without depending on any particular HTTP library.
 */
public enum ErrorCode {

    // Authentication
    AUTH_INVALID_CREDENTIALS("FT-AUTH-001", 401, "Unauthorized", "Invalid username or password"),

    // Validation
    VALIDATION_FAILED("FT-VAL-001", 400, "Bad Request", "Validation failed"),

    // Accounts
    ACCOUNT_NOT_FOUND("FT-ACC-001", 404, "Not Found", "Account not found or not owned by the customer"),

    // Beneficiaries
    BENEFICIARY_NOT_FOUND("FT-BEN-001", 404, "Not Found", "Beneficiary not found"),
    BENEFICIARY_ACCOUNT_NOT_FOUND("FT-BEN-002", 400, "Bad Request", "Beneficiary account does not exist"),
    BENEFICIARY_OWN_ACCOUNT("FT-BEN-003", 400, "Bad Request", "Cannot add your own account as a beneficiary"),
    BENEFICIARY_DUPLICATE("FT-BEN-004", 400, "Bad Request", "Beneficiary already exists"),
    BENEFICIARY_MERCHANT_ACCOUNT("FT-BEN-005", 400, "Bad Request", "Beneficiary is a merchant account"),

    // Customers
    CUSTOMER_NOT_FOUND("FT-CUS-001", 404, "Not Found", "Customer not found"),

    // Transfers / transactions
    TRANSFER_SAME_ACCOUNT("FT-TXN-001", 400, "Bad Request", "Source and destination accounts must differ"),
    INSUFFICIENT_BALANCE("FT-TXN-002", 422, "Unprocessable Entity", "Insufficient balance for this transfer"),
    DESTINATION_ACCOUNT_NOT_FOUND("FT-TXN-003", 404, "Not Found", "Destination account not found"),

    // Merchant payments
    MERCHANT_ACCOUNT_NOT_FOUND("FT-MER-001", 404, "Not Found", "Merchant account not found"),
    SERVICE_UNAUTHORIZED("FT-MER-002", 401, "Unauthorized", "Invalid or missing service key"),

    // Fallback
    INTERNAL_ERROR("FT-SYS-001", 500, "Internal Server Error", "An unexpected error occurred");

    private final String code;
    private final int statusCode;
    private final String reasonPhrase;
    private final String defaultMessage;

    ErrorCode(String code, int statusCode, String reasonPhrase, String defaultMessage) {
        this.code = code;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
