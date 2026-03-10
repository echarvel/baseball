package com.app.playerservicejava.service;

import com.app.playerservicejava.model.TeamFeedbackRequest;
import com.app.playerservicejava.model.TeamFeedbackResponse;
import com.app.playerservicejava.model.TeamGenerateRequest;
import com.app.playerservicejava.model.TeamGenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.playerservicejava.config.MlServiceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TeamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeamService.class);

    @Autowired
    private MlServiceProperties mlServiceProperties;

    @Autowired
    private RestTemplate restTemplate;

    public TeamGenerateResponse generateTeam(String seedId, int teamSize) {
        TeamGenerateRequest request = new TeamGenerateRequest(seedId, teamSize);
        return restTemplate.postForObject(mlServiceProperties.getUrl() + "/team/generate", request, TeamGenerateResponse.class);
    }

    public TeamFeedbackResponse submitFeedback(TeamFeedbackRequest request) {
        return restTemplate.postForObject(mlServiceProperties.getUrl() + "/team/feedback", request, TeamFeedbackResponse.class);
    }
}
