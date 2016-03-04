-- Add new foreign ket to dataset for citation date (from datasetfieldtype)
ALTER TABLE dataset ADD COLUMN citationdatedatasetfieldtype_id bigint;

ALTER TABLE dataset
  ADD CONSTRAINT fk_dataset_citationdatedatasetfieldtype_id FOREIGN KEY (citationdatedatasetfieldtype_id)
      REFERENCES datasetfieldtype (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;