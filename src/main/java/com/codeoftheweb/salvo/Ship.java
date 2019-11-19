package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
public class Ship {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private ShipTypes type;

    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_id")
    private GamePlayer gamePlayer;

    @ElementCollection
    @Column(name="location")
    private List<String> locations= new ArrayList<>();

    static public int goletaLength = 3;
    static public int galeonLength = 5;
    static public int fragataLength = 4;
    static public int carabelaLength = 3;
    static public int bergantinLength = 2;

    public Ship(){}

    public Ship(ShipTypes type){
        this.type= type;
    }

    public Ship(ShipTypes type, List<String> locations){this.type= type; this.locations= locations;}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ShipTypes getType() {
        return type;
    }

    public void setType(ShipTypes type) {
        this.type = type;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public void addLocation(String[] location){
        this.locations.addAll(Arrays.asList(location));
    }

    public Map<String,Object> shipDTO(){
        Map<String,Object> dto= new LinkedHashMap<>();

        dto.put("ship", this.getType());
        dto.put("locations", this.getLocations());

        return dto;
    }
}
