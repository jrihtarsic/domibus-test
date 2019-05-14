package eu.domibus.plugin.routing;

import eu.domibus.api.routing.BackendFilter;
import eu.domibus.api.routing.RoutingCriteria;
import eu.domibus.common.exception.ConfigurationException;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.plugin.NotificationListener;
import eu.domibus.plugin.routing.dao.BackendFilterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Walczac
 */
@Service
public class RoutingService {

    @Autowired
    private BackendFilterDao backendFilterDao;

    @Autowired
    private List<NotificationListener> notificationListeners;

    @Autowired
    private DomainCoreConverter coreConverter;

    /**
     * Returns the configured backend filters present in the classpath
     *
     * @return The configured backend filters
     */
    @Cacheable(value = "backendFilterCache")
    public List<BackendFilter> getBackendFilters() {
        return getBackendFiltersUncached();
    }

    public List<BackendFilter> getBackendFiltersUncached() {
        final List<BackendFilterEntity> filters = new ArrayList<>(backendFilterDao.findAll());
        final List<NotificationListener> backendsTemp = new ArrayList<>(notificationListeners);

        for (BackendFilterEntity filter : filters) {
            for (final NotificationListener backend : backendsTemp) {
                if (filter.getBackendName().equalsIgnoreCase(backend.getBackendName())) {
                    backendsTemp.remove(backend);
                    break;
                }
            }
        }

        for (final NotificationListener backend : backendsTemp) {
            final BackendFilterEntity filter = new BackendFilterEntity();
            filter.setBackendName(backend.getBackendName());
            filters.add(filter);
        }
        return coreConverter.convert(filters, BackendFilter.class);
    }

    @CacheEvict(value = "backendFilterCache", allEntries = true)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_AP_ADMIN')")
    public void updateBackendFilters(final List<BackendFilter> filters) {

        validateFilters(filters);

        List<BackendFilterEntity> backendFilterEntities = coreConverter.convert(filters, BackendFilterEntity.class);
        List<BackendFilterEntity> allBackendFilterEntities = backendFilterDao.findAll();
        List<BackendFilterEntity> backendFilterEntityListToDelete = backendFiltersToDelete(allBackendFilterEntities, backendFilterEntities);
        backendFilterDao.deleteAll(backendFilterEntityListToDelete);
        backendFilterDao.update(backendFilterEntities);
    }

    private void validateFilters(List<BackendFilter> filters) {
        filters.forEach(filter -> {
            if (filters.stream().anyMatch(f -> f != filter
                    && f.getBackendName().equals(filter.getBackendName())
                    && areEqual(f.getRoutingCriterias(), filter.getRoutingCriterias()))) {
                throw new ConfigurationException("Two message filters cannot have the same criteria.");
            }
        });
    }

    private boolean areEqual(List<RoutingCriteria> c1, List<RoutingCriteria> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        for (RoutingCriteria cr1 : c1) {
            if (!c2.stream().anyMatch(cr2 -> cr2.getName().equals(cr1.getName()) && cr2.getExpression().equals(cr1.getExpression()))) {
                return false;
            }
        }
        return true;
    }

    private List<BackendFilterEntity> backendFiltersToDelete(final List<BackendFilterEntity> masterData, final List<BackendFilterEntity> newData) {
        List<BackendFilterEntity> result = new ArrayList<>(masterData);
        result.removeAll(newData);
        return result;
    }
}
