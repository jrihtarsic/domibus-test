package eu.domibus.quartz;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.ebms3.common.quartz.AutowiringSpringBeanJobFactory;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Cosmin Baciu, Tiago Miguel
 * @since 4.0
 */
@Configuration
@DependsOn("springContextProvider")
public class DomainSchedulerFactoryConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomainSchedulerFactoryConfiguration.class);
    private static final String GROUP_GENERAL = "GENERAL";

    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;

    @Autowired
    @Qualifier("quartzTaskExecutor")
    protected Executor quartzTaskExecutor;

    @Autowired
    protected ApplicationContext applicationContext;

    @Qualifier("domibusJDBC-XADataSource")
    @Autowired
    protected DataSource dataSource;

    @Qualifier("domibusJDBC-nonXADataSource")
    @Autowired
    protected DataSource nonTransactionalDataSource;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected AutowiringSpringBeanJobFactory autowiringSpringBeanJobFactory;

    @Autowired
    protected DomainService domainService;

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public SchedulerFactoryBean schedulerFactory(Domain domain) {
        // General schema
        if(domain == null) {
            return schedulerFactoryGeneral();
        }

        // Domain
        return schedulerFactoryDomain(domain);
    }

    /**
     * Sets the triggers only for general schema
     * @return Scheduler Factory Bean changed
     */
    private SchedulerFactoryBean schedulerFactoryGeneral() {
        LOG.debug("Instantiating the scheduler factory for general schema");
        SchedulerFactoryBean schedulerFactoryBean = schedulerFactory("General", getGeneralSchemaPrefix(), null);
        final Map<String, Trigger> beansOfType = applicationContext.getBeansOfType(Trigger.class);
        List<Trigger> domibusStandardTriggerList = beansOfType.values().stream()
                                                        .filter( trigger -> trigger instanceof CronTriggerImpl &&
                                                                    ((CronTriggerImpl)trigger).getGroup().equalsIgnoreCase(GROUP_GENERAL))
                                                    .collect(Collectors.toList());
        schedulerFactoryBean.setTriggers(domibusStandardTriggerList.toArray(new Trigger[domibusStandardTriggerList.size()]));
        return schedulerFactoryBean;
    }

    /**
     * Sets the triggers specific only for domain schema
     * @param domain Domain
     * @return Scheduler Factory Bean changed
     */
    private SchedulerFactoryBean schedulerFactoryDomain(Domain domain) {
        LOG.debug("Instantiating the scheduler factory for domain [{}]", domain);
        SchedulerFactoryBean schedulerFactoryBean = schedulerFactory(domainService.getSchedulerName(domain), getTablePrefix(domain), domain);
        //get all the Spring Bean Triggers so that new instances with scope prototype are injected
        final Map<String, Trigger> beansOfType = applicationContext.getBeansOfType(Trigger.class);
        List<Trigger> domibusStandardTriggerList = beansOfType.values().stream()
                                                        .filter( trigger -> !(trigger instanceof CronTriggerImpl) ||
                                                                    !((CronTriggerImpl) trigger).getGroup().equalsIgnoreCase(GROUP_GENERAL))
                                                    .collect(Collectors.toList());
        schedulerFactoryBean.setTriggers(domibusStandardTriggerList.toArray(new Trigger[domibusStandardTriggerList.size()]));
        return schedulerFactoryBean;
    }

    /**
     * Creates a new Scheduler Factory Bean based on {@schedulerName}, {@tablePrefix} and {@domain}
     * @param schedulerName Scheduler Name
     * @param tablePrefix Table Prefix
     * @param domain Domain
     * @return Scheduler Factory Bean
     */
    private SchedulerFactoryBean schedulerFactory(String schedulerName, String tablePrefix, Domain domain) {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setSchedulerName(schedulerName);
        scheduler.setTaskExecutor(executor);
        scheduler.setAutoStartup(false);
        scheduler.setApplicationContext(applicationContext);
        scheduler.setWaitForJobsToCompleteOnShutdown(true);
        scheduler.setOverwriteExistingJobs(true);
        scheduler.setDataSource(dataSource);
        scheduler.setNonTransactionalDataSource(nonTransactionalDataSource);
        scheduler.setTransactionManager(transactionManager);
        Properties properties = new Properties();
        properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        properties.setProperty("org.quartz.jobStore.isClustered", domibusPropertyProvider.getProperty("domibus.deployment.clustered"));
        properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", "20000");
        properties.setProperty("org.quartz.jobStore.useProperties", "false");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.scheduler.jmx.export", "true");
        properties.setProperty("org.quartz.threadExecutor.class", DomibusQuartzThreadExecutor.class.getCanonicalName());

        properties.setProperty("org.quartz.scheduler.instanceName", "general");
        if (StringUtils.isNotEmpty(tablePrefix)) {
            if (domain != null) {
                LOG.debug("Using the Quartz tablePrefix [{}] for domain [{}]", tablePrefix, domain);
            } else {
                LOG.debug("Using the Quartz tablePrefix [{}] for general schema", tablePrefix);
            }
            properties.setProperty("org.quartz.jobStore.tablePrefix", tablePrefix);
        }

        scheduler.setQuartzProperties(properties);
        scheduler.setJobFactory(autowiringSpringBeanJobFactory);

        return scheduler;
    }

    /**
     * Returns the general schema prefix for QRTZ tables
     * @return General schema prefix
     */
    protected String getGeneralSchemaPrefix() {
        return getSchemaPrefix(domainService.getGeneralSchema());
    }

    /**
     * Returns the schema prefix for QRTZ tables for the domain
     * @param domain Domain
     * @return Domain' schema prefix
     */
    protected String getTablePrefix(Domain domain) {
        return getSchemaPrefix(domainService.getDatabaseSchema(domain));
    }

    /**
     * Returns the Schema prefix for a specific schema
     * @param schema Schema
     * @return Schema prefix
     */
    private String getSchemaPrefix(String schema) {
        if (StringUtils.isEmpty(schema)) {
            return null;
        }

        return schema + ".QRTZ_";
    }
}
