package eu.domibus.web.rest.validators;

import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
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

        Map<String, String[]> queryParams = request.getParameterMap();
        return handleQueryParams(queryParams);
    }

    protected boolean handleQueryParams(Map<String, String[]> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return true;
        }
        try {
            validate(queryParams);
            return true;
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            return true;
        }
    }

    private void validate(Map<String, String[]> parameterMap) {
        parameterMap.forEach((key, val) -> {
            if (!blacklistValidator.isValid(val)) {
                throw new ValidationException(String.format("Blacklisted character detected in the query parameter: %s", key));
            }
        });
    }
}