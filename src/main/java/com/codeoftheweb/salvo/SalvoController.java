package com.codeoftheweb.salvo;

import com.codeoftheweb.salvo.repositories.GamePlayerRepository;
import com.codeoftheweb.salvo.repositories.GameRepository;
import com.codeoftheweb.salvo.repositories.PlayerRepository;
import com.codeoftheweb.salvo.repositories.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private SalvoApplication salvoApplication;
    @Autowired
    private ScoreRepository scoreRepository;

    public boolean isGuest(Authentication authentication) {
        return authentication == null;
    }

    //ENDPOINT PARA TRAER LA LISTA DE JUEGOS
    @RequestMapping("/games")
    public Map<String, Object> getAll(Authentication authentication) {

        Map<String, Object> dto = new LinkedHashMap<>();

        if (getUser(authentication) != null) {
            dto.put("player", getUser(authentication).playerDTO());
        } else {
            dto.put("player", "guest");
        }
        dto.put("games", gameRepository.findAll().stream().map(Game::gameDTO).collect(Collectors.toList()));
        return dto;
    }

    //ENDPOINT PARA CREAR UN NUEVO JUEGO
    @PostMapping("/games")
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (getUser(authentication) != null) {
            Player user = playerRepository.findByUserName(getUser(authentication).getUserName());
            try {
                Game newGame = gameRepository.save(new Game(LocalDateTime.now()));
                GamePlayer newGP = gamePlayerRepository.save(new GamePlayer(newGame.getCreationDate(), newGame, user));

                dto.put("GamePlayer_Id", newGP.getId());
                status = HttpStatus.CREATED;
            } catch (Exception e) {
                dto.put("Error", e.toString());
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            dto.put("Error", "You must be logged to create a game");
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(dto, status);
    }

    //UNE UN NUEVO PLAYER A UN JUEGO YA CREADO CON UN SOLO JUGADOR
    @PostMapping("/game/{gameid}/players")
    public ResponseEntity<Map<String, Object>> getPlayersByGame(@PathVariable Long gameid, Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (getUser(authentication) != null) {
            Player player = playerRepository.findByUserName(getUser(authentication).getUserName());
            if (gameRepository.findById(gameid).isPresent()) {
                Game game = gameRepository.findById(gameid).get();
                if (game.getGamePlayers().size() == 1) {
                    if (player.getId() != game.getGamePlayers().stream().findFirst().get().getPlayer().getId()) {
                        GamePlayer gp = gamePlayerRepository.save(new GamePlayer(LocalDateTime.now(), game, player));

                        dto.put("GamePLayerId", gp.getId());
                        status = HttpStatus.CREATED;
                    } else {
                        dto.put("Error", "You already are in the game");
                        status = HttpStatus.UNAUTHORIZED;
                    }
                } else {
                    dto.put("Error", "Game is full");
                    status = HttpStatus.UNAUTHORIZED;
                }
            } else {
                dto.put("Error", "No such game");
                status = HttpStatus.BAD_REQUEST;
            }
        } else {
            dto.put("Error", "You must be logged");
            status = HttpStatus.UNAUTHORIZED;
        }

        return new ResponseEntity<>(dto, status);
    }

    //ENDPOINT PARA TRAER LA INFO DE UN GAMEVIEW
    @RequestMapping("/game_view/{gameplayerid}")
    public ResponseEntity<Map<String, Object>> getGameView(@PathVariable Long gameplayerid, Authentication authentication) {
        GamePlayer gp = gamePlayerRepository.findById(gameplayerid).orElse(null);
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (gp != null) {
            if (authentication.getName().equals(gp.getPlayer().getUserName())) {
                dto = this.gameViewDTO(gp);
                status = HttpStatus.OK;
            } else {
                dto.put("Error", "UNAUTHORIZED");
                status = HttpStatus.UNAUTHORIZED;
            }
        } else {
            dto.put("error", "game NOT found");
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(dto, status);
    }

    //ENDPOINT PARA LA LISTA DE JUGADORES Y SUS SCORES
    @RequestMapping("/leaderboards")
    public List<Map<String, Object>> getLeaderboards() {
        return playerRepository.findAll().stream().map(Player::leaderboardDTO).collect(Collectors.toList());
    }

    //ENDPOINT NUEVO USUARIO
    @PostMapping("/players")
    public ResponseEntity<Map<String, Object>> registerPlayer(@RequestParam String regUsername, @RequestParam String regPassword) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (playerRepository.findByUserName(regUsername) == null) {
            if (!regUsername.isEmpty() && !regPassword.isEmpty()) {
                playerRepository.save(new Player(regUsername, salvoApplication.passwordEncoder().encode(regPassword)));
                response.put("OK", "User Creation Successful");

                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } else {
                response.put("Error", "Missing field information");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } else {
            response.put("Error", "User already exists");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
    }

    //ENDPOINT TO SET SHIPS POSITIONS
    @PostMapping("/games/players/{gamePlayerId}/ships")
    public ResponseEntity<Map<String, Object>> setShips(@PathVariable Long gamePlayerId, @RequestBody Set<Ship> ships, Authentication authentication) {
        GamePlayer gp = gamePlayerRepository.findById(gamePlayerId).orElse(null);
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (getUser(authentication) != null) {

            if (gp != null) {

                if (getUser(authentication).getId() == gp.getPlayer().getId()) {

                    int currentNumOfShips = gp.getShips().size();

                    if (currentNumOfShips != 5 && ships.size() == 5) {

                        if (areShipsSeparated(ships)) {

                            for (Ship ship : ships) {
                                if (isShipLocationOk(ship, ship.getLocations())) {
                                    gp.addShip(ship);
                                } else {
                                    dto.put("Error", ship.getType().toString() + " misplaced");
                                    status = HttpStatus.BAD_REQUEST;

                                    return new ResponseEntity<>(dto, status);
                                }
                            }

                            gamePlayerRepository.save(gp);

                            dto.put("Created", "Ships placed correctly");
                            status = HttpStatus.CREATED;
                        } else {
                            dto.put("Error", "Ships are outplaced");
                            status = HttpStatus.BAD_REQUEST;
                        }
                    } else {
                        dto.put("Error", "Too many Ships");
                        status = HttpStatus.FORBIDDEN;
                    }

                } else {
                    dto.put("Error", "Unauthorized");
                    status = HttpStatus.UNAUTHORIZED;
                }
            } else {
                dto.put("Error", "GamePlayer do not exists");
                status = HttpStatus.BAD_REQUEST;
            }
        } else {
            dto.put("Error", "You must be logged");
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(dto, status);
    }

    @PostMapping("/games/players/{gamePlayerId}/salvos")
    public ResponseEntity<Map<String, Object>> setSalvos(@PathVariable Long gamePlayerId, @RequestBody Salvo salvo, Authentication authentication) {
        GamePlayer gp = gamePlayerRepository.findById(gamePlayerId).orElse(null);
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (getUser(authentication) != null) {

            if (gp != null) {

                if (getUser(authentication).getId() == gp.getPlayer().getId()) {

                    if(gp.getGame().getScores().size() == 0) {

                        if (isSalvoOk(salvo)) {
                            if (setTurn(gp) != -1) {

                                salvo.setTurn(setTurn(gp));
                                gp.addSalvo(salvo);
                                gamePlayerRepository.save(gp);

                                dto.put("OK", "Salvo Created");
                                status = HttpStatus.CREATED;

                            } else {
                                dto.put("Error", "Turn already played");
                                status = HttpStatus.ALREADY_REPORTED;
                            }
                        } else {
                            dto.put("Error", "Salvo misplaced");
                            status = HttpStatus.FORBIDDEN;
                        }

                    }else{
                        dto.put("Error","GAMEOVER");
                        status= HttpStatus.FORBIDDEN;
                    }
                } else {
                    dto.put("Error", "Unauthorized");
                    status = HttpStatus.UNAUTHORIZED;
                }
            } else {
                dto.put("Error", "GamePlayer do not exists");
                status = HttpStatus.BAD_REQUEST;
            }
        } else {
            dto.put("Error", "You must be logged");
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(dto, status);
    }

    //FUNCION DTO DE GAME VIEW, LLAMADA POR EL REQUEST GAMEVIEW
    private Map<String, Object> gameViewDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<>();

        dto.put("id", gamePlayer.getGame().getId());
        dto.put("creationDate", gamePlayer.getGame().getCreationDate());
        dto.put("gamePlayer", gamePlayer.getGame().getGamePlayers().stream().map(GamePlayer::gamePlayerDTO).collect(Collectors.toList()));
        dto.put("player", gamePlayer.getPlayer().getUserName());
        dto.put("ships", gamePlayer.getShips().stream().map(Ship::shipDTO));
        dto.put("salvoes", gamePlayer.getGame().getGamePlayers().stream().flatMap(gp -> gp.getSalvoes().stream().map(Salvo::salvoDTO)));
        dto.put("hits", didIHitSomething(gamePlayer));
        dto.put("sunks", didISinkSomething(gamePlayer));
        dto.put("state", stateOfGame(gamePlayer));
        if(gamePlayer.getGame().getScores().size() != 0){
            dto.put("score",gamePlayer.getGame().getScores().stream().filter(score -> score.getPlayer().getId() == gamePlayer.getPlayer().getId()).findFirst().get().getScore());
        }

        return dto;
    }

    //FUNCION QUE DEVUELVE EL USUARIO LOGUEADO O NULL SI NO HAY NADIE
    private Player getUser(Authentication authentication) {
        return authentication != null ? playerRepository.findByUserName(authentication.getName()) : null;
    }

    //CHEQUEA SI LA POSICION DEL BARCO ES HORIZONTAL
    private boolean isHorizontal(List<String> locations) {
        return locations.get(0).split("")[0].equals(locations.get(1).split("")[0]);
    }

    //CHEQUEA QUE LAS POSICIONES DE CADA BARCO SEAN DISTINTAS Y CORRELATIVAS
    private boolean isShipLocationOk(Ship ship, List<String> locations) {
        Set<String> check = new HashSet<>(locations);
        boolean rtn = false;

        if (locations.size() == check.size()) {
            if (isHorizontal(locations)) {
                int fstCell = Integer.parseInt(locations.get(0).split("")[1]);
                int lstCell = Integer.parseInt(locations.get(locations.size() - 1).substring(1));

                for (String location : locations) {
                    int cellNumber = Integer.parseInt(location.substring(1));
                    if (isLengthOk(ship, lstCell - fstCell) && cellNumber >= fstCell && cellNumber <= lstCell) {
                        rtn = true;
                    } else {
                        return false;
                    }
                }
            } else {
                int fstCell = locations.get(0).charAt(0);
                int lstCell = locations.get(locations.size() - 1).charAt(0);

                for (String location : locations) {
                    int cellLetter = location.charAt(0);
                    if (isLengthOk(ship, lstCell - fstCell) && cellLetter >= fstCell && cellLetter <= lstCell) {
                        rtn = true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return rtn;
    }

    //CHEQUEA QUE LOS BARCOS NO SE SUPERPONGAN
    private boolean areShipsSeparated(Set<Ship> ships) {
        List<String> locationsList = new ArrayList<>();
        Set<String> locationsSet = new LinkedHashSet<>();

        ships.forEach(ship -> ship.getLocations().forEach(location -> {
            locationsList.add(location);
            locationsSet.add(location);
        }));

        return locationsList.size() == locationsSet.size();
    }

    //CHEQUEA QUE EL LARGO DEL BARCO SEA CORRECTO
    private boolean isLengthOk(Ship ship, int distanceFstLst) {
        int correctDistance;

        switch (ship.getType()) {
            case GALEON:
                correctDistance = Ship.galeonLentgh - 1;
                break;
            case FRAGATA:
                correctDistance = Ship.fragataLentgh - 1;
                break;
            case GOLETA:
                correctDistance = Ship.goletaLentgh - 1;
                break;
            case CARABELA:
                correctDistance = Ship.carabelaLentgh - 1;
                break;
            case BERGANTIN:
                correctDistance = Ship.bergantinLentgh - 1;
                break;
            default:
                correctDistance = 155;
                break;
        }
        return distanceFstLst == correctDistance;
    }

    //CHEQUEA QUE LOS DISPAROS SEAN CORRECTOS
    private boolean isSalvoOk(Salvo salvo) {
        List<String> correctLetters = new ArrayList<String>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"));
        Range<Integer> correctNumbers = Range.<Integer>from(Range.Bound.inclusive(1)).to(Range.Bound.inclusive(10));
        List<Boolean> rtn = new ArrayList<Boolean>();


        if (salvo.getLocations().size() <= 5 && salvo.getLocations().size() > 0) {
            for (String location : salvo.getLocations()) {
                if (correctLetters.contains(location.split("")[0]) && correctNumbers.contains(Integer.parseInt(location.substring(1)))) {
                    rtn.add(true);
                } else {
                    rtn.add(false);
                }
            }
        } else {
            rtn.add(false);
        }

        return !rtn.contains(false);
    }

    //DEVUELVE EL TURNO DEL SALVO NUEVO O -1 SI EL TURNO YA SE JUGÓ
    private int setTurn(GamePlayer gp) {
        int turn = gp.getSalvoes().size() + 1;
        List<Integer> prevTurns = new ArrayList<>();
        int rtn;

        for (Salvo salvo : gp.getSalvoes()) {
            prevTurns.add(salvo.getTurn());
        }

        if (prevTurns.contains(turn)) {
            rtn = -1;
        }

        return turn;
    }

    //FUNCION QUE DEVUELVE LOS HITS A BARCOS CONTRARIOS
    private List<String> didIHitSomething(GamePlayer gp) {
        Game game = gp.getGame();
        GamePlayer rivalGp = game.getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        List<String> rivalLocations = new ArrayList<>();
        List<String> rtn = new ArrayList<>();

        if (rivalGp != null) {
            Set<Ship> rivalShips = rivalGp.getShips();

            rivalShips.forEach(ship -> rivalLocations.addAll(ship.getLocations()));

            for (Salvo salvo : gp.getSalvoes()) {
                for (String location : salvo.getLocations()) {
                    if (rivalLocations.contains(location)) {
                        rtn.add(location);
                    }
                }
            }
        }
        return rtn;
    }

    private List<Map<String, Object>> didISinkSomething(GamePlayer gp) {
        Game game = gp.getGame();
        GamePlayer rivalGp = game.getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        List<Map<String, Object>> rtn = new ArrayList<>();
        List<Object> rivalSunkShips = new ArrayList<>();
        List<Object> mySunkShips = new ArrayList<>();
        Map<String, Object> dto = new HashMap<>();
        Map<String, Object> shipInfo = new HashMap<>();

        if (rivalGp != null) {
            if (gp.getSalvoes().size() > 0) {
                for (Ship ship : rivalGp.getShips()) {

                    Set<String> ubicaciones = new HashSet<>(ship.getLocations());
                    Set<String> allSalvos = new HashSet<>();

                    for (Salvo salvo : gp.getSalvoes()) {
                        allSalvos.addAll(salvo.getLocations());
                    }

                    if (allSalvos.containsAll(ubicaciones)) {
                        //shipInfo.put("type", ship.getType());
                        //rivalSunkShips.add(ship.getType().toString());
                        rivalSunkShips.add(ship.shipDTO());
                    }
                }
            }
            if (rivalGp.getSalvoes().size() > 0) {
                for (Ship ship : gp.getShips()) {

                    Set<String> ubicaciones = new HashSet<>(ship.getLocations());
                    Set<String> allSalvos = new HashSet<>();

                    for (Salvo salvo : rivalGp.getSalvoes()) {
                        allSalvos.addAll(salvo.getLocations());
                    }

                    if (allSalvos.containsAll(ubicaciones)) {
                        //shipInfo.put("type", ship.getType());
                        //mySunkShips.add(ship.getType().toString());
                        mySunkShips.add(ship.shipDTO());
                    }
                }

            }
        }

        if(gp.getGame().getScores().size() < 2) {
            if (rivalSunkShips.size() == 5) {
                if (mySunkShips.size() < 5 && gp.getSalvoes().size() == rivalGp.getSalvoes().size()) {
                    scoreRepository.save(new Score(gp.getPlayer(), gp.getGame(), LocalDateTime.now(), Score.scoreType.WIN));
                }
                if (mySunkShips.size() == 5 && gp.getSalvoes().size() == rivalGp.getSalvoes().size()) {
                    scoreRepository.save(new Score(gp.getPlayer(), gp.getGame(), LocalDateTime.now(), Score.scoreType.TIE));
                }
            } else {
                if (mySunkShips.size() == 5 && gp.getSalvoes().size() == rivalGp.getSalvoes().size()) {
                    scoreRepository.save(new Score(gp.getPlayer(), gp.getGame(), LocalDateTime.now(), Score.scoreType.LOSS));
                }
            }
        }

        dto.put("rival", rivalSunkShips);
        dto.put("mine", mySunkShips);
        rtn.add(dto);
        return rtn;
    }

    //DEVUELVE EL ESTADO DEL JUEGO
    private String stateOfGame(GamePlayer gamePlayer){
        GamePlayer gp= gamePlayerRepository.findById(gamePlayer.getId()).get();
        Game game= gp.getGame();
        GamePlayer rivalGP= game.getGamePlayers().stream().filter(gPlayer -> gPlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        String stateOfGame;


        if(rivalGP != null){
            if(gp.getShips().size() != 0) {
                if (rivalGP.getShips().size() != 0){
                    if(rivalGP.getSalvoes().size() == gp.getSalvoes().size()){
                        if (areYouP1(gamePlayer)) {
                            stateOfGame = "EnterSalvo";
                        } else {
                            stateOfGame = "WaitingForOpponent";
                        }
                        if (game.getScores().size() != 0) {
                            stateOfGame = "GAMEOVER";
                        }
                    }else{
                        if (!areYouP1(gamePlayer)) {
                            stateOfGame = "EnterSalvo";
                        } else {
                            stateOfGame = "WaitingForOpponent";
                        }
                        if (game.getScores().size() == 2) {
                            stateOfGame = "GAMEOVER";
                        }
                    }
                } else {
                    stateOfGame = "WaitingForOpponent";
                }
            }else{
                stateOfGame= "PlaceShips";
            }
        }else{
            stateOfGame= "NoRival";
        }

        return stateOfGame;
    }


    private boolean areYouP1(GamePlayer gp){
        GamePlayer rivalGP= gp.getGame().getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);

        return gp.getId() < rivalGP.getId();
    }
}



