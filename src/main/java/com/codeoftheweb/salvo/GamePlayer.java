package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private LocalDateTime joinDate;

    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    @OneToMany(mappedBy="gamePlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Ship> ships= new HashSet<>();

    @OneToMany(mappedBy="gamePlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Salvo> salvoes= new HashSet<>();

    public GamePlayer(){
        this.joinDate= LocalDateTime.now();
    }

    public GamePlayer(LocalDateTime join, Game juego, Player jugador) {
        this.joinDate = join;
        this.game = juego;
        this.player = jugador;
    }

    public GamePlayer(LocalDateTime join, Game juego, Player jugador, Ship ship){
        this.joinDate= join;
        this.game= juego;
        this.player= jugador;
        this.addShip(ship);
    }

    public GamePlayer(LocalDateTime join, Game juego, Player jugador, Ship ship, List<String> locations){
        this.joinDate= join;
        this.game= juego;
        this.player= jugador;
        this.addShip(ship);
        ship.setLocations(locations);
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public Set<Ship> getShips() {
        return ships;
    }


    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public Set<Salvo> getSalvoes() {
        return salvoes;
    }

    public void setSalvoes(Set<Salvo> salvoes) {
        this.salvoes = salvoes;
    }

    public Map<String,Object> gamePlayerDTO(){
        Map<String,Object> dto= new LinkedHashMap<>();

        dto.put("id", this.id);
        dto.put("player", this.player.playerDTO());
        if(this.player.getScore(this.game).isPresent()) {
            dto.put("score", this.player.getScore(this.game).get().scoreDTO());
        }else{
            dto.put("score", "null");
        }
        return dto;
    }

    public void addShip(Ship ship){
        this.ships.add(ship);
        ship.setGamePlayer(this);
    }

    public void addSalvo(Salvo salvo){
        this.salvoes.add(salvo);
        salvo.setGamePlayer(this);
    }

}
