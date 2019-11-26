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

    @PostMapping("/gamevsbot")
    public ResponseEntity<Map<String, Object>> createGameVsBot(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();
        HttpStatus status;

        if (getUser(authentication) != null) {
            Player user = playerRepository.findByUserName(getUser(authentication).getUserName());
            try {
                Game newGame = gameRepository.save(new Game(LocalDateTime.now()));
                GamePlayer newGP = gamePlayerRepository.save(new GamePlayer(newGame.getCreationDate(), newGame, user));

                GamePlayer botGP= gamePlayerRepository.save(new GamePlayer(LocalDateTime.now(), newGame, playerRepository.findById((long) 1).get()));

                botPlaceShips(botGP);

                gamePlayerRepository.save(botGP);

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

                                GamePlayer rivalGP= gp.getGame().getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != getUser(authentication).getId()).findFirst().orElse(null);

                                if(rivalGP != null) {
                                    if (rivalGP.getPlayer().isBot()) {

                                        rivalGP.addSalvo(botSendSalvo(rivalGP));

                                        gamePlayerRepository.save(rivalGP);
                                    }
                                }
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
                correctDistance = Ship.galeonLength - 1;
                break;
            case FRAGATA:
                correctDistance = Ship.fragataLength - 1;
                break;
            case GOLETA:
                correctDistance = Ship.goletaLength - 1;
                break;
            case CARABELA:
                correctDistance = Ship.carabelaLength - 1;
                break;
            case BERGANTIN:
                correctDistance = Ship.bergantinLength - 1;
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

    //FUNCION QUE DEVUELVE BARCOS CONTRARIOS HUNDIDOS
    private List<Map<String, Object>> didISinkSomething(GamePlayer gp) {
        Game game = gp.getGame();
        GamePlayer rivalGp = game.getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        List<Map<String, Object>> rtn = new ArrayList<>();
        List<Map<String,Object>> rivalSunkShips = new ArrayList<>();
        List<Map<String,Object>> mySunkShips = new ArrayList<>();
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

    //INDICA SI EL JUGADOR ES EL P1
    private boolean areYouP1(GamePlayer gp){
        GamePlayer rivalGP= gp.getGame().getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);

        return gp.getId() < rivalGP.getId();
    }

    //UBICA LOS BARCOS DEL BOT
    private boolean botPlaceShips(GamePlayer botgp){

        Random orientationPicker= new Random();
        String orientation;
        List<ShipTypes> type= Arrays.asList(ShipTypes.GALEON, ShipTypes.FRAGATA, ShipTypes.CARABELA, ShipTypes.GOLETA, ShipTypes.BERGANTIN);
        List<String> occupiedPos= new ArrayList<>();

        for(int i=0;i<5;i++) {
            if (orientationPicker.nextBoolean()) {
                orientation = "horizontal";
            } else {
                orientation = "vertical";
            }

            botgp.addShip(chooseBotShipLocations(orientation, type.get(i),occupiedPos));

            botgp.getShips().forEach(ship -> occupiedPos.addAll(ship.getLocations()));
        }

        return false;
    }

    //POR CADA BARCO ELIGE LAS UBICACIONES
    private Ship chooseBotShipLocations(String orientation, ShipTypes type,List<String> invalidPos){
        int numberOfCells;
        Ship newShip= new Ship();
        List<String> validCells= new Grid().getCells();
        validCells.removeAll(invalidPos);
        System.out.println("cualquier barco puede ocupar las celdas: " + validCells);

        switch(type){
            case BERGANTIN:
                numberOfCells= Ship.bergantinLength;
                newShip.setType(type);
                switch(orientation){
                    case "horizontal":
                        validCells.removeAll(Arrays.asList("A10","B10","C10","D10","E10","F10","G10","H10","I10","J10"));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells, true, invalidPos));
                        break;
                    case "vertical":
                        validCells.removeAll(Arrays.asList("J1","J2","J3","J4","J5","J6","J7","J8","J9","J10"));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,false, invalidPos));
                        break;
                }
                break;
            case CARABELA:
                numberOfCells= Ship.carabelaLength;
                newShip.setType(type);
                switch(orientation){
                    case "horizontal":
                        validCells.removeAll(createPosToErase(Grid.getLetters(), Arrays.asList("9","10")));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,true, invalidPos));
                        break;
                    case "vertical":
                        validCells.removeAll(createPosToErase(Arrays.asList("I","J"), Grid.getNumbers()));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,false, invalidPos));
                        break;
                }
                break;
            case GOLETA:
                numberOfCells= Ship.goletaLength;
                newShip.setType(type);
                switch(orientation){
                    case "horizontal":
                        validCells.removeAll(createPosToErase(Grid.getLetters(), Arrays.asList("9","10")));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,true, invalidPos));
                        break;
                    case "vertical":
                        validCells.removeAll(createPosToErase(Arrays.asList("I","J"), Grid.getNumbers()));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,false, invalidPos));
                        break;
                }
                break;
            case FRAGATA:
                numberOfCells= Ship.fragataLength;
                newShip.setType(type);
                switch(orientation){
                    case "horizontal":
                        validCells.removeAll(createPosToErase(Grid.getLetters(), Arrays.asList("8","9","10")));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,true, invalidPos));
                        break;
                    case "vertical":
                        validCells.removeAll(createPosToErase(Arrays.asList("H","I","J"), Grid.getNumbers()));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,false, invalidPos));
                        break;
                }
                break;
            case GALEON:
                numberOfCells= Ship.galeonLength;
                newShip.setType(type);
                switch(orientation){
                    case "horizontal":
                        validCells.removeAll(createPosToErase(Grid.getLetters(), Arrays.asList("7","8","9","10")));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,true, invalidPos));
                        break;
                    case "vertical":
                        validCells.removeAll(createPosToErase(Arrays.asList("G","H","I","J"), Grid.getNumbers()));
                        System.out.println("el barco: "+ type +" puede ocupar las celdas: " + validCells + " en forma "+ orientation);
                        newShip.setLocations(selectCells(numberOfCells, validCells,false, invalidPos));
                        break;
                }
        }

        return newShip;
    }

    //SELECCIONA LAS CELDAS PARA CADA BARCO
    private List<String> selectCells(Integer num, List<String> validCells, Boolean isHorizontal, List<String> invalidPos){
        List<String> rtn= new ArrayList<>();
        Random picker= new Random();
        List<String> allPositions= new Grid().getCells();
        String fstCell;
        validCells.removeAll(invalidPos);


        fstCell= validCells.get(picker.nextInt(validCells.size()));
        rtn.add(fstCell);


        if(isHorizontal){
            for(int i=1; i<num; i++){
                System.out.println(allPositions.get(allPositions.indexOf(fstCell)+i));

                rtn.add(allPositions.get(allPositions.indexOf(fstCell)+i));

                System.out.println(rtn);
            }
        }else{
            for(int i=1; i<num; i++){
                System.out.println(allPositions.get(allPositions.indexOf(fstCell)+10*i));
                rtn.add(allPositions.get(allPositions.indexOf(fstCell)+10*i));
                System.out.println(rtn);
            }
        }

        if(rtn.stream().anyMatch(cell -> invalidPos.contains(cell))){
            invalidPos.addAll(rtn);
            return selectCells(num, validCells, isHorizontal, invalidPos);
        }else{
            return rtn;
        }
    }

    //DADOS UNA LISTA DE LETRAS Y UNA DE NUMERO CREA LAS CELDAS
    static private List<String> createPosToErase(List<String> letters, List<String> numbers){
        List<String> rtn= new ArrayList<>();

        for(int i=0; i<letters.size(); i++){
            for(int j=0; j<numbers.size(); j++){
                rtn.add(letters.get(i) + numbers.get(j));
            }
        }

        return rtn;
    }

    //Crea y devuelve el Salvo del bot
    private Salvo botSendSalvo(GamePlayer gp){
        Salvo botSalvo= new Salvo();

        botSalvo.setTurn(setTurn(gp));

        botSalvo.setLocations(botSalvoLocations(gp));

        return botSalvo;
    }

    //Agrega al salvo las locations
    private Set<String> botSalvoLocations(GamePlayer gp){
        Set<String> rtn= new HashSet<>();
        List<String> firedSalvos= new ArrayList<>();
        List<String> fullGrid= new Grid().getCells();
        List<String> selectableCells= new ArrayList<>();
        int availableSalvos;

        gp.getSalvoes().forEach(salvo -> firedSalvos.addAll(salvo.getLocations()));
        fullGrid.removeAll(firedSalvos);
        selectableCells.addAll(fullGrid);

        availableSalvos= myAfloatBoats(gp);
        rtn= selectCellsToFire(availableSalvos, firedSalvos, selectableCells, gp);

        return rtn;
    }

    //Elige las celdas donde ira el nuevo salvo
    private Set<String> selectCellsToFire(int availableSalvos, List<String> firedSalvos, List<String> selectableCells, GamePlayer gp) {
        Set<String> rtn= new HashSet<>();
        Random picker= new Random();
        Grid grid= new Grid();
        List<String> hits= didIHitSomething(gp);
        List<String> sunkenShipsPositions= sunkenRivalShipsPositions(gp);

        //Crea las diagonales de cada cuadrante para disparar
        List<String> diagonalsC1= grid.createDiagonals(grid.getC1());
        List<String> diagonalsC2= grid.createDiagonals(grid.getC2());
        List<String> diagonalsC3= grid.createDiagonals(grid.getC3());
        List<String> diagonalsC4= grid.createDiagonals(grid.getC4());

        //Remueve de cada diagonal las ubicaciones donde ya se disparo
        diagonalsC1.removeAll(firedSalvos);
        diagonalsC2.removeAll(firedSalvos);
        diagonalsC3.removeAll(firedSalvos);
        diagonalsC4.removeAll(firedSalvos);

        //Remueve de las ubicaciones aquellas que pertenecen a un barco ya hundido
        hits.removeAll(sunkenShipsPositions);

        if(hits.size() == 0){
            //caso en el que aun no se golpeo ningún barco o ya se hundieron los golpeados anteriormente
            rtn= noHitsShootRandom(availableSalvos, diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);

        }else{
            //caso en el que hay barcos golpeados sin hundir
            if(hits.size() == 1){

                rtn.addAll(shootAroundOneHit(hits, firedSalvos, availableSalvos, diagonalsC1,diagonalsC2,diagonalsC3,diagonalsC4));

            }else{
                //Caso donde hay más de un HIT
                if(anyConsecutiveCells(hits)) {

                    List<List<String>> consecutiveHitsList = consecutiveHitCells(hits, firedSalvos);

                    if (consecutiveHitsList.size() == 1) {
                        rtn.add(suggestNxtCell(consecutiveHitsList.get(0), firedSalvos));

                        while (availableSalvos > rtn.size()) {

                            List<List<String>> quadrantsLessToMost = whichQuadrantsHasLessSalvos(diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);
                            List<String> randomQuad = quadrantsLessToMost.get(picker.nextInt(quadrantsLessToMost.size()));

                            rtn.add(randomQuad.get(picker.nextInt(randomQuad.size())));

                        }

                    } else {
                        for (List<String> list : consecutiveHitsList) {

                            rtn.add(suggestNxtCell(list, firedSalvos));

                        }

                        while (availableSalvos > rtn.size()) {

                            List<List<String>> quadrantsLessToMost = whichQuadrantsHasLessSalvos(diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);
                            List<String> randomQuad = quadrantsLessToMost.get(picker.nextInt(quadrantsLessToMost.size()));

                            rtn.add(randomQuad.get(picker.nextInt(randomQuad.size())));

                        }

                        if (rtn.size() > availableSalvos) {
                            List<String> aux = new ArrayList<>(rtn);
                            aux.removeIf(elem -> aux.indexOf(elem) > availableSalvos - 1);
                            rtn.clear();
                            rtn.addAll(aux);
                        }
                    }
                }else{
                    rtn= shootAroundOneHit(hits, firedSalvos, availableSalvos, diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);
                }

            }

        }

        return rtn;

    }

    //Devuelve los diagonales ordenadas de menor cantidad de tiros a mayor
    private List<List<String>> whichQuadrantsHasLessSalvos(List<String> diagonalsC1, List<String> diagonalsC2, List<String> diagonalsC3, List<String> diagonalsC4) {
        List<List<String>> quadrants= new ArrayList<>();

        quadrants.add(diagonalsC1);
        quadrants.add(diagonalsC2);
        quadrants.add(diagonalsC3);
        quadrants.add(diagonalsC4);

        quadrants.sort(Comparator.comparingInt(List::size));

        return quadrants;
    }

    //Devuelve la cantidad de barcos aun a flote que tiene el bot
    private int myAfloatBoats(GamePlayer gp){
        Game game= gp.getGame();
        GamePlayer rivalGP= game.getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        int sunken=0;
        List<String> allRivalSalvos= new ArrayList<>();

        for(Salvo salvo : rivalGP.getSalvoes()){

            allRivalSalvos.addAll(salvo.getLocations());

        }

        for(Ship ship : gp.getShips()){

            if(allRivalSalvos.containsAll(ship.getLocations())){
                sunken++;
            }
        }

        return 5-sunken;

    }


    //Devuelve las posiciones de los barcos hundidos del rival
    private List<String> sunkenRivalShipsPositions(GamePlayer gp){

        Game game= gp.getGame();
        GamePlayer rivalGp = game.getGamePlayers().stream().filter(gamePlayer -> gamePlayer.getPlayer().getId() != gp.getPlayer().getId()).findFirst().orElse(null);
        List<String> rivalSunkShips = new ArrayList<>();

        for (Ship ship : rivalGp.getShips()) {

            Set<String> ubicaciones = new HashSet<>(ship.getLocations());
            Set<String> allSalvos = new HashSet<>();

            for (Salvo salvo : gp.getSalvoes()) {
                allSalvos.addAll(salvo.getLocations());
            }

            if (allSalvos.containsAll(ubicaciones)) {
                rivalSunkShips.addAll(ship.getLocations());
            }
        }

        return rivalSunkShips;
    }

    //Devuelve una lista de listas con ubicaciones de hits consecutivos, de mayor a menor
    private List<List<String>> consecutiveHitCells(List<String> hits, List<String> alreadyFired){
        List<List<String>> rtn= new ArrayList<>();
        List<String> aux= new ArrayList<>();

        for(String cell : hits){
            List<String> adjacentCells= nextCells(cell);
            aux.add(cell);

            for(String adjCell : adjacentCells){
                if(hits.contains(adjCell)){
                    aux.add(adjCell);
                    Collections.sort(aux);
                    String orientation= discoverOrientation(aux);

                    if(orientation.equals("horizontal")){
                        String prevCell= nextAdjCellByDirection("lf", aux.get(0));
                        String nxtCell= nextAdjCellByDirection("rt", aux.get(aux.size()-1));
                        if( prevCell != null && hits.contains(prevCell) && !alreadyFired.contains(prevCell)){
                            aux.add(prevCell);
                        }

                        if( nxtCell != null && hits.contains(nxtCell) && !alreadyFired.contains(nxtCell)){
                            aux.add(nxtCell);
                        }
                    }else{
                        String upCell= nextAdjCellByDirection("up", aux.get(0));
                        String dwCell= nextAdjCellByDirection("dw", aux.get(aux.size()-1));
                        if(upCell != null && hits.contains(upCell) && !alreadyFired.contains(upCell)){
                            aux.add(upCell);
                        }

                        if(dwCell != null && hits.contains(dwCell) && !alreadyFired.contains(dwCell)){
                            aux.add(dwCell);
                        }
                    }
                }
//                rtn.add(aux);
//                hits.removeAll(aux);

            }
            rtn.add(aux);
            hits.removeAll(aux);
        }

        rtn.sort(Comparator.comparingInt(List::size));
        Collections.reverse(rtn);

        return rtn;
    }

    //Devuelve las celdas adyacentes a una celda
    private List<String> nextCells(String cell){
        List<String> rtn= new ArrayList<>();
        String cellLetter= cell.substring(0,1);
        String cellNumber= cell.substring(1);
        String cornerLU= "A1";
        String cornerLD= "J1";
        String cornerRU= "A10";
        String cornerRD= "J10";
        Grid grid= new Grid();
        int indexOfCellFullGrid= grid.getCells().indexOf(cell);

        if(cell.equals(cornerLU)) {
            rtn.addAll(Arrays.asList("A2","B1"));
            return rtn;
        }
        if(cell.equals(cornerLD)){
            rtn.addAll(Arrays.asList("J2","I1"));
            return rtn;
        }
        if(cell.equals(cornerRD)){
            rtn.addAll(Arrays.asList("J9","I10"));
            return rtn;
        }
        if(cell.equals(cornerRU)){
            rtn.addAll(Arrays.asList("A9","B10"));
            return rtn;
        }

        if(cellLetter.equals("A")){

            rtn.add(grid.getCells().get(indexOfCellFullGrid - 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 10));

            return rtn;
        }

        if(cellLetter.equals("J")){

            rtn.add(grid.getCells().get(indexOfCellFullGrid - 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid - 10));

            return rtn;
        }

        if(cellNumber.equals("1")){

            rtn.add(grid.getCells().get(indexOfCellFullGrid - 10));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 10));

            return rtn;
        }

        if(cellNumber.equals("10")){

            rtn.add(grid.getCells().get(indexOfCellFullGrid - 10));
            rtn.add(grid.getCells().get(indexOfCellFullGrid - 1));
            rtn.add(grid.getCells().get(indexOfCellFullGrid + 10));

            return rtn;
        }

        rtn.add(grid.getCells().get(indexOfCellFullGrid + 1));
        rtn.add(grid.getCells().get(indexOfCellFullGrid - 1));
        rtn.add(grid.getCells().get(indexOfCellFullGrid + 10));
        rtn.add(grid.getCells().get(indexOfCellFullGrid - 10));

        return rtn;
    }

    //Con una lista de dos celdas consecutivas descubre si la orientacion del barco es vertical u horizontal
    private String discoverOrientation(List<String> consecutiveCells){
        if(consecutiveCells.get(0).charAt(0) == (consecutiveCells.get(1).charAt(0))){
            return "horizontal";
        }else{
            return "vertical";
        }
    }

    //devuelve la celda adyacente en la direccion elegida, si no hay devuelve null
    private String nextAdjCellByDirection(String direction, String cell){
        List<String> fullGrid= new Grid().getCells();
        int idxFullGrid= fullGrid.indexOf(cell);
        try {
            switch (direction) {
                case "up":
                    return fullGrid.get(idxFullGrid - 10);

                case "dw":
                    return fullGrid.get(idxFullGrid + 10);

                case "lf":
                    return fullGrid.get(idxFullGrid - 1);

                case "rt":
                    return fullGrid.get(idxFullGrid + 1);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Dispara alrededor de un unico Hit en todas las direcciones, si sobran salvos dispara random
    private Set<String> shootAroundOneHit(List<String> hits, List<String> firedSalvos, int availableSalvos, List<String> diagonalsC1, List<String> diagonalsC2, List<String> diagonalsC3, List<String> diagonalsC4){
        Set<String> rtn= new HashSet<>();
        Random picker= new Random();

        List<String> adjacentCells= nextCells(hits.get(0));
        adjacentCells.removeAll(firedSalvos);

        for(int i=0; i<adjacentCells.size(); i++){
            if(availableSalvos > rtn.size()){
                rtn.add(adjacentCells.get(i));
            }
        }

        while(availableSalvos > rtn.size()){

            List<List<String>> quadrantsLessToMost = whichQuadrantsHasLessSalvos(diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);
            List<String> randomQuad= quadrantsLessToMost.get(picker.nextInt(quadrantsLessToMost.size()));

            rtn.add(randomQuad.get(picker.nextInt(randomQuad.size())));

        }
        return rtn;
    }

    //Si no hay hits para disparar alrededor diapara de forma aleatoria
    private Set<String> noHitsShootRandom(int availableSalvos, List<String> diagonalsC1, List<String> diagonalsC2, List<String> diagonalsC3, List<String> diagonalsC4) {

        Random picker= new Random();
        Set<String> rtn= new HashSet<>();

        switch(availableSalvos){
            case 5:
                String fstShot= diagonalsC1.get(picker.nextInt(diagonalsC1.size()));
                rtn.add(fstShot);
                diagonalsC1.remove(fstShot);

                rtn.add(diagonalsC2.get(picker.nextInt(diagonalsC2.size())));

                rtn.add(diagonalsC3.get(picker.nextInt(diagonalsC3.size())));

                rtn.add(diagonalsC4.get(picker.nextInt(diagonalsC4.size())));

                rtn.add(diagonalsC1.get(picker.nextInt(diagonalsC1.size())));
                break;

            case 4:

                rtn.add(diagonalsC1.get(picker.nextInt(diagonalsC1.size())));

                rtn.add(diagonalsC2.get(picker.nextInt(diagonalsC2.size())));

                rtn.add(diagonalsC3.get(picker.nextInt(diagonalsC3.size())));

                rtn.add(diagonalsC4.get(picker.nextInt(diagonalsC4.size())));

                break;

            case 3:
            case 2:
            case 1:

                List<List<String>> quadrantsLessToMost = whichQuadrantsHasLessSalvos(diagonalsC1, diagonalsC2, diagonalsC3, diagonalsC4);

                for(int i=0; i<availableSalvos;i++){
                    String selectedCell= quadrantsLessToMost.get(i).get(picker.nextInt(quadrantsLessToMost.get(i).size()));
                    rtn.add(selectedCell);
                }

                break;
        }
        return rtn;
    }

    //Sugiere la proxima celda para disparar a un barco
    private String suggestNxtCell(List<String> cells, List<String> alreadyShot){
        String orient= discoverOrientation(cells);

        if(orient.equals("horizontal")){
            String prevCell= nextAdjCellByDirection("lf", cells.get(0));
            String nxtCell= nextAdjCellByDirection("lf", cells.get(cells.size()-1));

            if(prevCell != null && prevCell.charAt(0) == cells.get(0).charAt(0) && !alreadyShot.contains(prevCell)){
                return prevCell;
            }
            if(nxtCell != null && nxtCell.charAt(0) == cells.get(cells.size()-1).charAt(0) && !alreadyShot.contains(nxtCell)){
                return nxtCell;
            }
        }else{
            String prevCell= nextAdjCellByDirection("up", cells.get(0));
            String nxtCell= nextAdjCellByDirection("dw", cells.get(cells.size()-1));

            if(prevCell != null && prevCell.substring(1).equals(cells.get(0).substring(1)) && !alreadyShot.contains(prevCell)){
                return prevCell;
            }
            if(nxtCell != null && nxtCell.substring(1).equals(cells.get(cells.size()-1)) && !alreadyShot.contains(nxtCell)){
                return nxtCell;
            }
        }
        return null;
    }

    private boolean anyConsecutiveCells(List<String> hitCells) {

        for(String cell : hitCells){
            for(String nxtCell : nextCells(cell)){
                if(hitCells.contains(nxtCell)){
                    return true;
                }
            }
        }
        return false;
    }




}




