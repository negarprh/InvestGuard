package com.example.riskanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "investguard.data")
public class MarketDataProviderProperties {

    private boolean finnhubEnabled = true;
    private String finnhubBaseUrl = "https://finnhub.io/api/v1";
    private String finnhubApiKey = "";
    private int httpTimeoutSeconds = 15;

    public boolean isFinnhubEnabled() {
        return finnhubEnabled;
    }

    public void setFinnhubEnabled(boolean finnhubEnabled) {
        this.finnhubEnabled = finnhubEnabled;
    }

    public String getFinnhubBaseUrl() {
        return finnhubBaseUrl;
    }

    public void setFinnhubBaseUrl(String finnhubBaseUrl) {
        this.finnhubBaseUrl = finnhubBaseUrl;
    }

    public String getFinnhubApiKey() {
        return finnhubApiKey;
    }

    public void setFinnhubApiKey(String finnhubApiKey) {
        this.finnhubApiKey = finnhubApiKey;
    }

    public int getHttpTimeoutSeconds() {
        return httpTimeoutSeconds;
    }

    public void setHttpTimeoutSeconds(int httpTimeoutSeconds) {
        this.httpTimeoutSeconds = httpTimeoutSeconds;
    }
}
