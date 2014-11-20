package edu.harvard.iq.dataverse.api.dto;

import java.util.List;

/**
 *
 * @author ellenk
 */
public class DataTableDTO {
    private String unf;
    private Long caseQuantity;
    private Long varQuantity;
    private Long recordsPerCase;
    private List<DataVariableDTO> dataVariables;

    public String getUnf() {
        return unf;
    }

    public void setUnf(String unf) {
        this.unf = unf;
    }

    public Long getCaseQuantity() {
        return caseQuantity;
    }

    public void setCaseQuantity(Long caseQuantity) {
        this.caseQuantity = caseQuantity;
    }

    public Long getVarQuantity() {
        return varQuantity;
    }

    public void setVarQuantity(Long varQuantity) {
        this.varQuantity = varQuantity;
    }

    public Long getRecordsPerCase() {
        return recordsPerCase;
    }

    public void setRecordsPerCase(Long recordsPerCase) {
        this.recordsPerCase = recordsPerCase;
    }

    public List<DataVariableDTO> getDataVariables() {
        return dataVariables;
    }

    public void setDataVariables(List<DataVariableDTO> dataVariables) {
        this.dataVariables = dataVariables;
    }
    
    
}
