package com.capgemini.api.zuulgateway.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ConfigClientBootstrapConfiguration {


    @Value("${spring.jwt.username}")
    private String jwtUsername;

    @Value("${spring.jwt.password}")
    private String jwtPassword;

    @Value("${spring.jwt.endpoint}")
    private String jwtEndpoint;

    private String jwtToken;

    @PostConstruct
    public void init() {

        RestTemplate restTemplate = new RestTemplate();

        TokenRequest loginBackend = new TokenRequest();
        loginBackend.setUsername(jwtUsername);
        loginBackend.setPassword(jwtPassword);

        String url = jwtEndpoint;
        try {
            jwtToken = restTemplate.postForObject(url, loginBackend, String.class);
            if (jwtToken == null) {
                throw new Exception();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    @ConditionalOnMissingBean(ConfigServicePropertySourceLocator.class)
    @ConditionalOnProperty(value = "spring.cloud.config.enabled", matchIfMissing = true)
    public ConfigServicePropertySourceLocator configServicePropertySource(
            ConfigClientProperties properties) {
        ConfigServicePropertySourceLocator configServicePropertySourceLocator = new ConfigServicePropertySourceLocator(
                properties);
        configServicePropertySourceLocator.setRestTemplate(customRestTemplate(properties));

        return configServicePropertySourceLocator;
    }

    private RestTemplate customRestTemplate(ConfigClientProperties clientProperties) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + jwtToken);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout((60 * 1000 * 3) + 5000); // TODO 3m5s make
        // configurable?
        RestTemplate template = new RestTemplate(requestFactory);
        if (!headers.isEmpty()) {
            template.setInterceptors(
                    Arrays.<ClientHttpRequestInterceptor> asList(new ConfigServicePropertySourceLocator.GenericRequestHeaderInterceptor(headers)));
        }

        return template;
    }

}
