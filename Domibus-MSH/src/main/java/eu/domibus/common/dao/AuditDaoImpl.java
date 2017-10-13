package eu.domibus.common.dao;

import eu.domibus.common.model.audit.Audit;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Repository
public class AuditDaoImpl implements AuditDao {

    @PersistenceContext(unitName = "domibusJTA")
    private EntityManager entityManager;

    @Override
    public List<Audit> listAudit(Set<String> auditTargets,
                                 Set<String> actions,
                                 Set<String> users,
                                 Date from,
                                 Date to,
                                 int start,
                                 int max) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Audit> criteriaQuery = criteriaBuilder.createQuery(Audit.class);
        Root<Audit> root = criteriaQuery.from(Audit.class);
        criteriaQuery.select(root);
        if (CollectionUtils.isNotEmpty(actions)) {
            Path<Object> actionField = root.get("actions");
            Predicate in = actionField.in(actions);
            criteriaQuery.where(in);
        }
        if (CollectionUtils.isNotEmpty(auditTargets)) {
            Path<Object> auditTargetField = root.get("auditTargetName");
            Predicate in = auditTargetField.in(auditTargets);
            criteriaQuery.where(in);
        }
        if (CollectionUtils.isNotEmpty(users)) {
            Path<Object> userField = root.get("users");
            Predicate in = userField.in(users);
            criteriaQuery.where(in);
        }
        if (from != null) {
            Path<Date> changedDate = root.get("changed");
            criteriaBuilder.greaterThanOrEqualTo(changedDate, from);
        }
        if (to != null) {
            Path<Date> changedDate = root.get("changed");
            criteriaBuilder.lessThanOrEqualTo(changedDate, to);
        }

        TypedQuery<Audit> query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(start);
        query.setMaxResults(max);
        return query.getResultList();
    }
}
