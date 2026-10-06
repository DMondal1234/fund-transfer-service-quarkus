package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.MerchantPaymentRequest;
import com.lbg.fundtransfer.dto.MerchantPaymentResponse;

/**
 * Customer-to-merchant payment (used by external services such as HungerBox).
 */
public interface MerchantPaymentService {

    MerchantPaymentResponse pay(MerchantPaymentRequest request);
}
