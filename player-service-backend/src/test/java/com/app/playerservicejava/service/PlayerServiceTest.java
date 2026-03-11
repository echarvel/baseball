package com.app.playerservicejava.service;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private Player makePlayer(String id, String firstName, String lastName) {
        Player p = new Player();
        p.setPlayerId(id);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        return p;
    }

    @Test
    void getPlayers_returnsAllPlayers() {
        Player p1 = makePlayer("aaronha01", "Hank", "Aaron");
        Player p2 = makePlayer("ruthba01", "Babe", "Ruth");
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));

        Players result = playerService.getPlayers();

        assertThat(result.getPlayers()).hasSize(2);
        assertThat(result.getPlayers().get(0).getPlayerId()).isEqualTo("aaronha01");
        assertThat(result.getPlayers().get(1).getPlayerId()).isEqualTo("ruthba01");
        verify(playerRepository).findAll();
    }

    @Test
    void getPlayers_returnsEmptyWhenNoPlayers() {
        when(playerRepository.findAll()).thenReturn(List.of());

        Players result = playerService.getPlayers();

        assertThat(result.getPlayers()).isEmpty();
    }

    @Test
    void getPlayerById_returnsPlayerWhenFound() {
        Player player = makePlayer("aaronha01", "Hank", "Aaron");
        when(playerRepository.findById("aaronha01")).thenReturn(Optional.of(player));

        Optional<Player> result = playerService.getPlayerById("aaronha01");

        assertThat(result).isPresent();
        assertThat(result.get().getPlayerId()).isEqualTo("aaronha01");
        assertThat(result.get().getFirstName()).isEqualTo("Hank");
    }

    @Test
    void getPlayerById_returnsEmptyWhenNotFound() {
        when(playerRepository.findById("nobody99")).thenReturn(Optional.empty());

        Optional<Player> result = playerService.getPlayerById("nobody99");

        assertThat(result).isEmpty();
    }

    @Test
    void getPlayerById_returnsEmptyOnException() {
        when(playerRepository.findById("bad")).thenThrow(new RuntimeException("DB error"));

        Optional<Player> result = playerService.getPlayerById("bad");

        assertThat(result).isEmpty();
    }
}
