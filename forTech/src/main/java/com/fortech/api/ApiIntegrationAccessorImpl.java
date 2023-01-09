package com.fortech.api;

import com.tracxpoint.serverless.api.config.ServerlessApiConfiguration;
import com.tracxpoint.serverless.manager.ConfigurationStorageManagerSon;
import com.tracxpoint.serverless.manager.NetworkManager;
import com.tracxpoint.serverless.model.CartConfigResponse;
import com.tracxpoint.serverless.model.CartStartedRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class ApiIntegrationAccessorImpl implements ApiIntegrationAccessor, InitializingBean {
    private static final String AUTHENTICATE = "/user/login";
    private static final String CART_CONFIG = "/cart/config";
    private static final String CART_REPORT_STARTED = "/cart/%s/report_started";

    private final RestTemplateBuilder restTemplateBuilder;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final AtomicReference<String> token = new AtomicReference<>();

    public ApiIntegrationAccessorImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;

    }


    @Override
    public void signIn() {
        authenticate();
    }

    public String getConfig() {
        URI config = URI.create(properties.getRest() + CART_CONFIG);
        try{
            CartConfigResponse response = getRestTemplateWithAuthorization().getForObject(config, CartConfigResponse.class);
            response.setIpAddress(networkManager.getIpAddress());
            return response;
        } catch (HttpStatusCodeException ex){
            log.error("Could not reach the Cart configuration");
            throw ex;
        } catch (Exception ex) {
            log.error("Server is not reachable");
            throw ex;
        }
    }

    @Override
    public void reportCartStarted(String request) {

    }




    private String getAuthToken() {
        return Objects.requireNonNull(
                getRestTemplate().postForEntity(
                        String.valueOf(URI.create(properties.getRest() + AUTHENTICATE)),
                        new AuthenticationRequest().setEmail(networkManager.getMacAddress()).setPassword(config.getPassword()),
                        AuthenticationResponse.class
                ).getBody()).authToken;
    }

    @Override
    public void afterPropertiesSet() {
        if (config.isCredentialsConfigured()) {
            authenticate();
        }
    }
    private RestTemplate getRestTemplateWithAuthorization() {
        return restTemplateBuilder.defaultHeader("Authorization", token.get()).build();
    }

    private void authenticate() {
        if (token.get() == null)
            token.set(getAuthToken());
        log.info(String.format("The token is [%s]", token));
        executorService.scheduleAtFixedRate(() -> token.set(getAuthToken()), 50, 50, TimeUnit.MINUTES);
    }


    private RestTemplate getRestTemplate() {
        return restTemplateBuilder.build();
    }

    @Data
    static class AuthenticationResponse {
        private String authToken;
    }

    @Data
    @Accessors(chain = true)
    static class AuthenticationRequest {
        private String email;
        private String password;
    }

}
