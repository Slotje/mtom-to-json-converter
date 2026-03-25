package nl.belastingdienst.bte.model;

public class FieldMapping {
    private String sourceField;
    private String targetProperty;
    private String dataType;
    private boolean required;
    private String format;
    private String defaultValue;
    private boolean multiValue;
    private String transformation;

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }
    public String getTargetProperty() { return targetProperty; }
    public void setTargetProperty(String targetProperty) { this.targetProperty = targetProperty; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public boolean isMultiValue() { return multiValue; }
    public void setMultiValue(boolean multiValue) { this.multiValue = multiValue; }
    public String getTransformation() { return transformation; }
    public void setTransformation(String transformation) { this.transformation = transformation; }
}
