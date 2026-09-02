package com.perscholas.cashtran.dto;

public class RegistrationResponseDTO {

    private Long id;
    private Long accountId;
    private String username;
    private String email;
    private boolean activated;
    private boolean mfaRequired;
    private String mfaSetupUrl;
    private String enrollmentToken;

    public RegistrationResponseDTO(
            Long id,
            Long accountId,
            String username,
            String email,
            boolean activated,
            boolean mfaRequired,
            String mfaSetupUrl,
            String enrollmentToken) {

        this.id = id;
        this.accountId = accountId;
        this.username = username;
        this.email = email;
        this.activated = activated;
        this.mfaRequired = mfaRequired;
        this.mfaSetupUrl = mfaSetupUrl;
        this.enrollmentToken = enrollmentToken;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public String getMfaSetupUrl() {
        return mfaSetupUrl;
    }

    public String getEnrollmentToken() {
        return enrollmentToken;
    }
}

