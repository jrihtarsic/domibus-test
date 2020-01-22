//package eu.domibus.core.pmode.validation;
//
//import eu.domibus.api.pmode.IssueLevel;
//import org.springframework.stereotype.Component;
//
///**
// * @author Ion Perpegel
// * @since 4.2
// *
// * Validates self party existence by harnessing the xPathValidator
// */
//@Component
//public class SelfPartyValidator extends XPathPModeValidator {
//    private static final String TARGET_EXPR = "//configuration/@party";
//    private static final String ACCEPTED_VALUE_EXPR = "//businessProcesses/parties/party/@name";
//
//    public SelfPartyValidator() {
//        super(TARGET_EXPR, ACCEPTED_VALUE_EXPR, IssueLevel.ERROR, "Party [%s] not found in business process parties.");
//    }
//}
