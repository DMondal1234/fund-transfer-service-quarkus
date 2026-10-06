package com.lbg.fundtransfer.resource;

import com.lbg.fundtransfer.dto.AccountResponse;
import com.lbg.fundtransfer.service.AccountService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/api/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {

    private final AccountService accountService;

    public AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    public List<AccountResponse> myAccounts(@Context SecurityContext securityContext) {
        return accountService.getAccountsFor(username(securityContext));
    }

    @GET
    @Path("/{accountNumber}")
    public AccountResponse account(@Context SecurityContext securityContext,
                                   @PathParam("accountNumber") String accountNumber) {
        return accountService.getAccount(username(securityContext), accountNumber);
    }

    private static String username(SecurityContext ctx) {
        return ctx.getUserPrincipal().getName();
    }
}
