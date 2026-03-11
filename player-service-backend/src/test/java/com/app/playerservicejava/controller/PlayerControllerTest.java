package com.app.playerservicejava.controller;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    private Player makePlayer(String id, String firstName, String lastName, String country) {
        Player p = new Player();
        p.setPlayerId(id);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setBirthCountry(country);
        return p;
    }

    @Test
    void getPlayers_returns200WithPlayerList() throws Exception {
        Players players = new Players();
        players.getPlayers().add(makePlayer("aaronha01", "Hank", "Aaron", "USA"));
        players.getPlayers().add(makePlayer("ruthba01", "Babe", "Ruth", "USA"));

        when(playerService.getPlayers()).thenReturn(players);

        mockMvc.perform(get("/v1/players"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[0].playerId").value("aaronha01"))
                .andExpect(jsonPath("$.players[0].firstName").value("Hank"))
                .andExpect(jsonPath("$.players[1].playerId").value("ruthba01"));
    }

    @Test
    void getPlayers_returnsEmptyList() throws Exception {
        when(playerService.getPlayers()).thenReturn(new Players());

        mockMvc.perform(get("/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(0)));
    }

    @Test
    void getPlayerById_returns200WhenFound() throws Exception {
        Player player = makePlayer("aaronha01", "Hank", "Aaron", "USA");
        when(playerService.getPlayerById("aaronha01")).thenReturn(Optional.of(player));

        mockMvc.perform(get("/v1/players/aaronha01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("aaronha01"))
                .andExpect(jsonPath("$.firstName").value("Hank"))
                .andExpect(jsonPath("$.lastName").value("Aaron"))
                .andExpect(jsonPath("$.birthCountry").value("USA"));
    }

    @Test
    void getPlayerById_returns404WhenNotFound() throws Exception {
        when(playerService.getPlayerById("nobody99")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/players/nobody99"))
                .andExpect(status().isNotFound());
    }
}
