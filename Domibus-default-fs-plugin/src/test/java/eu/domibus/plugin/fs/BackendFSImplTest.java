package eu.domibus.plugin.fs;

import eu.domibus.common.MessageStatus;
import eu.domibus.common.MessageStatusChangeEvent;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.MessageLister;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@RunWith(JMockit.class)
public class BackendFSImplTest {

    private static final String TEXT_XML = "text/xml";

    @Injectable
    protected MessageRetriever<Submission> messageRetriever;

    @Injectable
    protected MessageSubmitter<Submission> messageSubmitter;

    @Injectable
    private MessageLister lister;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    private FSMessageTransformer defaultTransformer;

    @Injectable
    String name = "fsplugin";

    @Tested
    BackendFSImpl backendFS;

    private FileObject rootDir;

    private FileObject incomingFolder;
    
    private FileObject outgoingFolder;

    @Before
    public void setUp() throws org.apache.commons.vfs2.FileSystemException {
        String location = "ram:///BackendFSImplTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        incomingFolder = rootDir.resolveFile(FSFilesManager.INCOMING_FOLDER);
        incomingFolder.createFolder();
        
        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        outgoingFolder.createFolder();
    }

    @After
    public void tearDown() throws FileSystemException {
        rootDir.close();
        incomingFolder.close();
        outgoingFolder.close();
    }


    @Test
    public void testDeliverMessageNormalFlow(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(payloadContent.getBytes(), TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow", "metadata.xml");

        new Expectations(backendFS) {{
            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(dataHandler, userMessage);

            fsFilesManager.getEnsureChildFolder(withAny(rootDir), FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;
        }};

        backendFS.deliverMessage(messageId);

        // Assert results
        FileObject[] files = incomingFolder.findFiles(new FileTypeSelector(FileType.FILE));
        Assert.assertEquals(1, files.length);
        FileObject fileMessage = files[0];

        Assert.assertEquals(messageId + ".xml", fileMessage.getName().getBaseName());
        Assert.assertEquals(payloadContent, IOUtils.toString(fileMessage.getContent().getInputStream()));
    }

    @Test
    public void testGetMessageSubmissionTransformer() {
        MessageSubmissionTransformer<FSMessage> result = backendFS.getMessageSubmissionTransformer();
        
        Assert.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testGetMessageRetrievalTransformer() {
        MessageRetrievalTransformer<FSMessage> result = backendFS.getMessageRetrievalTransformer();
        
        Assert.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testMessageStatusChanged() throws FSSetUpException, FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        event.setFromStatus(MessageStatus.READY_TO_SEND);
        event.setToStatus(MessageStatus.SEND_ENQUEUED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));
        
        final FileObject contentFile = outgoingFolder.resolveFile("content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.READY_TO_SEND");
        
        new Expectations(1, backendFS) {{
//            unneeded when main location contains file
//            fsPluginProperties.getDomains();
//            result = Collections.emptySet();
            
            fsFilesManager.setUpFileSystem();
            result = rootDir;
            
            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;
            
            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[] { contentFile };
        }};
        
        backendFS.messageStatusChanged(event);
        
        contentFile.close();
        
        new VerificationsInOrder(1) {{
            fsFilesManager.renameFile(contentFile, "content_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml.SEND_ENQUEUED");
        }};
    }

}
