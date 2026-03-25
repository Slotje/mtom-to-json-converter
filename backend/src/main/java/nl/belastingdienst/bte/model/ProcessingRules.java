package nl.belastingdienst.bte.model;

public class ProcessingRules {
    private int retentionDays = 30;
    private boolean autoRetryEnabled = true;
    private boolean processingEnabled = true;

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public boolean isAutoRetryEnabled() { return autoRetryEnabled; }
    public void setAutoRetryEnabled(boolean autoRetryEnabled) { this.autoRetryEnabled = autoRetryEnabled; }
    public boolean isProcessingEnabled() { return processingEnabled; }
    public void setProcessingEnabled(boolean processingEnabled) { this.processingEnabled = processingEnabled; }
}
