package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpAddressRange;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IpGroupDTO {
    private String alias;
    private String identifier;
    private Long id;
    private String name;
    private String description;
    private List<String> addresses = new ArrayList<>();
    private List<List<String>> ranges = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public String getAlias() {
        return alias;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public List<List<String>> getRanges() {
        return ranges;
    }

    // -------------------- SETTERS --------------------


    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses.addAll(addresses);
    }

    public void setRanges(List<List<String>> ranges) {
        this.ranges.addAll(ranges);
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {

        // -------------------- LOGIC --------------------

        public IpGroupDTO convert(IpGroup ipGroup) {
            IpGroupDTO converted = new IpGroupDTO();

            converted.setAlias(ipGroup.getAlias());
            converted.setIdentifier(ipGroup.getIdentifier());
            converted.setId(ipGroup.getId());
            converted.setName(ipGroup.getDisplayName());
            converted.setDescription(ipGroup.getDescription());

            List<String> singleAdresses = ipGroup.getRanges().stream()
                    .filter(IpAddressRange::isSingleAddress)
                    .map(IpAddressRange::getBottom)
                    .map(IpAddress::toString)
                    .collect(Collectors.toList());
            converted.setAddresses(singleAdresses);

            List<List<String>> ranges = ipGroup.getRanges().stream()
                    .filter(r -> !r.isSingleAddress())
                    .map(r -> Stream.of(r.getBottom(), r.getTop())
                            .map(IpAddress::toString)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            converted.setRanges(ranges);

            return converted;
        }
    }
}
