package com.gridpulse.payment.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.gridpulse.common.security.InternalApi;
import com.gridpulse.payment.client.CoreClient;
import com.gridpulse.web.security.InternalApiProperties;

import lombok.extern.slf4j.Slf4j;

// CoreClient over a load balanced RestClient: http://core-service = one of the healthy core instances
// every call carries X-Internal-Key, plus the caller's own token when there is one (the core checks the driver)
// the key is only sent from here: this service has no /internal/** urls, so no InternalKeyFilter
@Slf4j
@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class CoreClientConfig {

    private static final String CORE = "http://core-service";

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    // the load balancer's health checks call every instance directly with this one
    @Bean
    public RestClient healthCheckRestClient() {
        return RestClient.builder().requestFactory(requestFactory()).build();
    }

    @Bean
    public CoreClient coreClient(RestClient.Builder loadBalancedRestClientBuilder,
            InternalApiProperties internalApiProperties) {
        String key = InternalApi.keyOrDevDefault(internalApiProperties.internalApiKey());
        if (InternalApi.DEV_KEY.equals(key)) {
            log.warn("INTERNAL_API_KEY is not set - the services use a fixed dev key, set it on a real server");
        }
        RestClient restClient = loadBalancedRestClientBuilder
                .baseUrl(CORE)
                .requestFactory(requestFactory())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(InternalApi.HEADER, key);
                    callerToken().ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, token));
                    return execution.execute(request, body);
                })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(CoreClient.class);
    }

    // connect 2 s, answer 5 s: a stuck core must not hold the driver's confirm for long
    private static ClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    // the Authorization header of the request being served; none in the refund job
    private static Optional<String> callerToken() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION));
    }
}
