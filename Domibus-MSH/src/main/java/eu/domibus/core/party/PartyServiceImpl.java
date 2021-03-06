package eu.domibus.core.party;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.party.Party;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.common.dao.PartyDao;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.*;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.crypto.api.CertificateEntry;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.model.MessageExchangePattern;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Service
public class PartyServiceImpl implements PartyService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyServiceImpl.class);
    private static final Predicate<Party> DEFAULT_PREDICATE = condition -> true;

    @Autowired
    private DomainCoreConverter domainCoreConverter;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private PartyDao partyDao;

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected DomainContextProvider domainProvider;

    @Autowired
    protected CertificateService certificateService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Party> getParties(final String name,
                                  final String endPoint,
                                  final String partyId,
                                  final String processName,
                                  final int pageStart,
                                  final int pageSize) {

        final Predicate<Party> searchPredicate = getSearchPredicate(name, endPoint, partyId, processName);
        return linkPartyAndProcesses().
                stream().
                filter(searchPredicate).
                skip(pageStart).
                limit(pageSize).
                collect(Collectors.toList());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findPartyNamesByServiceAndAction(String service, String action) {
        return pModeProvider.findPartyIdByServiceAndAction(service, action, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findPushToPartyNamesByServiceAndAction(String service, String action) {
        List<MessageExchangePattern> meps = new ArrayList<>();
        meps.add(MessageExchangePattern.ONE_WAY_PUSH);
        meps.add(MessageExchangePattern.TWO_WAY_PUSH_PUSH);
        meps.add(MessageExchangePattern.TWO_WAY_PUSH_PULL);
        meps.add(MessageExchangePattern.TWO_WAY_PULL_PUSH);
        return pModeProvider.findPartyIdByServiceAndAction(service, action, meps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGatewayPartyIdentifier() {
        String result = null;
        eu.domibus.common.model.configuration.Party gatewayParty = pModeProvider.getGatewayParty();
        // return the first identifier
        if (!gatewayParty.getIdentifiers().isEmpty()) {
            result = gatewayParty.getIdentifiers().iterator().next().getPartyId();
        }
        return result;
    }

    /**
     * In the actual configuration the link between parties and processes exists from process to party.
     * We need to reverse this association, we want to have a relation party -&gt; process I am involved in as a responder
     * or initiator.
     *
     * @return a list of party linked with their processes.
     */
    protected List<Party> linkPartyAndProcesses() {

        //Retrieve all party entities.
        List<eu.domibus.common.model.configuration.Party> allParties;
        try {
            allParties = pModeProvider.findAllParties();
        } catch (IllegalStateException e) {
            LOG.trace("findAllParties thrown exception: ", e);
            return new ArrayList<>();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Linking party and processes for following parties: ");
            allParties.forEach(party -> LOG.debug("      Party [{}]", party));
        }

        //create a new Party to live outside the service per existing party entity in the pmode.
        List<Party> parties = domainCoreConverter.convert(allParties, Party.class);

        //transform parties to map for convenience.
        final Map<String, Party> partyMapByName =
                parties.
                        stream().
                        collect(collectingAndThen(toMap(Party::getName, Function.identity()), ImmutableMap::copyOf));

        //retrieve all existing processes in the pmode.
        final List<Process> allProcesses =
                pModeProvider.findAllProcesses().
                        stream().
                        collect(collectingAndThen(toList(), ImmutableList::copyOf));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding all processes in the pMode: ");
            allProcesses.forEach(process -> LOG.debug("[{}]", process));
        }

        linkProcessWithPartyAsInitiator(partyMapByName, allProcesses);

        linkProcessWithPartyAsResponder(partyMapByName, allProcesses);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding all parties with initiators and responders: ");
            parties.forEach(party -> printPartyProcesses(party));
        }

        return parties;
    }

    protected void printPartyProcesses(Party party) {
        LOG.debug("Party [{}]", party);
        if (party == null) {
            return;
        }

        if (party.getProcessesWithPartyAsInitiator() != null) {
            LOG.debug("     initiator processes: ");
            party.getProcessesWithPartyAsInitiator().forEach(process -> LOG.debug("[{}]", process));
        }
        if (party.getProcessesWithPartyAsResponder() != null) {
            LOG.debug("     responder processes: ");
            party.getProcessesWithPartyAsResponder().forEach(process -> LOG.debug("[{}]", process));
        }
    }


    protected void linkProcessWithPartyAsInitiator(final Map<String, Party> partyMapByName, final List<Process> allProcesses) {
        allProcesses.forEach(
                processEntity -> {
                    //loop process initiators.
                    processEntity.getInitiatorParties().forEach(partyEntity -> {
                                Party party = partyMapByName.get(partyEntity.getName());
                                eu.domibus.api.process.Process process = domainCoreConverter.convert(
                                        processEntity,
                                        eu.domibus.api.process.Process.class);
                                //add the processes for which this party is initiator.
                                party.addProcessesWithPartyAsInitiator(process);
                            }
                    );
                }
        );
    }

    protected void linkProcessWithPartyAsResponder(final Map<String, Party> partyMapByName, final List<Process> allProcesses) {
        allProcesses.forEach(
                processEntity -> {
                    //loop process responder.
                    processEntity.getResponderParties().forEach(partyEntity -> {
                                Party party = partyMapByName.get(partyEntity.getName());
                                eu.domibus.api.process.Process process = domainCoreConverter.convert(
                                        processEntity,
                                        eu.domibus.api.process.Process.class);
                                //add the processes for which this party is responder.
                                party.addprocessesWithPartyAsResponder(process);
                            }
                    );
                }
        );
    }

    protected Predicate<Party> getSearchPredicate(String name, String endPoint, String partyId, String processName) {
        return namePredicate(name).
                and(endPointPredicate(endPoint)).
                and(partyIdPredicate(partyId)).
                and(processPredicate(processName));
    }

    protected Predicate<Party> namePredicate(final String name) {

        if (StringUtils.isNotEmpty(name)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("create name predicate for [{}]", name);
            }
            return
                    party ->
                            StringUtils.containsIgnoreCase(party.getName(), name.toUpperCase());

        }
        return DEFAULT_PREDICATE;

    }

    protected Predicate<Party> endPointPredicate(final String endPoint) {
        if (StringUtils.isNotEmpty(endPoint)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("create endPoint predicate for [{}]", endPoint);
            }
            return party ->
                    StringUtils.containsIgnoreCase(party.getEndpoint(), endPoint.toUpperCase());
        }
        return DEFAULT_PREDICATE;
    }

    protected Predicate<Party> partyIdPredicate(final String partyId) {
        //Search in the list of partyId to find one that match the search criteria.
        if (StringUtils.isNotEmpty(partyId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("create partyId predicate for [{}]", partyId);
            }
            return
                    party -> {
                        long count = party.getIdentifiers().stream().
                                filter(identifier -> StringUtils.containsIgnoreCase(identifier.getPartyId(), partyId)).count();
                        return count > 0;
                    };
        }
        return DEFAULT_PREDICATE;
    }

    protected Predicate<Party> processPredicate(final String processName) {
        //Search in the list of process for which this party is initiator and the one for which this party is a responder.
        if (StringUtils.isNotEmpty(processName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("create process predicate for [{}]", processName);
            }
            return
                    party -> {
                        long count = party.getProcessesWithPartyAsInitiator().stream().
                                filter(process -> StringUtils.containsIgnoreCase(process.getName(), processName)).count();
                        count += party.getProcessesWithPartyAsResponder().stream().
                                filter(process -> StringUtils.containsIgnoreCase(process.getName(), processName)).count();
                        return count > 0;
                    };
        }

        return DEFAULT_PREDICATE;
    }

    protected static class ReplacementResult {
        private final List<eu.domibus.common.model.configuration.Party> removedParties = new ArrayList<>();

        private final Configuration updatedConfiguration;

        public ReplacementResult(Configuration updatedConfiguration, List<eu.domibus.common.model.configuration.Party> removedParties) {
            this.updatedConfiguration = updatedConfiguration;
            this.removedParties.addAll(removedParties);
        }

        public ReplacementResult(Configuration updatedConfiguration) {
            this.updatedConfiguration = updatedConfiguration;
        }

        public Configuration getUpdatedConfiguration() {
            return updatedConfiguration;
        }

        public List<eu.domibus.common.model.configuration.Party> getRemovedParties() {
            return Collections.unmodifiableList(removedParties);
        }

        public void addRemovedParty(eu.domibus.common.model.configuration.Party party) {
            this.removedParties.add(party);
        }

        public void addRemovedParties(eu.domibus.common.model.configuration.Party... parties) {
            addRemovedParties(Arrays.asList(parties));
        }

        public void addRemovedParties(List<eu.domibus.common.model.configuration.Party> parties) {
            this.removedParties.addAll(parties);
        }

        public void clearRemovedParties() {
            this.removedParties.clear();
        }
    }

    protected ReplacementResult replaceParties(List<Party> partyList, Configuration configuration) {
        List<eu.domibus.common.model.configuration.Party> newParties = domainCoreConverter.convert(partyList, eu.domibus.common.model.configuration.Party.class);

        List<eu.domibus.common.model.configuration.Party> removedParties = updateConfigurationParties(newParties, configuration);

        updatePartyIdTypes(newParties, configuration);

        updateProcessConfiguration(partyList, configuration);

        return new ReplacementResult(configuration, removedParties);
    }

    private List<eu.domibus.common.model.configuration.Party> updateConfigurationParties(List<eu.domibus.common.model.configuration.Party> newParties, Configuration configuration) {
        BusinessProcesses businessProcesses = configuration.getBusinessProcesses();
        Parties parties = businessProcesses.getPartiesXml();

        List<eu.domibus.common.model.configuration.Party> removedParties = parties.getParty().stream()
                .filter(existingP -> !newParties.stream().anyMatch(newP -> newP.getName().equals(existingP.getName())))
                .collect(toList());
        preventGatewayPartyRemoval(removedParties);

        parties.getParty().clear();
        parties.getParty().addAll(newParties);
        return removedParties;
    }

    private void preventGatewayPartyRemoval(List<eu.domibus.common.model.configuration.Party> removedParties) {
        String partyMe = pModeProvider.getGatewayParty().getName();
        if (removedParties.stream().anyMatch(party -> party.getName().equals(partyMe))) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_003, "Cannot delete the party describing the current system. ");
        }
    }

    private void updatePartyIdTypes(List<eu.domibus.common.model.configuration.Party> newParties, Configuration configuration) {
        BusinessProcesses businessProcesses = configuration.getBusinessProcesses();
        Parties parties = businessProcesses.getPartiesXml();
        PartyIdTypes partyIdTypes = parties.getPartyIdTypes();
        List<PartyIdType> partyIdType = partyIdTypes.getPartyIdType();

        newParties.forEach(party -> {
            party.getIdentifiers().forEach(identifier -> {
                if (!partyIdType.contains(identifier.getPartyIdType())) {
                    partyIdType.add(identifier.getPartyIdType());
                }
            });
        });
    }

    private void updateProcessConfiguration(List<Party> partyList, Configuration configuration) {
        BusinessProcesses businessProcesses = configuration.getBusinessProcesses();
        List<Process> processes = businessProcesses.getProcesses();

        processes.forEach(process -> {
            updateProcessConfigurationInitiatorParties(partyList, process);
            updateProcessConfigurationResponderParties(partyList, process);
        });
    }

    private void updateProcessConfigurationResponderParties(List<Party> partyList, Process process) {
        Set<String> rParties = partyList.stream()
                .filter(p -> p.getProcessesWithPartyAsResponder().stream()
                        .anyMatch(pp -> process.getName().equalsIgnoreCase(pp.getName())))
                .map(p -> p.getName())
                .collect(Collectors.toSet());

        if (process.getResponderPartiesXml() == null) {
            process.setResponderPartiesXml(new ResponderParties());
        }
        List<ResponderParty> rp = process.getResponderPartiesXml().getResponderParty();
        rp.removeIf(x -> !rParties.contains(x.getName()));
        rp.addAll(rParties.stream().filter(name -> rp.stream().noneMatch(x -> name.equalsIgnoreCase(x.getName())))
                .map(name -> {
                    ResponderParty y = new ResponderParty();
                    y.setName(name);
                    return y;
                }).collect(Collectors.toSet()));
        if (rp.isEmpty()) {
            process.setResponderPartiesXml(null);
        }
    }

    private void updateProcessConfigurationInitiatorParties(List<Party> partyList, Process process) {
        Set<String> iParties = partyList.stream()
                .filter(p -> p.getProcessesWithPartyAsInitiator().stream()
                        .anyMatch(pp -> process.getName().equalsIgnoreCase(pp.getName())))
                .map(p -> p.getName())
                .collect(Collectors.toSet());

        if (process.getInitiatorPartiesXml() == null) {
            process.setInitiatorPartiesXml(new InitiatorParties());
        }
        List<InitiatorParty> ip = process.getInitiatorPartiesXml().getInitiatorParty();
        ip.removeIf(x -> !iParties.contains(x.getName()));
        ip.addAll(iParties.stream().filter(name -> ip.stream().noneMatch(x -> name != null && name.equalsIgnoreCase(x.getName())))
                .map(name -> {
                    InitiatorParty y = new InitiatorParty();
                    y.setName(name);
                    return y;
                }).collect(Collectors.toSet()));
        if (ip.isEmpty()) {
            process.setInitiatorPartiesXml(null);
        }
    }

    @Override
    public void updateParties(List<Party> partyList, Map<String, String> partyToCertificateMap) {
        final PModeArchiveInfo pModeArchiveInfo = pModeProvider.getRawConfigurationList().stream().findFirst().orElse(null);
        if (pModeArchiveInfo == null) {
            throw new IllegalStateException("Could not update PMode parties: PMode not found!");
        }

        ConfigurationRaw rawConfiguration = pModeProvider.getRawConfiguration(pModeArchiveInfo.getId());

        Configuration configuration;
        try {
            configuration = pModeProvider.getPModeConfiguration(rawConfiguration.getXml());
        } catch (XmlProcessingException e) {
            LOG.error("Error reading current PMode", e);
            throw new IllegalStateException(e);
        }

        ReplacementResult replacementResult = replaceParties(partyList, configuration);

        updateConfiguration(rawConfiguration.getConfigurationDate(), replacementResult.getUpdatedConfiguration());

        updatePartyCertificate(partyToCertificateMap, replacementResult);
    }

    private void updateConfiguration(Date configurationDate, Configuration updatedConfiguration) {
        ZonedDateTime confDate = ZonedDateTime.ofInstant(configurationDate.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ssO");
        String updatedDescription = "Updated parties to version of " + confDate.format(formatter);

        try {
            byte[] updatedPMode = pModeProvider.serializePModeConfiguration(updatedConfiguration);
            pModeProvider.updatePModes(updatedPMode, updatedDescription);
        } catch (XmlProcessingException e) {
            LOG.error("Error writing current PMode", e);
            throw new IllegalStateException(e);
        }
    }

    protected void updatePartyCertificate(Map<String, String> partyToCertificateMap, ReplacementResult replacementResult) {
        Domain currentDomain = domainProvider.getCurrentDomain();
        List<String> aliases = getRemovedParties(replacementResult);
        if (CollectionUtils.isNotEmpty(aliases)) {
            multiDomainCertificateProvider.removeCertificate(currentDomain, aliases);
        }
        List<CertificateEntry> certificates = new ArrayList<>();
        for (Map.Entry<String, String> pair : partyToCertificateMap.entrySet()) {
            if (pair.getValue() == null) {
                continue;
            }

            String partyName = pair.getKey();
            String certificateContent = pair.getValue();
            try {
                X509Certificate cert = certificateService.loadCertificateFromString(certificateContent);
                certificates.add(new CertificateEntry(partyName, cert));
            } catch (DomibusCertificateException e) {
                LOG.error("Error deserializing certificate", e);
                throw new IllegalStateException(e);
            }
        }
        if (CollectionUtils.isNotEmpty(certificates)) {
            multiDomainCertificateProvider.addCertificate(currentDomain, certificates, true);
        }
    }

    protected List<String> getRemovedParties(ReplacementResult replacementResult) {
        return replacementResult.getRemovedParties().stream().map(party -> party.getName()).collect(toList());
    }

    @Override
    public List<eu.domibus.api.process.Process> getAllProcesses() {
        //Retrieve all processes, needed in UI console to be able to check
        List<eu.domibus.common.model.configuration.Process> allProcesses;
        try {
            allProcesses = pModeProvider.findAllProcesses();
        } catch (IllegalStateException e) {
            return new ArrayList<>();
        }
        List<eu.domibus.api.process.Process> processes = domainCoreConverter.convert(allProcesses, eu.domibus.api.process.Process.class);
        return processes;
    }

}
