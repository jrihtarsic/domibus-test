package eu.domibus.core.message.retention;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DomibusTransactionEventListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusTransactionEventListener.class);

    @EventListener
    public void logBeforeCommit(ApplicationEvent event) {
        if(event == null)
            return;
        LOG.info(" ~~~~~~~~~~~~~~~~ Handling event inside a transaction BEFORE COMMIT. [{}] [{}] [{}]", event.getClass(), event.getTimestamp(), event.getSource());
    }

    @EventListener
    public void logAfterCommit(ApplicationEvent event) {
        if(event == null)
            return;
        LOG.info("~~~~~~~~~~~~~~~~ Handling event inside a transaction AFTER COMMIT. [{}] [{}] [{}]", event.getClass(), event.getTimestamp(), event.getSource());
    }
}
