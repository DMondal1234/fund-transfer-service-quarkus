package com.lbg.fundtransfer;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class FundTransferApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
