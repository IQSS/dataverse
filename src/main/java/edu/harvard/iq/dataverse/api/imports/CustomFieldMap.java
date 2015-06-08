/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api.imports;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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
