package com.app.playerservicejava.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TeamFeedbackRequest {

    @JsonProperty("seed_id")
    private String seedId;

    @JsonProperty("member_id")
    private String memberId;

    private int feedback;

    public String getSeedId() { return seedId; }
    public void setSeedId(String seedId) { this.seedId = seedId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public int getFeedback() { return feedback; }
    public void setFeedback(int feedback) { this.feedback = feedback; }
}
