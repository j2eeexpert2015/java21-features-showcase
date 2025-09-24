package org.example.dto.template;

public class TemplateResponse {
    private String templateType;
    private String templateSource;
    private String generatedContent;
    private String processorUsed;
    private String securityStatus;

    public TemplateResponse() {}

    public TemplateResponse(String templateType, String templateSource, String generatedContent,
                            String processorUsed, String securityStatus) {
        this.templateType = templateType;
        this.templateSource = templateSource;
        this.generatedContent = generatedContent;
        this.processorUsed = processorUsed;
        this.securityStatus = securityStatus;
    }

    // Getters and setters
    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateSource() {
        return templateSource;
    }

    public void setTemplateSource(String templateSource) {
        this.templateSource = templateSource;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getProcessorUsed() {
        return processorUsed;
    }

    public void setProcessorUsed(String processorUsed) {
        this.processorUsed = processorUsed;
    }

    public String getSecurityStatus() {
        return securityStatus;
    }

    public void setSecurityStatus(String securityStatus) {
        this.securityStatus = securityStatus;
    }
}