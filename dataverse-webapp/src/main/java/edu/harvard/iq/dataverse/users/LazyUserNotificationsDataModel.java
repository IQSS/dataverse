package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.notification.dto.UserNotificationDTO;
import edu.harvard.iq.dataverse.notification.dto.UserNotificationMapper;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationQuery;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationQueryResult;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.util.PrimefacesUtil;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LazyUserNotificationsDataModel extends LazyDataModel<UserNotificationDTO> {

    private final AuthenticatedUser authenticatedUser;
    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationMapper userNotificationMapper;
    private final boolean markViewedAsRead;
    private Map<String, UserNotificationDTO> notifications;

    // -------------------- CONSTRUCTORS --------------------

    public LazyUserNotificationsDataModel(AuthenticatedUser authenticatedUser, UserNotificationRepository userNotificationRepository, UserNotificationMapper userNotificationMapper, boolean markViewedAsRead) {
        this.userNotificationRepository = userNotificationRepository;
        this.authenticatedUser = authenticatedUser;
        this.userNotificationMapper = userNotificationMapper;
        this.markViewedAsRead = markViewedAsRead;
    }

    // -------------------- LOGIC --------------------

    @Override
    public UserNotificationDTO getRowData(String rowId) {
        return notifications.get(rowId);
    }

    @Override
    public Object getRowKey(UserNotificationDTO notification) {
        return notification.getId().toString();
    }

    @Override
    public List<UserNotificationDTO> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, FilterMeta> filterMeta) {
        String filterValue = getGlobalFilterValue(filterMeta);

        notifications = new HashMap<>();
        List<UserNotificationDTO> notificationDTOList = new ArrayList<>();

        UserNotificationQueryResult result = userNotificationRepository.query(UserNotificationQuery.newQuery()
                .withUserId(authenticatedUser.getId())
                .withSearchLabel(filterValue)
                .withOffset(first)
                .withResultLimit(pageSize)
                .withAscending(sortOrder == SortOrder.ASCENDING));

        for (UserNotification notification : result.getResult()) {
            UserNotificationDTO dto = userNotificationMapper.toDTO(notification);
            notifications.put(notification.getId().toString(), dto);
            notificationDTOList.add(dto);

            if (!notification.isReadNotification() && markViewedAsRead) {
                notification.setReadNotification(true);
                userNotificationRepository.save(notification);
            }
        }

        this.setRowCount(result.getTotalCount().intValue());

        return notificationDTOList;
    }

    // -------------------- PRIVATE --------------------

    private String getGlobalFilterValue(Map<String, FilterMeta> filterMeta) {
        FilterMeta globalFilter = filterMeta.getOrDefault(PrimefacesUtil.GLOBAL_FILTER_KEY, null);
        return Objects.toString(globalFilter != null && globalFilter.getFilterValue() != null ? globalFilter.getFilterValue() : StringUtils.EMPTY);
    }
}
