package com.lbg.fundtransfer.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.h2.tools.Server;

@ApplicationScoped
public class H2ConsoleServer {
    void start(@Observes StartupEvent ev) throws Exception {
        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
    }
}