package eu.domibus.core.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration
public class SimpleDateFormatConfiguration {

    @Bean("xmlDateTimeFormat")
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }
}
