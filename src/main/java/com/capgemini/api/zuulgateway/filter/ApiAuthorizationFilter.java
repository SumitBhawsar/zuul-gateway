package com.capgemini.api.zuulgateway.filter;

import com.capgemini.api.zuulgateway.config.ZuulRouteConfig;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.http.HttpHeaders;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Component
public class ApiAuthorizationFilter extends ZuulFilter {

    @Autowired
    private ZuulRouteConfig zuulRouteConfig;

    @Value("${jwks.url:http://localhost:10111/jwks.json}")
    private String jwkUrl;


    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean shouldFilter() {
        String requestUri = RequestContext.getCurrentContext().getRequest().getRequestURI();
        return !requestUri.startsWith("/actuator/");
    }

    @Override
    public Object run() throws ZuulException {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String requestedUri = RequestContext.getCurrentContext().getRequest().getRequestURI();
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!hasText(jwtToken)) {
            throw new RuntimeException("Auth Header not sent");
        }
        JwtClaims jwtClaims = null;
        try {
            HttpsJwks httpsJkws = new HttpsJwks(jwkUrl);

            // The HttpsJwksVerificationKeyResolver uses JWKs obtained from the HttpsJwks and will select the
            // most appropriate one to use for verification based on the Key ID and other factors provided
            // in the header of the JWS/JWT.
            HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);


            // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
            // be used to validate and process the JWT. But, in this case, provide it with
            // the HttpsJwksVerificationKeyResolver instance rather than setting the
            // verification key explicitly.
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKeyResolver(httpsJwksKeyResolver)
                    .setExpectedAudience("Audience")
                    .setExpectedIssuer("Issuer")
                    .build();
            jwtClaims = jwtConsumer.processToClaims(jwtToken.replace("Bearer ", ""));
            checkAccess(jwtClaims.getSubject());
        } catch (RuntimeException rte){
            throw rte;
        }catch (Exception exc) {
            exc.printStackTrace();
            throw new RuntimeException("Auth Header Signature is not valid ");
        }
        return null;
    }

    private void checkAccess(String subject) {
        String serviceId = (String) RequestContext.getCurrentContext().get(PROXY_KEY);
        ZuulRouteConfig.AllowedChannels allowedChannel = zuulRouteConfig.getRoutes().get(serviceId);

        if(allowedChannel != null && !isEmpty(allowedChannel.getChannels())){
            boolean allowed = false;
            for (String channel : allowedChannel.getChannels()){
                if(channel.equals(subject)){
                    allowed = true;
                    break;
                }
            }

            if(!allowed){
                throw new RuntimeException("Channel is not allowed to access this URL");
            }
        }
    }

}
