package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {

    @GET
    public Response listMetadataBlocks(@QueryParam("onlyDisplayedOnCreate") boolean onlyDisplayedOnCreate,
                                       @QueryParam("returnDatasetFieldTypes") boolean returnDatasetFieldTypes) {
        List<MetadataBlock> metadataBlocks = metadataBlockSvc.listMetadataBlocks(onlyDisplayedOnCreate);
        return ok(json(metadataBlocks, returnDatasetFieldTypes, onlyDisplayedOnCreate));
    }

    @Path("{identifier}")
    @GET
    public Response getMetadataBlock(@PathParam("identifier") String idtf) {
        MetadataBlock b = findMetadataBlock(idtf);
        return (b != null) ? ok(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }

    @AuthRequired
    @PUT
    @Path("{identifier}/datasetTypes")
    public Response updateAssociationsWithDatasetTypes(@Context ContainerRequestContext crc, @PathParam("identifier") String idtf, String jsonBody) {
        AuthenticatedUser user = createDataverseRequest(getRequestUser(crc)).getAuthenticatedUser();
        if (!user.isSuperuser()) {
            return unauthorized("superuser only");
        }
        MetadataBlock mdb = findMetadataBlock(idtf);
        if (mdb == null) {
            return notFound("Can't find metadata block '" + idtf + "'");
        }
        List<DatasetFieldType> existingDatasetFieldTypes = mdb.getDatasetFieldTypes();
        System.out.println("jsonBody: " + jsonBody);
        JsonArray jsonBodyAsArray = JsonUtil.getJsonArray(jsonBody);
        if (jsonBodyAsArray.size() == 0) {
            List<DatasetType> listOfTypes = mdb.getDatasetTypes();
            for (DatasetType datasetType : listOfTypes) {
                datasetType.getMetadataBlocks().remove(mdb);
                try {
                    datasetTypeSvc.save(datasetType);
                } catch (WrappedResponse ex) {
                    Logger.getLogger(MetadataBlocks.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            mdb.setDatasetTypes(new ArrayList<>());
            MetadataBlock saved = metadataBlockSvc.save(mdb);
            return ok("zero size! saved!");
        }
        List<DatasetType> datasetTypesToSave = new ArrayList<>();
        
        // MULTI BEGIN
//        JsonArray json = JsonUtil.getJsonArray(jsonBody);
//        for (JsonString jsonValue : json.getValuesAs(JsonString.class)) {
//            String typeName = jsonValue.getString();
//            System.out.println("typename: " + typeName);
//            //BEGIN
//            DatasetType datasetTypeDataset = datasetTypeSvc.getByName("dataset");
//            System.out.println("datasetTypeDataset: " + datasetTypeDataset);
//            //END
//            DatasetType datasetType = datasetTypeSvc.getByName(typeName);
//            System.out.println("datasetType: " + datasetType);
//            if (datasetType != null) {
//                datasetTypesToSave.add(datasetType);
//                List<MetadataBlock> mdbs = datasetType.getMetadataBlocks();
//                mdbs.add(mdb);
//            } else {
//                return badRequest("Dataset type could not be found: " + typeName);
//            }
//        }
        // MULTI END

        // SINGLE BEGIN
        // FIXME get more than first
        String first = JsonUtil.getJsonArray(jsonBody).getString(0);
        DatasetType firstDatasetType = datasetTypeSvc.getByName(first);
        datasetTypesToSave.add(firstDatasetType);
        mdb.setDatasetTypes(datasetTypesToSave);
        firstDatasetType.getMetadataBlocks().add(mdb);
        List<MetadataBlock> mdbs = firstDatasetType.getMetadataBlocks();
        mdbs.add(mdb);
        // SINGLE END

        MetadataBlock saved = metadataBlockSvc.save(mdb);

        List<DatasetType> savedMdb = saved.getDatasetTypes();
        JsonArrayBuilder updatedDatasetFieldTypes = Json.createArrayBuilder();
        for (DatasetType savedDatasetType : savedMdb) {
            System.out.println("found one: " + savedDatasetType.getName());
            updatedDatasetFieldTypes.add(savedDatasetType.getName());
        }
        JsonArray updatedDatasetFieldTypesFinal = updatedDatasetFieldTypes.build();
        System.out.println("updatedDatasetFieldTypesFinal: " + updatedDatasetFieldTypesFinal);
//        System.out.println("updatedDatasetFieldTypes: " + updatedDatasetFieldTypes);
        return ok(Json.createObjectBuilder()
                .add("associatedDatasetTypes", Json.createObjectBuilder()
//                        .add("before", Json.createArrayBuilder(existingDatasetFieldTypes))
                        .add("after", updatedDatasetFieldTypesFinal)
//                        .add("after", updatedDatasetFieldTypesFinal.get(0))
                )
        );
    }
}
