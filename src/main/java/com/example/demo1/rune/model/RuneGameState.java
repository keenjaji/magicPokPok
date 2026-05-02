package com.example.demo1.rune.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RuneGameState {
    private String gameId;
    private List<RunePlayer> players = new ArrayList<>();
    private List<RuneCard> deck = new ArrayList<>();
    private String phase = "LOBBY"; // LOBBY, DRAW, PLAY, RESOLVE, END
    private int turn = 1;
    private String activePlayerId;
    private List<String> logs = new ArrayList<>();
}
