package eu.domibus.common.services.impl;

import eu.domibus.api.audit.AuditLog;
import eu.domibus.common.dao.AuditDao;
import eu.domibus.common.model.common.RevisionLogicalName;
import eu.domibus.common.services.AuditService;
import eu.domibus.common.util.AnnotationsUtil;
import eu.domibus.core.converter.DomainCoreConverter;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart
 * @since 4.0
 * {@inheritDoc}
 * <p>
 * Service in charge of retrieving audit logs, audit targets, etc...
 */
@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditDao auditDao;

    @Autowired
    private DomainCoreConverter domainCoreConverter;

    @Autowired
    private AnnotationsUtil annotationsUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listAudit(
            final Set<String> auditTargets,
            final Set<String> actions,
            final Set<String> users,
            final Date from,
            final Date to,
            final int start,
            final int max) {
        return domainCoreConverter.convert(
                auditDao.listAudit(
                        auditTargets,
                        actions,
                        users,
                        from,
                        to,
                        start,
                        max), AuditLog.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Long countAudit(final Set<String> auditTargetName,
                           final Set<String> action,
                           final Set<String> user,
                           final Date from,
                           final Date to) {
        return auditDao.countAudit(auditTargetName, action, user, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable("auditTarget")
    @Transactional(readOnly = true)
    public List<String> listAuditTarget() {
        Set<Class<?>> typesAnnotatedWith = new Reflections("eu.domibus").
                getTypesAnnotatedWith(RevisionLogicalName.class);
        return typesAnnotatedWith.stream().
                map(aClass -> annotationsUtil.getValue(aClass, RevisionLogicalName.class)).
                //check if present is needed because the set contains subclasses that do not contain the annotation.
                        filter(Optional::isPresent).
                        map(Optional::get).
                        distinct().
                        collect(Collectors.toList());
    }
}
