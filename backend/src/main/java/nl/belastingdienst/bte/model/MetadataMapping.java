package nl.belastingdienst.bte.model;

import java.util.List;

public class MetadataMapping {
    private String objectStore;
    private String documentClass;
    private List<FieldMapping> fields;

    public String getObjectStore() { return objectStore; }
    public void setObjectStore(String objectStore) { this.objectStore = objectStore; }
    public String getDocumentClass() { return documentClass; }
    public void setDocumentClass(String documentClass) { this.documentClass = documentClass; }
    public List<FieldMapping> getFields() { return fields; }
    public void setFields(List<FieldMapping> fields) { this.fields = fields; }
}
