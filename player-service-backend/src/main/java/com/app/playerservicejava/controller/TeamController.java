package com.app.playerservicejava.controller;

import com.app.playerservicejava.model.TeamFeedbackRequest;
import com.app.playerservicejava.model.TeamFeedbackResponse;
import com.app.playerservicejava.model.TeamGenerateResponse;
import com.app.playerservicejava.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "v1/team", produces = { MediaType.APPLICATION_JSON_VALUE })
public class TeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping("/generate")
    public ResponseEntity<TeamGenerateResponse> generateTeam(
            @RequestParam("seedId") String seedId,
            @RequestParam(value = "teamSize", defaultValue = "10") int teamSize) {
        return ResponseEntity.ok(teamService.generateTeam(seedId, teamSize));
    }

    @PostMapping("/feedback")
    public ResponseEntity<TeamFeedbackResponse> submitFeedback(@RequestBody TeamFeedbackRequest request) {
        return ResponseEntity.ok(teamService.submitFeedback(request));
    }
}
