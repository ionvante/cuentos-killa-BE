package com.forjix.cuentoskilla.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EndpointLogger.class);

    private final RequestMappingHandlerMapping handlerMapping;

    @Autowired
    public EndpointLogger(@Qualifier("requestMappingHandlerMapping")
                          RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<String, Map<String, Set<String>>> grouped = new TreeMap<>();
        Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> map = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, org.springframework.web.method.HandlerMethod> entry : map.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                methods = EnumSet.allOf(RequestMethod.class);
            }
            for (String pattern : patterns) {
                String path = pattern;
                if (path.startsWith("/api")) {
                    path = path.substring(4); // remove /api
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                String root;
                String rest;
                if (path.length() > 1) {
                    int idx = path.indexOf('/', 1);
                    if (idx == -1) {
                        root = path.substring(1);
                        rest = "/";
                    } else {
                        root = path.substring(1, idx);
                        rest = path.substring(idx);
                    }
                } else {
                    root = "";
                    rest = "/";
                }
                root = "/" + capitalize(root);

                Map<String, Set<String>> byMethod = grouped.computeIfAbsent(root, k -> new TreeMap<>());
                for (RequestMethod method : methods) {
                    Set<String> subpaths = byMethod.computeIfAbsent(method.name(), k -> new TreeSet<>());
                    subpaths.add(rest);
                }
            }
        }

        logger.info("API/");
        for (Map.Entry<String, Map<String, Set<String>>> groupEntry : grouped.entrySet()) {
            logger.info("    {}", groupEntry.getKey());
            for (Map.Entry<String, Set<String>> methodEntry : groupEntry.getValue().entrySet()) {
                for (String sub : methodEntry.getValue()) {
                    logger.info("        {} {}", methodEntry.getKey(), sub);
                }
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
