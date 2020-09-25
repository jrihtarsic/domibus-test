package eu.domibus.core.message.retention;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Aspect
class TransactionAspect extends TransactionSynchronizationAdapter {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TransactionAspect.class);

    String sig = null;
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void registerTransactionSyncrhonization(ProceedingJoinPoint pjp) {
        if(pjp != null && pjp.getSignature() != null) {
            LOG.error("~~~~~~~~~~~~~~~~~~~~~~~~ registerTransactionSyncrhonization [{}]", pjp.getSignature().getDeclaringTypeName());
            sig = pjp.getSignature().getDeclaringTypeName();
        }
        TransactionSynchronizationManager.registerSynchronization(this);

    }

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void beforeCommit(boolean readOnly) {
        LOG.error("~~~~~~~~~~~~~~~~~~~~~~~~ [{}]", sig);
        startTime.set(System.nanoTime());
    }

    @Override
    public void afterCommit(){
        LOG.error("~~~~~~~~~~~~~~~~~~~~~~ Transaction time: [{}] [{}], [{}]",  (System.nanoTime() - startTime.get())/1000000, (System.nanoTime() - startTime.get()), sig);
    }
}