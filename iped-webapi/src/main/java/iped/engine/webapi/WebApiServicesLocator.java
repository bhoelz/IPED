package iped.engine.webapi;

import java.util.ServiceLoader;

import iped.engine.webapi.spi.WebApiServices;
import iped.engine.webapi.spi.WebApiServicesFactory;

final class WebApiServicesLocator {
    private static final WebApiServices SERVICES = load();

    private WebApiServicesLocator() {
    }

    static WebApiServices get() {
        return SERVICES;
    }

    private static WebApiServices load() {
        return ServiceLoader.load(WebApiServicesFactory.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No WebApiServicesFactory implementation found"))
                .create();
    }
}
