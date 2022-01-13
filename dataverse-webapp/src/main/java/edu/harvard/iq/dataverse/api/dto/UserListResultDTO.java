package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.common.AuthenticatedUserUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.userdata.UserListResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserListResultDTO {

    private Integer userCount;
    private Integer selectedPage;
    private PagerDTO pagination;
    private Map<String, String> bundleStrings;
    private List<UserDTO> users;

    // -------------------- GETTERS --------------------

    public Integer getUserCount() {
        return userCount;
    }

    public Integer getSelectedPage() {
        return selectedPage;
    }

    public PagerDTO getPagination() {
        return pagination;
    }

    public Map<String, String> getBundleStrings() {
        return bundleStrings;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    // -------------------- SETTERS --------------------

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }

    public void setSelectedPage(Integer selectedPage) {
        this.selectedPage = selectedPage;
    }

    public void setPagination(PagerDTO pagination) {
        this.pagination = pagination;
    }

    public void setBundleStrings(Map<String, String> bundleStrings) {
        this.bundleStrings = bundleStrings;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }

    // -------------------- INNER CLASSES --------------------

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserDTO {
        private Long id;
        private String userIdentifier;
        private String lastName;
        private String firstName;
        private String email;
        private String affiliation;
        private String position;
        private String notificationsLanguage;
        private Boolean isSuperuser;
        private String authenticationProvider;
        private String roles;
        private String createdTime;
        private String lastLoginTime;
        private String lastApiUseTime;

        // -------------------- GETTERS --------------------

        public Long getId() {
            return id;
        }

        public String getUserIdentifier() {
            return userIdentifier;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getEmail() {
            return email;
        }

        public String getAffiliation() {
            return affiliation;
        }

        public String getPosition() {
            return position;
        }

        public String getNotificationsLanguage() {
            return notificationsLanguage;
        }

        public Boolean getIsSuperuser() {
            return isSuperuser;
        }

        public String getAuthenticationProvider() {
            return authenticationProvider;
        }

        public String getRoles() {
            return roles;
        }

        public String getCreatedTime() {
            return createdTime;
        }

        public String getLastLoginTime() {
            return lastLoginTime;
        }

        public String getLastApiUseTime() {
            return lastApiUseTime;
        }


        // -------------------- SETTERS --------------------

        public void setId(Long id) {
            this.id = id;
        }

        public void setUserIdentifier(String userIdentifier) {
            this.userIdentifier = userIdentifier;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setAffiliation(String affiliation) {
            this.affiliation = affiliation;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public void setNotificationsLanguage(String notificationsLanguage) {
            this.notificationsLanguage = notificationsLanguage;
        }

        public void setIsSuperuser(Boolean isSuperuser) {
            this.isSuperuser = isSuperuser;
        }

        public void setAuthenticationProvider(String authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }

        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }

        public void setLastLoginTime(String lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
        }

        public void setLastApiUseTime(String lastApiUseTime) {
            this.lastApiUseTime = lastApiUseTime;
        }
    }

    public static class Converter {

        // -------------------- LOGIC --------------------

        public UserListResultDTO convert(UserListResult result) {
            UserListResultDTO converted = new UserListResultDTO();
            converted.setUserCount(result.getPager().getNumResults());
            converted.setSelectedPage(result.getPager().getSelectedPageNumber());
            converted.setPagination(new PagerDTO.Converter().convert(result.getPager()));
            converted.setBundleStrings(getBundleStrings());
            converted.setUsers(result.getUserList().stream()
                    .map(u -> {
                        UserDTO user = new UserDTO();
                        user.setId(u.getId());
                        user.setUserIdentifier(u.getUserIdentifier());
                        user.setLastName(u.getLastName());
                        user.setFirstName(u.getFirstName());
                        user.setEmail(u.getEmail());
                        user.setAffiliation(u.getAffiliation());
                        user.setPosition(u.getPosition());
                        user.setNotificationsLanguage(Objects.toString(u.getNotificationsLanguage(), null));
                        user.setIsSuperuser(u.isSuperuser());
                        user.setAuthenticationProvider(
                                AuthenticatedUserUtil.getAuthenticationProviderFriendlyName(
                                        u.getAuthenticatedUserLookup().getAuthenticationProviderId()));
                        user.setRoles(u.getRoles());
                        user.setCreatedTime(Objects.toString(u.getCreatedTime(), null));
                        user.setLastLoginTime(Objects.toString(u.getLastLoginTime(), null));
                        user.setLastApiUseTime(Objects.toString(u.getLastApiUseTime(), null));
                        return user;
                    })
                    .collect(Collectors.toList()));
            return converted;
        }

        // -------------------- PRIVATE --------------------

        // Should match order of UserDTO object
        private Map<String, String> getBundleStrings() {
            Map<String, String> bundleStrings = new LinkedHashMap<>();
            putFromBundle(bundleStrings, "userId", "userIdentifier", "lastName", "firstName",
                    "email", "affiliation", "position", "isSuperuser");
            bundleStrings.put("authenticationProvider",
                    BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.authProviderFactoryAlias"));
            putFromBundle(bundleStrings, "roles", "createdTime", "lastLoginTime", "lastApiUseTime");
            return bundleStrings;
        }

        private void putFromBundle(Map<String, String> map, String... names) {
            for (String name : names) {
                map.put(name, BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header." + name));
            }
        }
    }
}
