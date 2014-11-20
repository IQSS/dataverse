/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.dto;

import java.util.List;

/**
 *
 * @author ellenk
 */
public  class MetadataBlockDTO {
         String displayName;
         List<FieldDTO> fields;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<FieldDTO> getFields() {
            return fields;
        }

        public void setFields(List<FieldDTO> fields) {
            this.fields = fields;
        }

        @Override
        public String toString() {
            return "MetadataBlockDTO{" + "displayName=" + displayName + ", fields=" + fields + '}';
        }
     }
