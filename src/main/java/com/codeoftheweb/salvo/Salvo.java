package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
public class Salvo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_Id")
    private GamePlayer gamePlayer;

    private int turn;

    @ElementCollection
    private Set<String> locations = new HashSet<>();

    public Salvo(){}

    public Salvo(String[] locations){
        this.addLocation(locations);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public Set<String> getLocations() {
        return locations;
    }

    public void setLocations(Set<String> locations) {
        this.locations = locations;
    }

    public void addLocation(String[] location){
        Collections.addAll(this.locations, location);
    }

    public Map<String, Object> salvoDTO(){
        Map<String,Object> dto= new LinkedHashMap<>();

        dto.put("turn", this.turn);
        dto.put("player", this.gamePlayer.getPlayer().getId());
        dto.put("locations", this.getLocations());

        return dto;
    }
}
