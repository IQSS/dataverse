package edu.harvard.iq.dataverse.api.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author ellenk
 */
public  class MetadataBlockDTO {
         String displayName;
         String name;
         List<FieldDTO> fields = new ArrayList<FieldDTO>();

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public FieldDTO getField(String typeName) {
            
            for( FieldDTO field : fields) {
                if (field.getTypeName().equals(typeName)) {
                    return field;
                }
            }
            return null;
        }
        
       public void addField(FieldDTO newField) {
        FieldDTO current = getField(newField.typeName);
        // If there is no Field in this Metadatablock with the typeName,
        // then add it to the list
        if (current == null) {
            fields.add(newField);
        // Else, add/replace the value in the new field to the current field
        } else {
            if (current.multiple) {
               if (newField.typeClass.equals("compound")) {
                    ArrayList<HashSet<FieldDTO>> currentValue = current.getMultipleCompound();
                    currentValue.addAll(newField.getMultipleCompound());
                    current.setMultipleCompound(currentValue);
                } else if (newField.typeClass.equals("controlledVocabulary")) {
                    List<String> currentValue = current.getMultipleVocab();
                    currentValue.addAll(newField.getMultipleVocab());
                    current.setMultipleVocab(currentValue);
                } else {
                    List<String> currentValue = current.getMultiplePrimitive();
                    currentValue.addAll(newField.getMultiplePrimitive());
                    current.setMultiplePrimitive(currentValue);
                }
            } else {
                // If this Field doesn't allow multiples, just replace the value 
                // with the new field value.
                // (or concatenate, if this is a primitive field)
                if (newField.typeClass.equals("primitive")) {
                    String currentValue = current.getSinglePrimitive(); 
                    String newValue = currentValue + " " + newField.getSinglePrimitive();
                    current.setSinglePrimitive(newValue);
                } else {
                    current.value = newField.value;
                    
                }
            }
        }
    }
        
        public List<FieldDTO> getFields() {
            return fields;
        }

        public void setFields(List<FieldDTO> fields) {
            this.fields = fields;
        }

        @Override
        public String toString() {
            return "MetadataBlockDTO{" + "displayName=" + displayName + ", name=" + name + ", fields=" + fields + '}';
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
     }
