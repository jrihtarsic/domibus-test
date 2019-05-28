package eu.domibus.web.rest.validators;

import com.google.common.base.Strings;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Custom validator that checks that the value does not contain any char from the blacklist
 *
 * @author Ion Perpegel
 * @since 4.1
 */
@Component
public class BlacklistValidator extends BaseBlacklistValidator<NotBlacklisted, String> {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(BlacklistValidator.class);

    @Override
    protected String getErrorMessage() {
        return NotBlacklisted.MESSAGE;
    }

    public boolean isValid(String value) {
        if (ArrayUtils.isEmpty(blacklist)) {
            return true;
        }
        if (Strings.isNullOrEmpty(value)) {
            return true;
        }

        return !Arrays.stream(blacklist).anyMatch(el -> value.contains(el.toString()));
    }

}
