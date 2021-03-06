package eu.domibus.core.multitenancy;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.multitenancy.UserDomain;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.user.User;
import eu.domibus.common.converters.UserConverter;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.services.DomibusCacheService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.core.multitenancy.dao.UserDomainEntity;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * @author Ion Perpegel(nperpion)
 * @since 4.0
 */
public class UserDomainServiceMultiDomainImpl implements UserDomainService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserDomainServiceMultiDomainImpl.class);

    @Autowired
    protected DomainTaskExecutor domainTaskExecutor;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    protected UserDomainDao userDomainDao;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected UserConverter userConverter;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusCacheService domibusCacheService;

    @Autowired
    protected DomainCoreConverter domainCoreConverter;

    /**
     * Get the domain associated to the provided user from the general schema. <br>
     * This is done in a separate thread as the DB connection is cached per thread and cannot be changed anymore to the schema of the associated domain
     *
     * @return the domain code of the user
     */
    @Cacheable(value = DomibusCacheService.USER_DOMAIN_CACHE, key = "#user")
    @Override
    public String getDomainForUser(String user) {
        LOG.debug("Searching domain for user [{}]", user);
        String domain = domainTaskExecutor.submit(() -> userDomainDao.findDomainByUser(user));
        LOG.debug("Found domain [{}] for user [{}]", domain, user);
        return domain;
    }

    /**
     * Get the preferred domain associated to the super user from the general schema. <br>
     * This is done in a separate thread as the DB connection is cached per thread and cannot be changed anymore to the schema of the associated domain
     *
     * @return the code of the preferred domain of a super user
     */
    @Cacheable(value = DomibusCacheService.PREFERRED_USER_DOMAIN_CACHE, key = "#user")
    @Override
    public String getPreferredDomainForUser(String user) {
        LOG.debug("Searching preferred domain for user [{}]", user);
        String domain = domainTaskExecutor.submit(() -> userDomainDao.findPreferredDomainByUser(user));
        LOG.debug("Found preferred domain [{}] for user [{}]", domain, user);
        return domain;
    }

    /**
     * Get all super users from the general schema. <br>
     * This is done in a separate thread as the DB connection is cached per thread and cannot be changed anymore to the schema of the associated domain
     *
     * @return the list of users from the general schema
     */
    @Override
    public List<User> getSuperUsers() {
        LOG.debug("Searching for super users");
        return domainTaskExecutor.submit(() -> {
            List<eu.domibus.common.model.security.User> userEntities = userDao.listUsers();
            List<User> users = userConverter.convert(userEntities);

            // fill in preferred domain
            List<UserDomainEntity> domains = userDomainDao.listPreferredDomains();
            users.forEach(u -> {
                String domainCode = domains.stream().filter(d -> d.getUserName().equals(u.getUserName()))
                        .map(d -> d.getPreferredDomain()).findFirst().orElse(null);
                u.setDomain(domainCode);
            });
            return users;
        });
    }


    @Override
    public void setDomainForUser(String user, String domainCode) {
        LOG.debug("Setting domain [{}] for user [{}]", domainCode, user);

        domainTaskExecutor.submit(() -> {
            userDomainDao.setDomainByUser(user, domainCode);
            domibusCacheService.clearCache(DomibusCacheService.USER_DOMAIN_CACHE);
            return null;
        });
    }

    @Override
    public void setPreferredDomainForUser(String user, String domainCode) {
        LOG.debug("Setting preferred domain [{}] for user [{}]", domainCode, user);

        domainTaskExecutor.submit(() -> {
            userDomainDao.setPreferredDomainByUser(user, domainCode);
            domibusCacheService.clearCache(DomibusCacheService.PREFERRED_USER_DOMAIN_CACHE);
            return null;
        });
    }

    @Override
    public void deleteDomainForUser(String user) {
        LOG.debug("Deleting domain for user [{}]", user);

        domainTaskExecutor.submit(() -> {
            userDomainDao.deleteDomainByUser(user);
            domibusCacheService.clearCache(DomibusCacheService.USER_DOMAIN_CACHE);
            return null;
        });
    }

}
