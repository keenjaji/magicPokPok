package com.example.demo1.rune.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuneCard {
    private String id;
    private String runeType; // FIRE, WATER, EARTH, AIR, LIGHT, DARK
    private int power;
    private boolean activated;
}
