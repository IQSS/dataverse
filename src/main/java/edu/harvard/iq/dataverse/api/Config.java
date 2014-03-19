package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("config")
public class Config extends AbstractApiBean {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @GET
    @Path("solr/schema")
    public String getSolrSchema() {

        StringBuilder sb = new StringBuilder();

        for (DatasetField datasetField : datasetFieldService.findAllOrderedByName()) {
            String nameSearchable = datasetField.getSolrField().getNameSearchable();
            String type = datasetField.getSolrField().getSolrType().getType();
            String multivalued = datasetField.getSolrField().isAllowedToBeMultivalued().toString();
            // <field name="datasetId" type="text_general" multiValued="false" stored="true" indexed="true"/>
            sb.append("<field name=\"" + nameSearchable + "\" type=\"" + type + "\" multiValued=\"" + multivalued + "\" stored=\"true\" indexed=\"true\"/>\n");
        }

        List<String> listOfStaticFields = new ArrayList();
        Object searchFieldsObject = new SearchFields();
        Field[] staticSearchFields = searchFieldsObject.getClass().getDeclaredFields();
        for (Field fieldObject : staticSearchFields) {
            String name = fieldObject.getName();
            String staticSearchField = null;
            try {
                staticSearchField = (String) fieldObject.get(searchFieldsObject);
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            }

            /**
             * @todo: if you search for "pdf" should you get all pdfs? do we
             * need a copyField source="filetypemime_s" to the catchall?
             */
            if (listOfStaticFields.contains(staticSearchField)) {
                return error("static search field defined twice: " + staticSearchField);
            }
            listOfStaticFields.add(staticSearchField);
        }

        sb.append("---\n");

        for (DatasetField datasetField : datasetFieldService.findAllOrderedByName()) {
            String nameSearchable = datasetField.getSolrField().getNameSearchable();
            String nameFacetable = datasetField.getSolrField().getNameFacetable();

            if (listOfStaticFields.contains(nameSearchable)) {
                if (nameSearchable.equals(SearchFields.DESCRIPTION)) {
                    // Skip, known conflct. We are merging these fields together across types.
                } else {
                    return error("searchable dataset metadata field conflict detected with static field: " + nameSearchable);
                }
            }

            if (listOfStaticFields.contains(nameFacetable)) {
                return error("facetable dataset metadata field conflict detected with static field: " + nameFacetable);
            }

            // <copyField source="*_i" dest="text" maxChars="3000"/>
            sb.append("<copyField source=\"" + nameSearchable + "\" dest=\"text\" maxChars=\"3000\"/>\n");
        }

        return sb.toString();
    }

}
