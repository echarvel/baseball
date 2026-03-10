package com.app.playerservicejava.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TeamGenerateRequest {

    @JsonProperty("seed_id")
    private String seedId;

    @JsonProperty("team_size")
    private int teamSize;

    public TeamGenerateRequest() {}

    public TeamGenerateRequest(String seedId, int teamSize) {
        this.seedId = seedId;
        this.teamSize = teamSize;
    }

    public String getSeedId() { return seedId; }
    public void setSeedId(String seedId) { this.seedId = seedId; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
}
