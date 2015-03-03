package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.SolrField;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("config")
public class Config extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Config.class.getCanonicalName());

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @GET
    @Path("solr/schema")
    public Response getSolrSchema() {

        StringBuilder sb = new StringBuilder();

        for (DatasetFieldType datasetField : datasetFieldService.findAllOrderedByName()) {
            String nameSearchable = datasetField.getSolrField().getNameSearchable();
            SolrField.SolrType solrType = datasetField.getSolrField().getSolrType();
            String type = solrType.getType();
            if (solrType.equals(SolrField.SolrType.EMAIL)) {
                /**
                 * @todo should we also remove all "email" field types (e.g.
                 * datasetContact) from schema.xml? We are explicitly not
                 * indexing them for
                 * https://github.com/IQSS/dataverse/issues/759
                 *
                 * "The list of potential collaborators should be searchable"
                 * according to https://github.com/IQSS/dataverse/issues/747 but
                 * it's not clear yet if this means a Solr or database search.
                 * For now we'll keep schema.xml as it is to avoid people having
                 * to update it. If anything, we can remove the email field type
                 * when we do a big schema.xml update for
                 * https://github.com/IQSS/dataverse/issues/754
                 */
                logger.log(Level.INFO, "email type detected ({0}) See also https://github.com/IQSS/dataverse/issues/759", nameSearchable);
            }
            String multivalued = datasetField.getSolrField().isAllowedToBeMultivalued().toString();
            // <field name="datasetId" type="text_general" multiValued="false" stored="true" indexed="true"/>
            sb.append("   <field name=\"").append(nameSearchable).append("\" type=\"").append(type).append("\" multiValued=\"").append(multivalued).append("\" stored=\"true\" indexed=\"true\"/>\n");
        }

        List<String> listOfStaticFields = new ArrayList();
        Object searchFieldsObject = new SearchFields();
        Field[] staticSearchFields = searchFieldsObject.getClass().getDeclaredFields();
        for (Field fieldObject : staticSearchFields) {
            String name = fieldObject.getName();
            String staticSearchField = null;
            try {
                staticSearchField = (String) fieldObject.get(searchFieldsObject);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
            }

            /**
             * @todo: if you search for "pdf" should you get all pdfs? do we
             * need a copyField source="filetypemime_s" to the catchall?
             */
            if (listOfStaticFields.contains(staticSearchField)) {
                return errorResponse( Status.INTERNAL_SERVER_ERROR, "static search field defined twice: " + staticSearchField);
            }
            listOfStaticFields.add(staticSearchField);
        }

        sb.append("---\n");

        for (DatasetFieldType datasetField : datasetFieldService.findAllOrderedByName()) {
            String nameSearchable = datasetField.getSolrField().getNameSearchable();
            String nameFacetable = datasetField.getSolrField().getNameFacetable();

            if (listOfStaticFields.contains(nameSearchable)) {
                if (nameSearchable.equals(SearchFields.DATASET_DESCRIPTION)) {
                    // Skip, expected conflct.
                } else {
                    return errorResponse( Status.INTERNAL_SERVER_ERROR, "searchable dataset metadata field conflict detected with static field: " + nameSearchable);
                }
            }

            if (listOfStaticFields.contains(nameFacetable)) {
                return errorResponse( Status.INTERNAL_SERVER_ERROR, "facetable dataset metadata field conflict detected with static field: " + nameFacetable);
            }

            // <copyField source="*_i" dest="text" maxChars="3000"/>
            sb.append("   <copyField source=\"").append(nameSearchable).append("\" dest=\"text\" maxChars=\"3000\"/>\n");
        }

        return okResponse(sb.toString());
    }

}
