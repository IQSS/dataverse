package edu.harvard.iq.dataverse.api.dto;

import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class DataverseMetadataBlockFacetDTO {

    private Long dataverseId;
    private String dataverseAlias;
    private boolean isMetadataBlockFacetRoot;
    private List<MetadataBlockDTO> metadataBlocks;

    public DataverseMetadataBlockFacetDTO(Long dataverseId, String dataverseAlias, boolean isMetadataBlockFacetRoot, List<MetadataBlockDTO> metadataBlocks) {
        this.dataverseId = dataverseId;
        this.dataverseAlias = dataverseAlias;
        this.isMetadataBlockFacetRoot = isMetadataBlockFacetRoot;
        this.metadataBlocks = metadataBlocks;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public String getDataverseAlias() {
        return dataverseAlias;
    }

    public boolean isMetadataBlockFacetRoot() {
        return isMetadataBlockFacetRoot;
    }

    public List<MetadataBlockDTO> getMetadataBlocks() {
        return metadataBlocks;
    }

    public static class MetadataBlockDTO {
        private String metadataBlockName;
        private String metadataBlockFacet;

        public MetadataBlockDTO(String metadataBlockName, String metadataBlockFacet) {
            this.metadataBlockName = metadataBlockName;
            this.metadataBlockFacet = metadataBlockFacet;
        }

        public String getMetadataBlockName() {
            return metadataBlockName;
        }

        public String getMetadataBlockFacet() {
            return metadataBlockFacet;
        }
    }
}
