package com.fortech.api;


public interface ApiIntegrationAccessor {

    String getConfig();
    void reportCartStarted(String request);
    void signIn();

}
