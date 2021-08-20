/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.List;

/**
 * @author rmp553
 */
public class UserListResult {

    private final Pager pager;
    private final List<AuthenticatedUser> userList;


    // -------------------- CONSTRUCTORS --------------------

    public UserListResult(Pager pager, List<AuthenticatedUser> userList) {
        this.pager = pager;
        this.userList = userList;
    }

    // -------------------- GETTERS --------------------

    /**
     * Get for pager
     *
     * @return Pager
     */
    public Pager getPager() {
        return pager;
    }

    /**
     * Get for userList
     *
     * @return List<AuthenticatedUser>
     */
    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }

    // -------------------- LOGIC --------------------

    /**
     * TO DO!
     * Return this object as a JsonObjectBuilder object
     *
     * @return
     */
    public JsonObjectBuilder toJSON() {

        JsonObjectBuilder jsonOverallData = Json.createObjectBuilder();
        jsonOverallData.add("userCount", pager.getNumResults())
                .add("selectedPage", pager.getSelectedPageNumber())
                .add("pagination", pager.asJsonObjectBuilder())
                .add("bundleStrings", AuthenticatedUser.getBundleStrings())
                .add("users", getUsersAsJSONArray())
        ;
        return jsonOverallData;
    }

    // -------------------- PRIVATE --------------------

    private JsonArrayBuilder getUsersAsJSONArray() {

        JsonArrayBuilder jsonUserListArray = Json.createArrayBuilder();

        for (AuthenticatedUser oneUser : userList) {
            jsonUserListArray.add(oneUser.toJson());
        }
        return jsonUserListArray;
    }
}
