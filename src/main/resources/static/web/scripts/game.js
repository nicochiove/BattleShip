let jsonGamePlayer= "";

//TOMA EL GP DE LA URL
function getParamGP(){
	
	let param= (new URL(document.location)).searchParams;
	let gp= parseInt(param.get('gp'));

	return gp;
}

//TRAE LA INFO DEL JUEGO DSP DE CADA SALVO DISPARADO
function afterShot(){
    fetch("/api/game_view/"+getParamGP())
    .then(function(response){
        return response.json();
    })
    .then(function(json){
        jsonGamePlayer= json;
        setSalvoes(jsonGamePlayer);
        document.getElementById('place_ships').style.display= 'none'
        esperaAlRival()
        //showAvailableSalvo(json)
        //showShootButton()
    })
}

//ACTUALIZA EL JSON CON LA INFO DEL JUEGO
function refreshGameData(){
    fetch("/api/game_view/"+getParamGP())
    .then(function(response){
        return response.json();
    })
    .then(function(json){
        jsonGamePlayer= json;
        gameContinue()
    })
}


//REALIZA EL FETCH PARA TRAER LA INFO DEL JUEGO
function getShipsPositions(){

    fetch("/api/game_view/"+getParamGP())
    .then(function(response){
        return response.json();
    })
    .then(function(json){
        jsonGamePlayer= json;
        setShips(jsonGamePlayer);
        setSalvoes(jsonGamePlayer);
        gameContinue()
    })
}

//PONE LOS BARCOS EN SU LUGAR
function setShips(obj){
    if(obj.ships.length !== 0){
        showSalvoGrid()        
        document.getElementById('place_ships').style.display= 'none'
        for(ship in obj.ships){
            createShips(obj.ships[ship].ship.toLowerCase(), getShipLength(obj.ships[ship].ship), getOrientation(obj.ships[ship].locations), document.getElementById(`ships${obj.ships[ship].locations[0]}`), true);
        }
        //showAvailableSalvo(obj)
        //showShootButton()
    }else{
        createShips('galeon', 5, 'horizontal', document.getElementById('dock'),false)
        createShips('fragata', 4, 'horizontal', document.getElementById('dock'),false)
        createShips('carabela', 3, 'horizontal', document.getElementById('dock'),false)
        createShips('goleta', 3, 'horizontal', document.getElementById('dock'),false)
        createShips('bergantin', 2, 'horizontal', document.getElementById('dock'),false)
        document.getElementById('place_ships').style.display= 'block'
    }

}

//DEVUELVE EL LARGO DE LOS BARCOS DEPENDIENDO SU TIPO
function getShipLength(type){
    let shipLength;
    switch(type){
        case "BERGANTIN":
            shipLength= 2;
        break;
        case "GOLETA":
            shipLength= 3;
        break;
        case "FRAGATA":
            shipLength= 4;
        break;
        case "GALEON":
            shipLength= 5;
        break;
        case "CARABELA":
            shipLength= 3;
        break;
    }

    return shipLength;
}

//DEVUELVE LA ORIENTACION DE UN BARCO DEPENDIENDO DE LAS CELDAS QUE OCUPA
function getOrientation(positions){

    let pos1= positions[0].split("");
    let pos2= positions[1].split("");
    let orient= "";

    if(pos1[0] === pos2[0]){
        orient= 'horizontal'
    }else{
        orient= 'vertical'
    }

    return orient
}

//CREA UN OBJETO CON LAS UBICACIONES DE LOS BARCOS
function setShipsPositions(){
    let ships= [];

    ships.push({type: "BERGANTIN", locations: getShipsLocations("bergantin")})
    ships.push({type: "FRAGATA", locations: getShipsLocations("fragata")})
    ships.push({type: "GALEON", locations: getShipsLocations("galeon")})
    ships.push({type: "CARABELA", locations: getShipsLocations("carabela")})
    ships.push({type: "GOLETA", locations: getShipsLocations("goleta")})

    return ships
}

//DEVUELVE LAS CELDAS DONDE SE UBICARON LOS BARCOS
function getShipsLocations(tipo){
    let cells= document.querySelectorAll(`.${tipo}-busy-cell`)
    let locations= []

    cells.forEach(cell => locations.push(`${cell.dataset.y}${cell.dataset.x}`))

    return locations

}

//REALIZA EL POST EN LA DB CON LOS BARCOS POSICIONADOS
function postShips(){

    param= new URLSearchParams(window.location.search)
    currentGP= param.get('gp')
    url= "/api/games/players/"+currentGP+"/ships"
    data= setShipsPositions()
    
    fetch(url, {method: "POST", body: JSON.stringify(data), headers: {'Content-Type': 'application/json'}})
        .then(function(response){
            if(response.ok){
                return response.json()
            }else{
                return Promise.reject(response.json())
            }
        }).then(function(json){
            redrawShips()
            getShipsPositions()
            document.getElementById('place_ships').style.display= 'none'
            refreshGameData()
            //esperaAlRival()
            gameContinue()
        }).catch(function(error){
            console.log(error.message)
        }).then(function(json){
            errorMsg= json['error'];
            document.getElementById('error_modal_body').innerHTML= `<h4>${errorMsg}</h4>`
            $('#error_modal_body').modal('show')
        })
}

