package com.app.playerservicejava.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Players implements Serializable {
    private List<Player> players;

    public Players() {
        this.players = new ArrayList<>();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
