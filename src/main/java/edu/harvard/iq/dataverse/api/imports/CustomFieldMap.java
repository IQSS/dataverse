/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api.imports;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 *
 * @author ekraffmiller
 */

@Entity
@NamedQueries(
        @NamedQuery( name="CustomFieldMap.findByTemplateField",
                     query="SELECT cfm FROM CustomFieldMap cfm WHERE cfm.sourceTemplate=:template and cfm.sourceDatasetField =:field")
)
@Table(indexes = {@Index(columnList="sourcedatasetfield"), @Index(columnList="sourcetemplate")})
public class CustomFieldMap implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String sourceTemplate;
    
    private String sourceDatasetField;
    
    private String targetDatasetField;

    public String getSourceTemplate() {
        return sourceTemplate;
    }

    public void setSourceTemplate(String sourceTemplate) {
        this.sourceTemplate = sourceTemplate;
    }

    public String getSourceDatasetField() {
        return sourceDatasetField;
    }

    public void setSourceDatasetField(String sourceDatasetField) {
        this.sourceDatasetField = sourceDatasetField;
    }

    public String getTargetDatasetField() {
        return targetDatasetField;
    }

    public void setTargetDatasetField(String targetDatasetField) {
        this.targetDatasetField = targetDatasetField;
    }

   
    
}