//QUITA LOS BARCOS DE LA GRILLA PARA VOLVER A DIBUJARLOS ESTATICOS
function redrawShips(){
    document.getElementById('bergantin').remove()
    //createShips("bergantin",2,getOrientation(getShipsLocations('bergantin')),document.querySelector('.bergantin-busy-cell'),true)
    document.getElementById('goleta').remove()
    //createShips("goleta",3,getOrientation(getShipsLocations('goleta')),document.querySelector('.goleta-busy-cell'),true)
    document.getElementById('carabela').remove()
    //createShips("carabela",3,getOrientation(getShipsLocations('carabela')),document.querySelector('.carabela-busy-cell'),true)
    document.getElementById('fragata').remove()
    //createShips("fragata",4,getOrientation(getShipsLocations('fragata')),document.querySelector('.fragata-busy-cell'),true)
    document.getElementById('galeon').remove()
    //createShips("galeon",5,getOrientation(getShipsLocations('galeon')),document.querySelector('.galeon-busy-cell'),true)
}

//MUESTRA LA GRILLA DE SALVO
function showSalvoGrid(){
    if(jsonGamePlayer["state"] == "EnterSalvo")
    document.getElementById('grid_salvo').style.display= 'inline-block'
}

//REALIZA EL FETCH DE SALVO
function sendSalvo(){
    param= new URLSearchParams(window.location.search)
    currentGP= param.get('gp')
    url= "/api/games/players/"+currentGP+"/salvos"
    data= {locations: getSalvosAimedCells()}
   
    limpiarDisplay()

    fetch(url, {method: "POST", body: JSON.stringify(data), headers: {'Content-Type': 'application/json'}})
        .then(function(response){
            if(response.ok){
                return response.json()
            }else{
                return Promise.reject(response.json())
            }
        }).then(function(json){
            disableAimed()
            document.getElementById('available_salvo').remove()
            afterShot()
            document.getElementById('shoot').disabled= true;
        }).catch(function(json){
            return json;
        }).then(function(json){
            errorMsg= json['Error'];
            console.log(errorMsg)
            document.getElementById('error_modal_body').innerHTML= `<h4>${errorMsg}</h4>`
            $('#error_modal_body').modal('show')
        })
}

//LE QUITA LA CLASE AIMED A LAS CELDAS DONDE SE DISPARO
function disableAimed(){
    let apuntados= document.querySelectorAll('.aimedSalvo')

    apuntados.forEach(apuntado => apuntado.classList.remove('aimedSalvo'))
}

//MUESTRA EL BOTON PARA ENVIAR LOS SALVOS
function showShootButton(){
    if(jsonGamePlayer["state"] == "EnterSalvo"){
        document.getElementById('shoot').style.display= 'block'
        if(!document.querySelector('.remainingSalvo')){
            document.getElementById('shoot').disabled= false
        }else{
            document.getElementById('shoot').disabled= true
        }
    }else{
        waiting()
    }
}

//DICE CUANTOS BARCOS DEL JUGADOR QUEDAN SIN HUNDIR
function howManyShipsAfloatDoIHave(json){
    let sunkShips= json.sunks[0].mine.length

    return document.querySelectorAll('.grid-item').length - sunkShips
}

function waiting(){
    document.querySelector('#display p').innerText= 'Wait a Moment sea dog...'
}

var timerRefresh

function esperaAlRival(){
    timerRefresh= setTimeout(function() { 
                                refreshGameData()
                                }, 4000);
}

function gameContinue(){
    let state= jsonGamePlayer["state"]

    switch(state){
        case "WaitingForOpponent":
            esperaAlRival()
            waiting()
            break;
        case "GAMEOVER":
            endGame()
            break;
        case "EnterSalvo":
            showSalvoGrid()
            showAvailableSalvo(jsonGamePlayer)
            showShootButton()
            setSalvoes(jsonGamePlayer)
            break;
        case "PlaceShips":
            document.getElementById('place_ships').style.display= 'block'
            break;
        case "NoRival":
            esperaAlRival()
            waiting()
            break;
    }
}

function endGame(){
    console.log("No, no tiene nada que ver con los avengers, es que temrina el juego")
    document.getElementById('final').style.display= 'block'
    document.getElementById('homebutton1').style.display= 'none'
    switch(jsonGamePlayer.score){
        case "WIN":
            document.getElementById('finalMsg').innerText= "Great Battle Captain, Time for some LOOTING!"
            document.getElementById('lootimg').style.display= 'block'
            break;
        case "LOSS":
            document.getElementById('finalMsg').innerText= "Aarghh Mate! Walk the PLANK!"
            document.getElementById('plankimg').style.display= 'block'
            break;
        case "TIE":
            document.getElementById('finalMsg').innerText= "YO HO! Next time won't be so lucky"
            document.getElementById('tie').style.display= 'block'
            break;
    }
}
