package com.lbg.fundtransfer.resource;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.MerchantPaymentRequest;
import com.lbg.fundtransfer.dto.MerchantPaymentResponse;
import com.lbg.fundtransfer.exception.BusinessException;
import com.lbg.fundtransfer.service.MerchantPaymentService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Internal, service-to-service payment API. Not for end users: it is authorized
 * by a shared service key (X-Service-Key header), and it moves money by account
 * numbers without the beneficiary requirement of the customer-facing API.
 */
@Path("/internal/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalPaymentResource {

    private final MerchantPaymentService merchantPaymentService;
    private final String serviceKey;

    public InternalPaymentResource(MerchantPaymentService merchantPaymentService,
                                   @ConfigProperty(name = "app.internal.api-key") String serviceKey) {
        this.merchantPaymentService = merchantPaymentService;
        this.serviceKey = serviceKey;
    }

    @POST
    @Path("/merchant")
    public MerchantPaymentResponse payMerchant(
            @HeaderParam("X-Service-Key") String key,
            @Valid MerchantPaymentRequest request) {
        if (serviceKey == null || !serviceKey.equals(key)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAUTHORIZED);
        }
        return merchantPaymentService.pay(request);
    }
}
