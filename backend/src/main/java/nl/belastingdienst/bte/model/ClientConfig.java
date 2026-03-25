package nl.belastingdienst.bte.model;

import java.util.List;

public class ClientConfig {
    private String clientId;
    private String clientName;
    private MetadataMapping metadataMapping;
    private BusinessInfo businessInfo;
    private ProcessingRules processingRules;

    // Getters and setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public MetadataMapping getMetadataMapping() { return metadataMapping; }
    public void setMetadataMapping(MetadataMapping metadataMapping) { this.metadataMapping = metadataMapping; }
    public BusinessInfo getBusinessInfo() { return businessInfo; }
    public void setBusinessInfo(BusinessInfo businessInfo) { this.businessInfo = businessInfo; }
    public ProcessingRules getProcessingRules() { return processingRules; }
    public void setProcessingRules(ProcessingRules processingRules) { this.processingRules = processingRules; }
}
