package eu.domibus.core.spring;

import eu.domibus.core.security.configuration.SecurityAdminConsoleConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration("domibusConfiguration")
@ImportResource({
        "classpath:META-INF/cxf/cxf.xml",
        "classpath:META-INF/cxf/cxf-extension-jaxws.xml",
        "classpath:META-INF/cxf/cxf-servlet.xml",
        "classpath*:META-INF/resources/WEB-INF/spring-context.xml"
})
@ComponentScan(
        basePackages = "eu.domibus",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.web\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.ext\\.rest\\..*")}
)
@EnableJms
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@Import({SecurityAdminConsoleConfiguration.class})
public class DomibusRootConfiguration {

}