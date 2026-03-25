package nl.belastingdienst.bte.model;

public class DetectedField {
    private String fieldName;
    private String xpathExpression;
    private String sampleValue;
    private String detectedType;

    public DetectedField() {}

    public DetectedField(String fieldName, String xpathExpression, String sampleValue, String detectedType) {
        this.fieldName = fieldName;
        this.xpathExpression = xpathExpression;
        this.sampleValue = sampleValue;
        this.detectedType = detectedType;
    }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getXpathExpression() { return xpathExpression; }
    public void setXpathExpression(String xpathExpression) { this.xpathExpression = xpathExpression; }
    public String getSampleValue() { return sampleValue; }
    public void setSampleValue(String sampleValue) { this.sampleValue = sampleValue; }
    public String getDetectedType() { return detectedType; }
    public void setDetectedType(String detectedType) { this.detectedType = detectedType; }
}
