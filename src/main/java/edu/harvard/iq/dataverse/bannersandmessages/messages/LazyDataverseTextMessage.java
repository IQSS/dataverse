package edu.harvard.iq.dataverse.bannersandmessages.messages;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseLocalizedMessageDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Stateful
public class LazyDataverseTextMessage extends LazyDataModel<DataverseTextMessageDto> {

    @EJB
    private DataverseTextMessageServiceBean dataverseTextMessageService;

    @Inject
    private DataverseMessagesMapper mapper;

    @Inject
    private SettingsWrapper settingsWrapper;

    private Long dataverseId;
    private List<DataverseTextMessageDto> dataverseTextMessageDtos;

    @Override
    public List<DataverseTextMessageDto> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        Optional<List<DataverseTextMessage>> dataverseTextMessages =
                Optional.ofNullable(dataverseTextMessageService.fetchTextMessagesForDataverseWithPaging(dataverseId, first, pageSize));

        dataverseTextMessageDtos = dataverseTextMessages.isPresent() ?
                mapper.mapToDtos(dataverseTextMessages.get()) :
                Lists.newArrayList();

        removeMessageLanguagesNotPresentInDataverse(dataverseTextMessageDtos);
        sortMessageLanguages(dataverseTextMessageDtos);

        setPageSize(pageSize);
        setRowCount(dataverseTextMessageService.countMessagesForDataverse(dataverseId).intValue());

        return dataverseTextMessageDtos;
    }

    @Override
    public Object getRowKey(DataverseTextMessageDto object) {
        return object.getId();
    }

    @Override
    public DataverseTextMessageDto getRowData(String rowKey) {
        Long id = Long.valueOf(rowKey);

        return dataverseTextMessageDtos.stream()
                .filter(dtm -> dtm.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private List<DataverseTextMessageDto> removeMessageLanguagesNotPresentInDataverse(List<DataverseTextMessageDto> dataList) {
        Set<String> dataverseLocales = settingsWrapper.getConfiguredLocales().keySet();

        dataList.forEach(dataverseTextMessageDto -> dataverseTextMessageDto.getDataverseLocalizedMessage()
                .removeIf(localizedMessageDto -> !dataverseLocales.contains(localizedMessageDto.getLocale())));
        return dataList;
    }

    private List<DataverseTextMessageDto> sortMessageLanguages(List<DataverseTextMessageDto> dataList) {
        dataList.forEach(dataverseTextMessageDto ->
                dataverseTextMessageDto.getDataverseLocalizedMessage()
                        .sort(Comparator.comparing(DataverseLocalizedMessageDto::getLocale)));
        return dataList;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }
}
