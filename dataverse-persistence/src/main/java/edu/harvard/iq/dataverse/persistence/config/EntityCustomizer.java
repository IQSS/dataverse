package edu.harvard.iq.dataverse.persistence.config;

import edu.harvard.iq.dataverse.persistence.config.annotations.CustomizeSelectionQuery;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.Tuple;
import org.eclipse.persistence.config.DescriptorCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.OneToManyMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class EntityCustomizer implements DescriptorCustomizer {
    public enum Customizations {
        DATASET_FIELDS_WITH_PRIMARY_SOURCE(mapping -> {
            if (!mapping.isOneToManyMapping()) {
                return;
            }
            OneToManyMapping datasetFieldsMapping = (OneToManyMapping) mapping;
            Expression currentCriteria = datasetFieldsMapping.buildSelectionCriteria();
            ExpressionBuilder expressionBuilder = currentCriteria.getBuilder();
            Expression additionalCriteria = expressionBuilder
                    .get("source").equal(DatasetField.DEFAULT_SOURCE);
            datasetFieldsMapping.setSelectionCriteria(currentCriteria.and(additionalCriteria));
        })
        ;

        public Consumer<DatabaseMapping> customization;

        Customizations(Consumer<DatabaseMapping> customization) {
            this.customization = customization;
        }
    }

    // -------------------- LOGIC --------------------

    @Override
    public void customize(ClassDescriptor classDescriptor) throws Exception {
        Field[] fields = Optional.ofNullable(classDescriptor)
                .map(ClassDescriptor::getJavaClass)
                .map(Class::getDeclaredFields)
                .orElseGet(() -> new Field[0]);

        Arrays.stream(fields)
                .map(f -> Tuple.of(f, f.getAnnotation(CustomizeSelectionQuery.class)))
                .filter(t -> t._2 != null)
                .forEach(t -> t._2.value().customization.accept(
                        extractMappingForField(t._1, classDescriptor)));
    }

    // -------------------- PRIVATE --------------------

    private DatabaseMapping extractMappingForField(Field field, ClassDescriptor classDescriptor) {
        String name = field.getName();
        return classDescriptor.getMappingForAttributeName(name);
    }
}
