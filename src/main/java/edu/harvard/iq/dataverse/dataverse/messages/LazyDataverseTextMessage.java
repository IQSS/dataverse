package edu.harvard.iq.dataverse.dataverse.messages;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ViewScoped
@Named("LazyDataverseTextMessage")
public class LazyDataverseTextMessage extends LazyDataModel<DataverseTextMessageDto> {

    @EJB
    private DataverseTextMessageServiceBean dataverseTextMessageService;

    @Inject
    private DataverseMessagesMapper mapper;

    private Long dataverseId;
    private List<DataverseTextMessageDto> dataverseTextMessageDtos;

    @Override
    public List<DataverseTextMessageDto> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

        Optional<List<DataverseTextMessage>> dataverseTextMessages =
                Optional.ofNullable(dataverseTextMessageService.fetchTextMessagesForDataverseWithPaging(dataverseId, first, pageSize));

        return dataverseTextMessageDtos = dataverseTextMessages.isPresent() ?
                mapper.mapToDtos(dataverseTextMessages.get()) :
                Lists.newArrayList();
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

    public Long getDataverseId() {
        return dataverseId;
    }

    public List<DataverseTextMessageDto> getDataverseTextMessageDtos() {
        return dataverseTextMessageDtos;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setDataverseTextMessageDtos(List<DataverseTextMessageDto> dataverseTextMessageDtos) {
        this.dataverseTextMessageDtos = dataverseTextMessageDtos;
    }
}
