package com.microsoftTeams.bot;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * class for storing build related information from jenkins
 */

public class BuildInfo {
    @JsonProperty("build_result")
    private String buildResult;

    @JsonProperty("build_user_id")
    private String buildUserId;

    @JsonProperty("build_number")
    private int buildNumber;

    @JsonProperty("build_url")
    private String buildUrl;

    // Constructor
    public BuildInfo() {}

    // Getters and Setters
    public String getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(String buildResult) {
        this.buildResult = buildResult;
    }

    public String getBuildUserId() {
        return buildUserId;
    }

    public void setBuildUserId(String buildUserId) {
        this.buildUserId = buildUserId;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }
}
