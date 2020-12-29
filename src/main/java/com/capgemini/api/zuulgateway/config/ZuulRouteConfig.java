package com.capgemini.api.zuulgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("zuul")
public class ZuulRouteConfig {
    private Map<String, AllowedChannels> routes = new LinkedHashMap<>();

    public static class AllowedChannels{
            private List<String> channels;

        public List<String> getChannels() {
            return channels;
        }

        public void setChannels(List<String> channels) {
            this.channels = channels;
        }
    }

    public Map<String, AllowedChannels> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, AllowedChannels> routes) {
        this.routes = routes;
    }
}

