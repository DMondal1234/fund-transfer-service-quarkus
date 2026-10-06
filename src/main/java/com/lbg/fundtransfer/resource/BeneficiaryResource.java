package com.lbg.fundtransfer.resource;

import com.lbg.fundtransfer.dto.BeneficiaryRequest;
import com.lbg.fundtransfer.dto.BeneficiaryResponse;
import com.lbg.fundtransfer.service.BeneficiaryService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.net.URI;
import java.util.List;

@Path("/api/beneficiaries")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BeneficiaryResource {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryResource(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GET
    public List<BeneficiaryResponse> list(@Context SecurityContext securityContext) {
        return beneficiaryService.list(username(securityContext));
    }

    @GET
    @Path("/{id}")
    public BeneficiaryResponse get(@Context SecurityContext securityContext,
                                   @PathParam("id") Long id) {
        return beneficiaryService.get(username(securityContext), id);
    }

    @POST
    public Response add(@Context SecurityContext securityContext,
                        @Valid BeneficiaryRequest request) {
        BeneficiaryResponse created = beneficiaryService.add(username(securityContext), request);
        return Response.created(URI.create("/api/beneficiaries/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    public BeneficiaryResponse update(@Context SecurityContext securityContext,
                                      @PathParam("id") Long id,
                                      @Valid BeneficiaryRequest request) {
        return beneficiaryService.update(username(securityContext), id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@Context SecurityContext securityContext,
                           @PathParam("id") Long id) {
        beneficiaryService.delete(username(securityContext), id);
        return Response.noContent().build();
    }

    private static String username(SecurityContext ctx) {
        return ctx.getUserPrincipal().getName();
    }
}
