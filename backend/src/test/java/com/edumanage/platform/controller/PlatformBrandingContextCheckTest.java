package com.edumanage.platform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class PlatformBrandingContextCheckTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    @Test
    void brandingControllerMapped() throws Exception {
        for (String path : new String[]{"/platform/schools/1/branding", "/platform/dashboard"}) {
            try {
                HandlerExecutionChain chain = mapping.getHandler(createRequest(path));
                System.out.println("HANDLER_FOR_" + path + ": " + (chain != null ? chain.getHandler().getClass().getName() : "null"));
            } catch (Exception e) {
                System.out.println("HANDLER_FOR_" + path + ": EXCEPTION " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
    }

    private MockHttpServletRequest createRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI(uri);
        request.setServletPath(uri);
        return request;
    }
}
