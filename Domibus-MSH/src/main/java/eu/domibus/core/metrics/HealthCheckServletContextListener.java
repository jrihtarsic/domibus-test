package eu.domibus.core.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.ServletContextEvent;

/**
 * @author Thomas Dussart
 * @since 4.1
 */
public class HealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    @Autowired
    MetricsHelper metricsHelper;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,  sce.getServletContext());
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return metricsHelper.getHealthCheckRegistry();
    }
}
