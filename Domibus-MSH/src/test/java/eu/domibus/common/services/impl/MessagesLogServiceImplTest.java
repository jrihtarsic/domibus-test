package eu.domibus.common.services.impl;

import eu.domibus.common.dao.SignalMessageLogDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.logging.MessageLog;
import eu.domibus.common.model.logging.MessageLogInfo;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.ebms3.common.model.MessageType;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MessagesLogServiceImplTest {

    @Tested
    private MessagesLogServiceImpl messagesLogServiceImpl;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private SignalMessageLogDao signalMessageLogDao;

    @Injectable
    private DomainCoreConverter domainConverter;

    @Test
    public void findMessageLogsTest1() {
        int page = 1, size = 20;
        String column = "col1";
        boolean asc = true;
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageType", MessageType.USER_MESSAGE);

        List<? extends MessageLog> res = messagesLogServiceImpl.findMessageLogs(page, size, column, asc, filters);

        new Verifications() {{
            userMessageLogDao.findPaged(size * (page - 1), size, column, asc, filters);
            times = 1;
            signalMessageLogDao.findPaged(size * (page - 1), size, column, asc, filters);
            times = 0;
        }};
    }

    @Test
    public void findMessageLogsTest2() {
        int page = 1, size = 20;
        String column = "col1";
        boolean asc = true;
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageType", MessageType.SIGNAL_MESSAGE);

        List<? extends MessageLog> res = messagesLogServiceImpl.findMessageLogs(page, size, column, asc, filters);

        new Verifications() {{
            userMessageLogDao.findPaged(size * (page - 1), size, column, asc, filters);
            times = 0;
            signalMessageLogDao.findPaged(size * (page - 1), size, column, asc, filters);
            times = 1;
        }};
    }

    @Test
    public void countMessagesTest1() {
        int size = 20;
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageType", MessageType.USER_MESSAGE);

        new Expectations() {{
            userMessageLogDao.countMessages(filters);
            result = 50;
        }};

        Long pages = messagesLogServiceImpl.countMessages(size, filters);

        new Verifications() {{
            userMessageLogDao.countMessages(filters);
            times = 1;
            signalMessageLogDao.countMessages(filters);
            times = 0;
        }};
        Assert.assertEquals(Long.valueOf(3), pages);
    }

    @Test
    public void countMessagesTest2() {
        int size = 0;
        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageType", MessageType.SIGNAL_MESSAGE);

        new Expectations() {{
            signalMessageLogDao.countMessages(filters);
            result = 15;
        }};

        Long pages = messagesLogServiceImpl.countMessages(size, filters);

        new Verifications() {{
            userMessageLogDao.countMessages(filters);
            times = 0;
            signalMessageLogDao.countMessages(filters);
            times = 1;
        }};

        Assert.assertEquals(Long.valueOf(2), pages);
    }

    @Test
    public void countAndFindPagedTest1() {
        int from = 1, max = 20;
        String column = "col1";
        boolean asc = true;
        MessageType messageType = MessageType.USER_MESSAGE;
        HashMap<String, Object> filters = new HashMap<>();
        int numberOfUserMessageLogs = 1;
        MessageLogInfo item1 = new MessageLogInfo();
        List<MessageLogInfo> resultList = Arrays.asList(item1);

        new Expectations() {{
            userMessageLogDao.countAllInfo(asc, filters);
            result = numberOfUserMessageLogs;
            userMessageLogDao.findAllInfoPaged(from, max, column, asc, filters);
            result = resultList;
        }};

        MessageLogResultRO res = messagesLogServiceImpl.countAndFindPaged(messageType, from, max, column, asc, filters);

        new Verifications() {{
            userMessageLogDao.countAllInfo(asc, filters);
            times = 1;
            userMessageLogDao.findAllInfoPaged(from, max, column, asc, filters);
            times = 1;
        }};

        Assert.assertEquals(Integer.valueOf(numberOfUserMessageLogs), res.getCount());
    }

    @Test
    public void countAndFindPagedTest2() {
        int from = 2, max = 30;
        String column = "col1";
        boolean asc = true;
        MessageType messageType = MessageType.SIGNAL_MESSAGE;
        HashMap<String, Object> filters = new HashMap<>();
        int numberOfLogs = 2;
        MessageLogInfo item1 = new MessageLogInfo();
        List<MessageLogInfo> resultList = Arrays.asList(item1);

        new Expectations() {{
            signalMessageLogDao.countAllInfo(asc, filters);
            result = numberOfLogs;
            signalMessageLogDao.findAllInfoPaged(from, max, column, asc, filters);
            result = resultList;
        }};

        MessageLogResultRO res = messagesLogServiceImpl.countAndFindPaged(messageType, from, max, column, asc, filters);

        new Verifications() {{
            signalMessageLogDao.countAllInfo(asc, filters);
            times = 1;
            signalMessageLogDao.findAllInfoPaged(from, max, column, asc, filters);
            times = 1;
        }};

        Assert.assertEquals(Integer.valueOf(numberOfLogs), res.getCount());
    }

    @Test
    public void convertMessageLogInfo() {
    }
}