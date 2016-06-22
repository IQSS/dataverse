package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetDistributor;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroup;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.DatasetFieldWalker;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Convert objects to Json.
 * @author michael
 */
public class JsonPrinter {
	
    	
	public static final BriefJsonPrinter brief = new BriefJsonPrinter();
	
    public static JsonArrayBuilder asJsonArray( Collection<String> strings ) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for ( String s : strings ) {
            arr.add(s);
        }
        return arr;
    }
    
	public static JsonObjectBuilder json( User u ) {
        RoleAssigneeDisplayInfo displayInfo = u.getDisplayInfo();
        return jsonObjectBuilder()
                .add("identifier", u.getIdentifier() )
                .add("displayInfo", jsonObjectBuilder()
                           .add("Title", displayInfo.getTitle())
                           .add("email", displayInfo.getEmailAddress()));
    }

    /**
     * @todo Rename this to just "json" to match the other methods once "json(
     * Dataverse dv )" is reviewed since in calls the "json( User u )" version
     * and we want to keep it that way rather than calling this method.
     */
    public static JsonObjectBuilder jsonForAuthUser(AuthenticatedUser authenticatedUser) {
        return jsonObjectBuilder()
                .add("id", authenticatedUser.getId())
                .add("identifier", authenticatedUser.getIdentifier())
                .add("displayName", authenticatedUser.getDisplayInfo().getTitle())
                .add("firstName", authenticatedUser.getFirstName())
                .add("lastName", authenticatedUser.getLastName())
                .add("email", authenticatedUser.getEmail())
                .add("superuser", authenticatedUser.isSuperuser())
                .add("affiliation", authenticatedUser.getAffiliation())
                .add("position", authenticatedUser.getPosition())
                .add("persistentUserId", authenticatedUser.getAuthenticatedUserLookup().getPersistentUserId())
                .add("authenticationProviderId", authenticatedUser.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }
    
    public static JsonObjectBuilder json( RoleAssignment ra ) {
		return jsonObjectBuilder()
				.add("id", ra.getId())
				.add("assignee", ra.getAssigneeIdentifier() )
				.add("roleId", ra.getRole().getId() )
				.add("_roleAlias", ra.getRole().getAlias())
				.add("definitionPointId", ra.getDefinitionPoint().getId() );
	}
	
	public static JsonArrayBuilder json( Set<Permission> permissions ) {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Permission p : permissions ) {
			bld.add( p.name() );
		}
		return bld;
	}
    
    public static JsonObjectBuilder json( RoleAssigneeDisplayInfo d ) {
        return jsonObjectBuilder()
                .add( "title", d.getTitle() )
                .add( "email", d.getEmailAddress() )
                .add( "affiliation", d.getAffiliation() );
    }
    
	public static JsonObjectBuilder json( IpGroup grp ) {
        JsonArrayBuilder rangeBld = Json.createArrayBuilder();
        for ( IpAddressRange r :grp.getRanges() ) {
            rangeBld.add( Json.createArrayBuilder().add(r.getBottom().toString()).add(r.getTop().toString()) );
        }
        return jsonObjectBuilder()
                .add("alias", grp.getPersistedGroupAlias() )
                .add("identifier", grp.getIdentifier())
                .add("id", grp.getId() )
                .add("name", grp.getDisplayName() )
                .add("description", grp.getDescription() )
                .add("ranges", rangeBld);
    }

        public static JsonObjectBuilder json(ShibGroup grp) {
        return jsonObjectBuilder()
                .add("name", grp.getName())
                .add("attribute", grp.getAttribute())
                .add("pattern", grp.getPattern())
                .add("id", grp.getId());
    }
    
	public static JsonArrayBuilder rolesToJson( List<DataverseRole> role ) {
        JsonArrayBuilder bld = Json.createArrayBuilder();
        for ( DataverseRole r : role ) {
            bld.add( json(r) );
        }
        return bld;
    }
    
	public static JsonObjectBuilder json( DataverseRole role ) {
		JsonObjectBuilder bld = jsonObjectBuilder()
				.add("alias", role.getAlias()) 
				.add("name", role.getName())
				.add("permissions", json(role.permissions()))
				.add("description", role.getDescription());
		if ( role.getId() != null ) bld.add("id", role.getId() );
		if ( role.getOwner()!=null && role.getOwner().getId()!=null ) bld.add("ownerId", role.getOwner().getId());
		
		return bld;
	}
	
	public static JsonObjectBuilder json( Dataverse dv ) {
		JsonObjectBuilder bld = jsonObjectBuilder()
						.add("id", dv.getId() )
						.add("alias", dv.getAlias()) 
						.add("name", dv.getName())
                                                .add("affiliation", dv.getAffiliation())
                                                .add("dataverseContacts", json(dv.getDataverseContacts()))
						.add("permissionRoot", dv.isPermissionRoot())
						.add("description", dv.getDescription());
		if ( dv.getOwner() != null ) {
			bld.add("ownerId", dv.getOwner().getId());
		}
		if ( dv.getCreateDate() != null ) {
			bld.add("creationDate", Util.getDateTimeFormat().format(dv.getCreateDate()));
		}
                if ( dv.getCreator() != null ) {
                    bld.add("creator",json(dv.getCreator()));
                }
		
		return bld;
	}

    public static JsonArrayBuilder json(List<DataverseContact> dataverseContacts) {
        JsonArrayBuilder bld = Json.createArrayBuilder();
        for (DataverseContact dc : dataverseContacts) {
            bld.add( jsonObjectBuilder()
                .add( "displayOrder",dc.getDisplayOrder())
                .add( "contactEmail",dc.getContactEmail())
            );
        }
        return bld;
    }       
      
	
	public static JsonObjectBuilder json( BuiltinUser user ) {
		return (user == null ) 
				? null 
				: jsonObjectBuilder()
					.add( "id", user.getId() )
					.add( "firstName", user.getFirstName())
					.add( "lastName",  user.getLastName())
					.add( "userName",  user.getUserName())
					.add( "affiliation", user.getAffiliation())
					.add( "position",  user.getPosition())
					.add( "email",     user.getEmail());
	}
	
	public static JsonObjectBuilder json( Dataset ds ) {
		return jsonObjectBuilder()
				.add( "id", ds.getId() )
				.add( "identifier", ds.getIdentifier() )
				.add( "persistentUrl", ds.getPersistentURL() )
				.add( "protocol", ds.getProtocol() )
				.add( "authority", ds.getAuthority() );
	}
	
	public static JsonObjectBuilder json( DatasetVersion dsv ) {
		JsonObjectBuilder bld = jsonObjectBuilder()
				.add("id", dsv.getId())
				.add("versionNumber", dsv.getVersionNumber())
				.add("versionMinorNumber", dsv.getMinorVersionNumber())
				.add("versionState", dsv.getVersionState().name() )
				.add("versionNote", dsv.getVersionNote())
				.add("archiveNote", dsv.getArchiveNote())
				.add("deaccessionLink", dsv.getDeaccessionLink())
				.add("distributionDate", dsv.getDistributionDate())
				.add("productionDate", dsv.getProductionDate())
				.add("UNF", dsv.getUNF())
				.add("archiveTime", format(dsv.getArchiveTime()) )
				.add("lastUpdateTime", format(dsv.getLastUpdateTime()) )
				.add("releaseTime", format(dsv.getReleaseTime()) )
				.add("createTime", format(dsv.getCreateTime()) )
				;
                
		bld.add("metadataBlocks", jsonByBlocks(dsv.getDatasetFields()));
        
        bld.add( "files", jsonFileMetadatas(dsv.getFileMetadatas()) );
		
		return bld;
	}

    /**
     * Export formats such as DDI require the citation to be included. See
     * https://github.com/IQSS/dataverse/issues/2579 for more on DDI export.
     *
     * @todo Instead of having this separate method, should "citation" be added
     * to the regular `json` method for DatasetVersion? Will anything break?
     * Unit tests for that method could not be found.
     */
    public static JsonObjectBuilder jsonWithCitation(DatasetVersion dsv) {
        JsonObjectBuilder dsvWithCitation = json(dsv);
        dsvWithCitation.add("citation", dsv.getCitation());
        return dsvWithCitation;
    }

    /**
     * Export formats such as DDI require the persistent identifier components
     * such as "protocol", "authority" and "identifier" to be included so we
     * create a JSON object we can convert to a DatasetDTO which can include a
     * DatasetVersionDTO, which has all the metadata fields we need to export.
     * See https://github.com/IQSS/dataverse/issues/2579 for more on DDI export.
     *
     * @todo Instead of having this separate method, should "datasetVersion" be
     * added to the regular `json` method for Dataset? Will anything break? Unit
     * tests for that method could not be found. If we keep this method as-is
     * should the method be renamed?
     */
    public static JsonObjectBuilder jsonAsDatasetDto(DatasetVersion dsv) {
        JsonObjectBuilder datasetDtoAsJson = json(dsv.getDataset());
        datasetDtoAsJson.add("datasetVersion", jsonWithCitation(dsv));
        return datasetDtoAsJson;
    }

    public static JsonArrayBuilder jsonFileMetadatas( Collection<FileMetadata> fmds ) {
        JsonArrayBuilder filesArr = Json.createArrayBuilder();
        for ( FileMetadata fmd : fmds ) {
            filesArr.add(json(fmd));
        }
        return filesArr;
    }
    
    public static JsonObjectBuilder json( DatasetDistributor dist ) {
        return jsonObjectBuilder()
                .add( "displayOrder",dist.getDisplayOrder())
                .add( "version",dist.getVersion())
                .add( "abbreviation", json(dist.getAbbreviation()) )
                .add( "affiliation", json(dist.getAffiliation()) )
                .add( "logo", json(dist.getLogo()) )
                .add( "name", json(dist.getName()) )
                .add( "url", json(dist.getUrl()) )
                ;
    }
    
    public static JsonObjectBuilder jsonByBlocks( List<DatasetField> fields ) {
        JsonObjectBuilder blocksBld = jsonObjectBuilder();
		
		for ( Map.Entry<MetadataBlock, List<DatasetField>> blockAndFields : DatasetField.groupByBlock(fields).entrySet() ) {
            MetadataBlock block = blockAndFields.getKey();
            blocksBld.add( block.getName(), json( block, blockAndFields.getValue()) );
		}
        return blocksBld;
    }
    
    /**
     * Create a JSON object for the block and its fields. The fields are
     * assumed to belong to the block - there's no checking of that in the
     * method.
     * 
     * @param block
     * @param fields
     * @return JSON Object builder with the block and fields information.
     */
    public static JsonObjectBuilder json( MetadataBlock block, List<DatasetField> fields ) {
        JsonObjectBuilder blockBld = jsonObjectBuilder();
			
        blockBld.add("displayName", block.getDisplayName());
        final JsonArrayBuilder fieldsArray = Json.createArrayBuilder();

        DatasetFieldWalker.walk(fields, new DatasetFieldsToJson(fieldsArray));

        blockBld.add("fields", fieldsArray);
        return blockBld;
    }
	
    public static String typeClassString( DatasetFieldType typ ) {
        if ( typ.isControlledVocabulary()) return "controlledVocabulary";
        if ( typ.isCompound()) return "compound";
        return "primitive";
    }
    
    public static JsonObject json( DatasetField dfv ) {
		if ( dfv.isEmpty() ) {
			return null;
        } else {
            JsonArrayBuilder fieldArray = Json.createArrayBuilder();
            DatasetFieldWalker.walk(dfv, new DatasetFieldsToJson(fieldArray));
            JsonArray out = fieldArray.build();
            return out.getJsonObject(0);
		}
	}
	
	public static JsonObjectBuilder json( MetadataBlock blk ) {
		JsonObjectBuilder bld = jsonObjectBuilder();
		bld.add("id", blk.getId());
		bld.add("name", blk.getName());
		bld.add("displayName", blk.getDisplayName());
		
		JsonObjectBuilder fieldsBld = jsonObjectBuilder();
		for ( DatasetFieldType df : new TreeSet<>(blk.getDatasetFieldTypes()) ) {
			fieldsBld.add( df.getName(), json(df) );
		}
		
		bld.add("fields", fieldsBld );
		
		return bld;
	}
	
	public static JsonObjectBuilder json( DatasetFieldType fld ) {
		JsonObjectBuilder fieldsBld = jsonObjectBuilder();
		fieldsBld.add( "name", fld.getName() );
		fieldsBld.add( "displayName", fld.getDisplayName());
		fieldsBld.add( "title", fld.getTitle());
		fieldsBld.add( "type", fld.getFieldType().toString());
		fieldsBld.add( "watermark", fld.getWatermark());
		fieldsBld.add( "description", fld.getDescription());
		if ( ! fld.getChildDatasetFieldTypes().isEmpty() ) {
			JsonObjectBuilder subFieldsBld = jsonObjectBuilder();
			for ( DatasetFieldType subFld : fld.getChildDatasetFieldTypes() ) {
				subFieldsBld.add( subFld.getName(), json(subFld) );
			}
			fieldsBld.add("childFields", subFieldsBld);
		}
		
		return fieldsBld;
	}
	
	public static JsonObjectBuilder json( FileMetadata fmd ) {
		return jsonObjectBuilder()
				// deprecated: .add("category", fmd.getCategory())
				.add("description", fmd.getDescription())
				.add("label", fmd.getLabel())
				.add("version", fmd.getVersion())
				.add("datasetVersionId", fmd.getDatasetVersion().getId())
				.add("datafile", json(fmd.getDataFile()))
				;
	}
	
	public static JsonObjectBuilder json( DataFile df ) {
                String fileName = "";
                if (df.getFileMetadata() != null) {
                    fileName = df.getFileMetadata().getLabel();
                }
		return jsonObjectBuilder()
				.add("id", df.getId() )
				.add("name", fileName)
				.add("contentType", df.getContentType())
				.add("filename", df.getStorageIdentifier())
				.add("originalFileFormat", df.getOriginalFileFormat())
				.add("originalFormatLabel", df.getOriginalFormatLabel())
				.add("UNF", df.getUnf())
				.add("md5", df.getmd5())
				.add("description", df.getDescription())
				;
	}
	
	public static String format( Date d ) {
		return (d==null) ? null : Util.getDateTimeFormat().format(d);
	}
    
    private static class DatasetFieldsToJson implements DatasetFieldWalker.Listener {

        Deque<JsonObjectBuilder> objectStack = new LinkedList<>();
        Deque<JsonArrayBuilder>  valueArrStack = new LinkedList<>();
        JsonObjectBuilder result = null;
        
        
        DatasetFieldsToJson( JsonArrayBuilder result ) {
            valueArrStack.push(result);
        }
        
        @Override
        public void startField(DatasetField f) {
            objectStack.push( jsonObjectBuilder() );
            // Invariant: all values are multiple. Diffrentiation between multiple and single is done at endField.
            valueArrStack.push(Json.createArrayBuilder());
            
            DatasetFieldType typ = f.getDatasetFieldType();
            objectStack.peek().add("typeName", typ.getName() );
            objectStack.peek().add("multiple", typ.isAllowMultiples());
            objectStack.peek().add("typeClass", typeClassString(typ) );
        }

        @Override
        public void endField(DatasetField f) {
            JsonObjectBuilder jsonField = objectStack.pop();
            JsonArray jsonValues = valueArrStack.pop().build();
            if ( ! jsonValues.isEmpty() ) {
                jsonField.add("value",
                    f.getDatasetFieldType().isAllowMultiples() ? jsonValues
                                                               : jsonValues.get(0) );
                valueArrStack.peek().add(jsonField);
            }
        }

        @Override
        public void primitiveValue(DatasetFieldValue dsfv) {
            if (dsfv.getValue() != null) {
                valueArrStack.peek().add( dsfv.getValue() );
            }
        }

        @Override
        public void controledVocabularyValue(ControlledVocabularyValue cvv) {
            valueArrStack.peek().add( cvv.getStrValue() );
        }

        @Override
        public void startCompoundValue(DatasetFieldCompoundValue dsfcv) {
            valueArrStack.push( Json.createArrayBuilder() );
        }

        @Override
        public void endCompoundValue(DatasetFieldCompoundValue dsfcv) {
            JsonArray jsonValues = valueArrStack.pop().build();
            if ( ! jsonValues.isEmpty() ) {
                JsonObjectBuilder jsonField = jsonObjectBuilder();
                for ( JsonObject jobj : jsonValues.getValuesAs(JsonObject.class) ) {
                    jsonField.add( jobj.getString("typeName"), jobj );
                }
                valueArrStack.peek().add( jsonField );
            }
        }
    }
    
    public static JsonObjectBuilder json( AuthenticationProviderRow aRow ) {
        return jsonObjectBuilder()
                        .add("id", aRow.getId())
                        .add("factoryAlias", aRow.getFactoryAlias() )
                        .add("title", aRow.getTitle())
                        .add("subtitle",aRow.getSubtitle())
                        .add("factoryData", aRow.getFactoryData())
                        .add("enabled", aRow.isEnabled())
                ;
    }
    
    public static <T> JsonObjectBuilder json(T j ) {
        if (j instanceof ExplicitGroup) {
            ExplicitGroup eg = (ExplicitGroup) j;
            JsonArrayBuilder ras = Json.createArrayBuilder();
            for ( String u : eg.getContainedRoleAssgineeIdentifiers() ) {
                ras.add(u);
            }
            return jsonObjectBuilder()
                    .add("identifier", eg.getIdentifier() )
                    .add("groupAliasInOwner", eg.getGroupAliasInOwner() )
                    .add("owner",eg.getOwner().getId())
                    .add("description", eg.getDescription())
                    .add("displayName", eg.getDisplayName())
                    .add("containedRoleAssignees", ras);

        } else { // implication: (j instanceof DataverseFacet)
            DataverseFacet f = (DataverseFacet) j;
            return jsonObjectBuilder()
                    .add("id", String.valueOf(f.getId())) // TODO should just be id I think
                    .add("name", f.getDatasetFieldType().getDisplayName());
        }
    }
    
    public static <T> JsonArrayBuilder json( Collection<T> jc ) {
        JsonArrayBuilder bld = Json.createArrayBuilder();
        for ( T j : jc ) {
            bld.add( json(j) );
        }
        return bld;
    }
}
