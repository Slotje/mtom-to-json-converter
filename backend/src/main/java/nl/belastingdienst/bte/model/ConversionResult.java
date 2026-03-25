package nl.belastingdienst.bte.model;

import java.util.List;
import java.util.Map;

public class ConversionResult {
    private boolean success;
    private Map<String, Object> jsonOutput;
    private List<ValidationResult> validationResults;
    private Map<String, String> extractedFields;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Map<String, Object> getJsonOutput() { return jsonOutput; }
    public void setJsonOutput(Map<String, Object> jsonOutput) { this.jsonOutput = jsonOutput; }
    public List<ValidationResult> getValidationResults() { return validationResults; }
    public void setValidationResults(List<ValidationResult> validationResults) { this.validationResults = validationResults; }
    public Map<String, String> getExtractedFields() { return extractedFields; }
    public void setExtractedFields(Map<String, String> extractedFields) { this.extractedFields = extractedFields; }
}
