package edu.harvard.iq.dataverse.search;

public abstract class IndexableObject {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public enum IndexableTypes {

        /**
         * @todo reconcile with IndexServiceBean.solrDocIdentifierDataset et al.
         */
        DATAVERSE("dataverse"), DATASET("dataset"), DATAFILE("datafile"), GROUP("group");

        private String name;

        private IndexableTypes(String string) {
            name = string;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
