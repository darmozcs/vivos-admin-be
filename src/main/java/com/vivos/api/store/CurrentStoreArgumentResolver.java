package com.vivos.api.store;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/** Resuelve los parametros {@code @CurrentStore UUID storeId} de los controllers. */
@Component
public class CurrentStoreArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentStoreResolver currentStoreResolver;

    CurrentStoreArgumentResolver(CurrentStoreResolver currentStoreResolver) {
        this.currentStoreResolver = currentStoreResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentStore.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return currentStoreResolver.resolveStoreId();
    }
}
