package edu.harvard.iq.dataverse.search.query.filter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.search.index.geobox.Rectangle;
import edu.harvard.iq.dataverse.search.index.geobox.RectangleToSolrConverter;
import edu.harvard.iq.dataverse.search.response.FilterQuery;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxFields;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Eager
@ApplicationScoped
public class GeospatialFilterCreator implements SpecialFilterCreator {
    private static final Logger logger = LoggerFactory.getLogger(GeospatialFilterCreator.class);

    private DatasetFieldTypeRepository repository;
    private SpecialFilterService specialFilterService;

    // -------------------- CONSTRUCTORS --------------------

    public GeospatialFilterCreator() { }

    @Inject
    public GeospatialFilterCreator(DatasetFieldTypeRepository repository, SpecialFilterService specialFilterService) {
        this.repository = repository;
        this.specialFilterService = specialFilterService;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void register() {
        specialFilterService.register(this);
    }

    @Override
    public String getKey() {
        return "GEO";
    }

    @Override
    public SpecialFilter create(String initialQuery, String... params) {
        if (params == null || params.length != 5 || Arrays.stream(params).anyMatch(StringUtils::isBlank)) {
            logger.info("Incomplete (or over-complete) geospatial filter params: " + Arrays.toString(params));
            return SpecialFilter.EMPTY;
        }
        String fieldName = params[0];
        DatasetFieldType type = repository.findByName(fieldName).orElse(null);
        if (type == null) {
            logger.info("Cannot find DatasetFieldType with name: " + fieldName);
            return SpecialFilter.EMPTY;
        }
        SolrField solrField = SolrField.of(type);
        String solrQuery = String.format("{!field f=%s}Intersects(%s)", solrField.getNameSearchable(), createSolrQueryPart(params));
        FilterQuery filterQuery = new FilterQuery(initialQuery, type.getDisplayName(),
                Arrays.stream(params, 1, params.length).collect(Collectors.joining(", ")));
        return new SpecialFilter(initialQuery, solrQuery, filterQuery);
    }

    // -------------------- PRIVATE --------------------

    private String createSolrQueryPart(String... params) {
        // We rely heavily on the assumption that parameters are of the form: number (possibly with decimal part)
        // plus one uppercase letter from the 'N', 'E', 'S', 'W' set (without space between).
        Map<String, String> coords = Arrays.stream(params, 1, params.length)
                .filter(p -> p.length() > 1)
                .collect(Collectors.toMap(p -> p.substring(p.length() - 1),
                        p -> p.substring(0, p.length() - 1), (prev, next) -> next));
        try {
            Rectangle rectangle = new Rectangle(
                    coords.get(GeoboxFields.X1.fieldType()), coords.get(GeoboxFields.Y1.fieldType()),
                    coords.get(GeoboxFields.X2.fieldType()), coords.get(GeoboxFields.Y2.fieldType()));
            return new RectangleToSolrConverter()
                    .wrapIfNeeded(rectangle.cutIfNeeded());
        } catch (NullPointerException npe) {
            throw new IllegalArgumentException("Malformed parameter(s): " + Arrays.toString(params));
        }
    }
}
