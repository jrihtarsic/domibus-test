package eu.domibus.web.rest;

import eu.domibus.api.audit.AuditLog;
import eu.domibus.api.csv.CsvException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.model.common.ModificationType;
import eu.domibus.common.services.AuditService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.csv.CsvCustomColumns;
import eu.domibus.core.csv.CsvExcludedItems;
import eu.domibus.core.csv.CsvService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.AuditFilterRequestRO;
import eu.domibus.web.rest.ro.AuditResponseRo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart
 * @since 4.0
 * <p>
 * Rest entry point to retrieve the audit logs.
 */
@RestController
@RequestMapping(value = "/rest/audit")
@Validated
public class AuditResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AuditResource.class);

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    private AuditService auditService;

    @Autowired
    DateUtil dateUtil;

    @Autowired
    CsvServiceImpl csvServiceImpl;

    /**
     * Entry point of the Audit rest service to list the system audit logs.
     *
     * @param auditCriteria the audit criteria used to filter the returned list.
     * @return an audit list.
     */
    @RequestMapping(value = {"/list"}, method = RequestMethod.POST)
    public List<AuditResponseRo> listAudits(@RequestBody @Valid AuditFilterRequestRO auditCriteria) {
        LOG.debug("Audit criteria received:" + auditCriteria.toString());

        List<AuditLog> sourceList = auditService.listAudit(
                auditCriteria.getAuditTargetName(),
                changeActionType(auditCriteria.getAction()),
                auditCriteria.getUser(),
                auditCriteria.getFrom(),
                auditCriteria.getTo(),
                auditCriteria.getStart(),
                auditCriteria.getMax());

        return domainConverter.convert(sourceList, AuditResponseRo.class);
    }


    @RequestMapping(value = {"/count"}, method = RequestMethod.POST)
    public Long countAudits(@RequestBody @Valid AuditFilterRequestRO auditCriteria) {
        return auditService.countAudit(
                auditCriteria.getAuditTargetName(),
                changeActionType(auditCriteria.getAction()),
                auditCriteria.getUser(),
                auditCriteria.getFrom(),
                auditCriteria.getTo());
    }

    /**
     * Action type send from the admin console are different from the one used in the database.
     * Eg: In the admin console the filter for a modified entity is Modified where in the database a modified reccord
     * has the MOD flag. This method does the translation.
     *
     * @param actions
     * @return
     */
    private Set<String> changeActionType(Set<String> actions) {
        Set<String> modificationTypes = new HashSet<>();
        if (actions == null || actions.isEmpty()) {
            return modificationTypes;
        }
        actions.forEach(action -> {
            Set<String> collect = Arrays.stream(ModificationType.values()).
                    filter(modificationType -> modificationType.getLabel().equals(action)).
                    map(Enum::name).
                    collect(Collectors.toSet());
            modificationTypes.addAll(collect);
        });
        return modificationTypes;
    }

    @RequestMapping(value = {"/targets"}, method = RequestMethod.GET)
    public List<String> auditTargets() {
        return auditService.listAuditTarget();
    }

    /**
     * This method returns a CSV file with the contents of Audit table
     *
     * @param auditCriteria same filter criteria as in filter method
     * @return CSV file with the contents of Audit table
     */
    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(@Valid AuditFilterRequestRO auditCriteria) {
        String resultText;
        auditCriteria.setStart(0);
        auditCriteria.setMax(csvServiceImpl.getMaxNumberRowsToExport());
        final List<AuditResponseRo> auditResponseRos = listAudits(auditCriteria);

        try {
            resultText = csvServiceImpl.exportToCSV(auditResponseRos, AuditResponseRo.class,
                    CsvCustomColumns.AUDIT_RESOURCE.getCustomColumns(), CsvExcludedItems.AUDIT_RESOURCE.getExcludedItems());
        } catch (CsvException e) {
            LOG.error("Exception caught during export to CSV", e);
            return ResponseEntity.noContent().build();
        }

        return csvServiceImpl.getResponseEntity(resultText, "audit");
    }
}
