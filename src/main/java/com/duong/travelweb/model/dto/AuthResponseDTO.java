package com.duong.travelweb.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AuthResponseDTO {
    /** Đăng ký xong nhưng phải xác thực email: không có token, kèm email đã gửi link. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean verificationRequired;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserDTO user;

    public Boolean getVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(Boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
