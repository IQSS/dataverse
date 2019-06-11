/**
 * Where all things authorization live. In some senses, these are not fully formally conceptualized yet, but we're
 * getting there. Some of the code is not yet refactored for the below concepts. This will be addressed soon.
 * 
 * Meanwhile, the concepts are:
 * <ul>
 *  <li>User - someone that can perform actions in the system. Examples: Guest, AuthenticatedUser</li>
 *  <li>AuthenticatedUser - a user that has an account, and is identified by a password. AuthenitcatedUsers
 *                          get to have an ApiToken as well. Roughly speaking, an AuthenticatedUser is a person
 *                          in the real world.</li>
 *  <li>UserRecord - a record of a user in some user repository. We have one repository bundled with Dataverse ({@link BuiltinUsers})
 *                   but this can also be a record on some remote Shibboleth server. Point is - user records can't do anything. They
 *                   have to be converted into an AuthenticatedUser first. <li>
 *  <li>Role - A named set of permissions. Defined at the installation level, or at a dataverse level.</li>
 *  <li>RoleAssignee - A user or a group, that can have roles assigned to it.</li>
 *  <li>Group - A set of role assignees. Can contain both users and other groups.</li>
 * </ul>
 */
package edu.harvard.iq.dataverse.authorization;
