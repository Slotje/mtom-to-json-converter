package nl.belastingdienst.bte.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private String layer; // "SCHEMA", "BUSINESS", "REFERENCE"
    private boolean valid;
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationError> warnings = new ArrayList<>();

    public ValidationResult(String layer) {
        this.layer = layer;
        this.valid = true;
    }

    public void addError(String code, String message, String suggestion) {
        this.errors.add(new ValidationError(code, message, suggestion, "ERROR"));
        this.valid = false;
    }

    public void addWarning(String code, String message, String suggestion) {
        this.warnings.add(new ValidationError(code, message, suggestion, "WARNING"));
    }

    public String getLayer() { return layer; }
    public boolean isValid() { return valid; }
    public List<ValidationError> getErrors() { return errors; }
    public List<ValidationError> getWarnings() { return warnings; }
}
