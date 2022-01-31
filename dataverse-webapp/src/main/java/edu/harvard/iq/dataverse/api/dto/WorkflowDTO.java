package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WorkflowDTO {
    private String name;
    private Long id;
    private List<WorkflowStepDTO> steps = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public List<WorkflowStepDTO> getSteps() {
        return steps;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSteps(List<WorkflowStepDTO> steps) {
        this.steps.addAll(steps);
    }

    // -------------------- INNER CLASSES --------------------

    public static class WorkflowStepDTO {
        private String stepType;
        private String provider;
        private Map<String, String> parameters = new HashMap<>();
        private Map<String, String> requiredSettings = new HashMap<>();

        // -------------------- GETTERS --------------------

        public String getStepType() {
            return stepType;
        }

        public String getProvider() {
            return provider;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public Map<String, String> getRequiredSettings() {
            return requiredSettings;
        }

        // -------------------- SETTERS --------------------

        public void setStepType(String stepType) {
            this.stepType = stepType;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters.putAll(parameters);
        }

        public void setRequiredSettings(Map<String, String> requiredSettings) {
            this.requiredSettings.putAll(requiredSettings);
        }
    }

    public static class Converter {
        public WorkflowDTO convert(Workflow workflow) {
            WorkflowDTO converted = convertMinimal(workflow);
            if (workflow.getSteps() == null) {
                return converted;
            }
            List<WorkflowStepDTO> steps = workflow.getSteps().stream()
                    .map(s -> {
                        WorkflowStepDTO step = new WorkflowStepDTO();
                        step.setStepType(s.getStepType());
                        step.setProvider(s.getProviderId());
                        step.setParameters(s.getStepParameters());
                        step.setRequiredSettings(s.getStepSettings());
                        return step;
                    })
                    .collect(Collectors.toList());
            converted.setSteps(steps);
            return converted;
        }

        public WorkflowDTO convertMinimal(Workflow workflow) {
            WorkflowDTO converted = new WorkflowDTO();
            converted.setName(workflow.getName());
            converted.setId(workflow.getId());
            return converted;
        }
    }
}
