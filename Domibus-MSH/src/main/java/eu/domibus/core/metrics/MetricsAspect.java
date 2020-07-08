package eu.domibus.core.metrics;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.ext.domain.metrics.Counter;
import eu.domibus.ext.domain.metrics.Default;
import eu.domibus.ext.domain.metrics.MetricNames;
import eu.domibus.ext.domain.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Thomas Dussart
 * @since 4.1
 * <p>
 * This aspect looks after framework annotation timer and counter, and then configure metrics. In this scenario,
 * drop wizard metrics.
 */
@Aspect
@Component
public class MetricsAspect {

    static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MetricsAspect.class);

    @Autowired
    private MetricRegistry metricRegistry;

    @Around("@annotation(timer)")
    public Object surroundWithATimer(ProceedingJoinPoint pjp, Timer timer) throws Throwable {
        com.codahale.metrics.Timer.Context context = null;
        final Class<?> clazz = timer.clazz();
        final MetricNames timerName = timer.value();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();
        LOG.trace("adding a timer with name:[{}] for method:[{}] in class:[{}] and declaring class:[{}]", timerName,method, clazz.getName(),declaringClass.getName());
        com.codahale.metrics.Timer methodTimer = metricRegistry.timer(getMetricsName(clazz, timerName,method,declaringClass,"_timer"));
        try {
            context = methodTimer.time();
            return pjp.proceed();
        } finally {
            if (context != null) {
                context.stop();
            }
        }
    }

    private String getMetricsName(Class<?> clazz, MetricNames timerName, Method method, Class<?> declaringClass,String suffix) {
        if(MetricNames.VOID.equals(timerName)){
            return name(declaringClass, method.getName()+suffix);
        }
        if (Default.class.isAssignableFrom(clazz)) {
            return timerName.name()+suffix;
        } else {
            return name(clazz, timerName.name()+suffix);
        }
    }

    @Around("@annotation(counter)")
    public Object surroundWithACounter(ProceedingJoinPoint pjp, Counter counter) throws Throwable {
        final Class<?> clazz = counter.clazz();
        final MetricNames counterName = counter.value();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();
        LOG.trace("adding a counter with name:[{}] for method:[{}] in class:[{}] and declaring class:[{}]", counterName,method, clazz.getName(),declaringClass.getName());
        com.codahale.metrics.Counter methodCounter = metricRegistry.counter(getMetricsName(clazz, counterName,method,declaringClass,"_counter"));
        try {
            methodCounter.inc();
            return pjp.proceed();
        } finally {
            methodCounter.dec();
        }
    }

    protected String getMetricsName(final Class<?> clazz, final String timerName) {
        if (Default.class.isAssignableFrom(clazz)) {
            return timerName;
        } else {
            return name(clazz, timerName);
        }
    }
}
