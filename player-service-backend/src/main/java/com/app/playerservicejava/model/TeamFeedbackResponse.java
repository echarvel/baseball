package com.app.playerservicejava.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TeamFeedbackResponse {

    @JsonProperty("seed_id")
    private String seedId;

    @JsonProperty("prediction_id")
    private String predictionId;

    @JsonProperty("member_id")
    private String memberId;

    private boolean accepted;

    public String getSeedId() { return seedId; }
    public void setSeedId(String seedId) { this.seedId = seedId; }
    public String getPredictionId() { return predictionId; }
    public void setPredictionId(String predictionId) { this.predictionId = predictionId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
