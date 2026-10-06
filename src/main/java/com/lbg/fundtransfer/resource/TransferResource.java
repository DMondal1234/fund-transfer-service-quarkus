package com.lbg.fundtransfer.resource;

import com.lbg.fundtransfer.dto.TransferRequest;
import com.lbg.fundtransfer.dto.TransferResponse;
import com.lbg.fundtransfer.service.TransferService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/transfers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TransferResource {

    private final TransferService transferService;

    public TransferResource(TransferService transferService) {
        this.transferService = transferService;
    }

    @POST
    public Response transfer(@Context SecurityContext securityContext,
                             @Valid TransferRequest request) {
        TransferResponse response = transferService.transfer(
                securityContext.getUserPrincipal().getName(), request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
