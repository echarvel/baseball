package com.app.playerservicejava.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TeamGenerateResponse {

    @JsonProperty("seed_id")
    private String seedId;

    @JsonProperty("prediction_id")
    private String predictionId;

    @JsonProperty("team_size")
    private int teamSize;

    @JsonProperty("member_ids")
    private List<String> memberIds;

    public String getSeedId() { return seedId; }
    public void setSeedId(String seedId) { this.seedId = seedId; }
    public String getPredictionId() { return predictionId; }
    public void setPredictionId(String predictionId) { this.predictionId = predictionId; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
}
