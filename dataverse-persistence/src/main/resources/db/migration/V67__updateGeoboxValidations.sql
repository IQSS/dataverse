update datasetfieldtype
    set validation = '[{"name":"geobox_component_validator", "parameters":{"runOnEmpty":"true"}}]'
    where parentdatasetfieldtype_id = (select id from datasetfieldtype dft where dft.name = 'geographicBoundingBox');