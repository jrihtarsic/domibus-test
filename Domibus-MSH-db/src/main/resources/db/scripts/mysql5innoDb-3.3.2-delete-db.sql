-- *********************************************************************
-- Delete script for Domibus DB with a time interval
-- Change START_DATE and END_DATE values accordingly
-- *********************************************************************
SET @START_DATE='2017-01-20 10:00:00', @END_DATE='2017-12-20 15:00:00';

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM TB_MESSAGING where
            (SIGNAL_MESSAGE_ID IN
            (select ID_PK from TB_SIGNAL_MESSAGE WHERE messageInfo_ID_PK IN
            (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE))));

DELETE FROM TB_MESSAGING where
            (USER_MESSAGE_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK IN
            (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE))));

DELETE FROM TB_ERROR_LOG where
            (ERROR_SIGNAL_MESSAGE_ID IN
            (SELECT MESSAGE_ID FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_ERROR_LOG where
            (MESSAGE_IN_ERROR_ID IN
            (SELECT MESSAGE_ID FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_PARTY_ID WHERE FROM_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_PARTY_ID WHERE TO_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_RECEIPT_DATA WHERE RECEIPT_ID IN
            (select ID_PK from TB_RECEIPT WHERE ID_PK IN
            (select receipt_ID_PK from TB_SIGNAL_MESSAGE WHERE messageInfo_ID_PK IN
            (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE))));

DELETE FROM TB_PROPERTY WHERE MESSAGEPROPERTIES_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_PROPERTY WHERE PARTPROPERTIES_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_PART_INFO WHERE PAYLOADINFO_ID IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_RAWENVELOPE_LOG WHERE USERMESSAGE_ID_FK IN
            (select ID_PK from TB_USER_MESSAGE WHERE messageInfo_ID_PK IN
            (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE)));

DELETE FROM TB_RAWENVELOPE_LOG WHERE SIGNALMESSAGE_ID_FK IN
            (select ID_PK from TB_SIGNAL_MESSAGE WHERE messageInfo_ID_PK IN
            (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE)));

DELETE FROM TB_ERROR WHERE SIGNALMESSAGE_ID IN (select ID_PK from TB_SIGNAL_MESSAGE WHERE messageInfo_ID_PK IN (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE)));

DELETE FROM TB_USER_MESSAGE WHERE messageInfo_ID_PK in (select ID_PK from TB_MESSAGE_INFO WHERE TIME_STAMP BETWEEN @START_DATE and @END_DATE);

DELETE FROM TB_SIGNAL_MESSAGE WHERE messageInfo_ID_PK IN (select ID_PK from TB_MESSAGE_INFO WHERE MESSAGE_ID in
            (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE));

DELETE FROM TB_RECEIPT WHERE ID_PK NOT IN(select receipt_ID_PK from TB_SIGNAL_MESSAGE);

DELETE FROM TB_MESSAGE_INFO WHERE MESSAGE_ID in (SELECT message_id FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE);

DELETE FROM TB_MESSAGE_LOG WHERE received BETWEEN @START_DATE and @END_DATE;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 1;

COMMIT;