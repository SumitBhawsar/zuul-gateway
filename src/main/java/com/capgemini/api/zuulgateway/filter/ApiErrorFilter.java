package com.capgemini.api.zuulgateway.filter;

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_ERROR_FILTER_ORDER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class ApiErrorFilter extends SendErrorFilter {
    @Override
    public int filterOrder() {
        return SEND_ERROR_FILTER_ORDER - 1;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        Throwable exception = ctx.getThrowable();
        Throwable rootCause = exception.getCause();
        boolean handled = false;
        while (rootCause != exception.getCause()) {
            if (exception instanceof RuntimeException) {
                addErrorResponse(rootCause.getMessage(), 401);
                handled = true;
            }
            exception = rootCause;
            rootCause = rootCause.getCause();
        }

        if (handled == false) {
            addErrorResponse(rootCause.getMessage(), 500);
        }
        ctx.set(SEND_ERROR_FILTER_RAN, true);
        return null;
    }

    private void addErrorResponse(String message, int code) {
        HttpServletResponse response = RequestContext.getCurrentContext().getResponse();
        try {
            PrintWriter pr = new PrintWriter(response.getOutputStream());
            pr.write("{ \"error\" : \"" + message + "\"}");
            RequestContext.getCurrentContext().setResponseStatusCode(code);
            response.setStatus(code);
            response.setContentType(APPLICATION_JSON_VALUE);
            pr.flush();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
