package com.vivos.api.config;

import com.vivos.api.store.CurrentStoreArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentStoreArgumentResolver currentStoreArgumentResolver;

    WebConfig(CurrentStoreArgumentResolver currentStoreArgumentResolver) {
        this.currentStoreArgumentResolver = currentStoreArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentStoreArgumentResolver);
    }
}
