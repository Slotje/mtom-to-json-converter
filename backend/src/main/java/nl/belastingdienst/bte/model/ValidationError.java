package nl.belastingdienst.bte.model;

public class ValidationError {
    private String code;
    private String message;
    private String suggestion;
    private String severity;

    public ValidationError() {}

    public ValidationError(String code, String message, String suggestion, String severity) {
        this.code = code;
        this.message = message;
        this.suggestion = suggestion;
        this.severity = severity;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
