package nl.belastingdienst.bte.model;

public class BusinessInfo {
    private String contactEmail;
    private String supportGroup;
    private Integer maxMessageSize;
    private Integer maxMessagesPerDay;

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getSupportGroup() { return supportGroup; }
    public void setSupportGroup(String supportGroup) { this.supportGroup = supportGroup; }
    public Integer getMaxMessageSize() { return maxMessageSize; }
    public void setMaxMessageSize(Integer maxMessageSize) { this.maxMessageSize = maxMessageSize; }
    public Integer getMaxMessagesPerDay() { return maxMessagesPerDay; }
    public void setMaxMessagesPerDay(Integer maxMessagesPerDay) { this.maxMessagesPerDay = maxMessagesPerDay; }
}
