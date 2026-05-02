package com.example.demo1.rune.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RunePlayer {
    private String id;
    private String name;
    private List<RuneCard> hand = new ArrayList<>();
    private List<RuneCard> board = new ArrayList<>();
    private int mana = 1;
    private int health = 30;
    private int shield = 0;
    private boolean ready;
}
