package eu.domibus.web.rest.validators;

import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Ion Perpegel
 * @since 4.1
 * A Spring interceptor that ensures that the request parameters of a REST call does not contain blacklisted chars
 */
@ControllerAdvice(annotations = RestController.class)
public class RestQueryParamsValidationInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(RestQueryParamsValidationInterceptor.class);

    @PostConstruct
    public void init() {
        blacklistValidator.init();
    }

    @Autowired
    ItemsBlacklistValidator blacklistValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod && shouldSkipValidation((HandlerMethod) handler)) {
            return true;
        }

        Map<String, String[]> queryParams = request.getParameterMap();
        return handleQueryParams(queryParams);
    }

    private boolean shouldSkipValidation(HandlerMethod handler) {
        HandlerMethod method = handler;
        SkipWhiteListed skipAnnot = method.getMethodAnnotation(SkipWhiteListed.class);
        if (skipAnnot != null) {
            return true;
        }
        if (ArrayUtils.isNotEmpty(method.getMethodParameters())) {
            boolean skip = Arrays.stream(method.getMethodParameters()).anyMatch(param -> param.getParameterAnnotation(SkipWhiteListed.class) != null);
            if (skip) {
                return true;
            }
        }
        return false;
    }

    protected boolean handleQueryParams(Map<String, String[]> queryParams) {
        LOG.debug("Validate query params:[{}]", queryParams);
        if (queryParams == null || queryParams.isEmpty()) {
            LOG.debug("Query params are empty, exiting.");
            return true;
        }
        try {
            validate(queryParams);
            LOG.debug("Query params:[{}] validated successfully", queryParams);
            return true;
        } catch (ValidationException ex) {
            LOG.debug("Query params:[{}] are invalid: [{}]", queryParams, ex);
            throw ex;
        } catch (Exception ex) {
            LOG.debug("Unexpected exception caught [{}] when validating query params [{}]. Request will be processed downhill.", ex, queryParams);
            return true;
        }
    }

    private void validate(Map<String, String[]> parameterMap) {
        parameterMap.forEach((key, val) -> {
            if (!blacklistValidator.isValid(val)) {
                throw new ValidationException(String.format("Forbidden character detected in the query parameter: %s", key));
            }
        });
    }
}