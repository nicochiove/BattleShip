//DIBUJA LOS SALVOS
function setSalvoes(obj){
    setSalvoesFiredByMe(obj);
    setSalvoesFiredToMe(obj);
}

//DIBUJA LOS SALVOS DISPARADOS POR EL JUGADOR
function setSalvoesFiredByMe(obj){
    for(salvo in obj.salvoes){
        if(obj.salvoes[salvo].player === playerView(obj)){
            for(i=0; i< obj.salvoes[salvo].locations.length; i++){
                createSalvoFiredByMe(obj.salvoes[salvo].locations[i], obj)
            }
        }
    }
    setSunks(obj)
}

//DIBUJA LOS SALVOS DISPARADOS AL JUGADOR
function setSalvoesFiredToMe(obj){
    for(salvo in obj.salvoes){
            if(obj.salvoes[salvo].player !== playerView(obj)){
                for(i=0; i< obj.salvoes[salvo].locations.length; i++){
                    if(myShipsLocations(obj).includes(obj.salvoes[salvo].locations[i])){
                        hitShip(obj.salvoes[salvo].locations[i])
                    }
                }
            }
        }
}

//AGREGA LAS CLASES PARA LOS SALVOS DISPARADOS POR EL JUGADOR
function createSalvoFiredByMe(cell,obj){
    let celda= document.getElementById(`salvo${cell}`)
    
    if(obj.hits.includes(cell)){
        celda.classList.add('hitSpot')
    }else{
        celda.classList.add('shotSalvo')
    }    
        
    celda.classList.remove('fireable')
}

//DEVUELVE EL USUARIO PARA SABER QUIEN DISPARO
function playerView(obj){
    let id=0;

    for(i=0; i<obj.gamePlayer.length;i++){
        if(obj.gamePlayer[i].id === getParamGP()){
            id= obj.gamePlayer[i].player.id;
        }
    }

    return id;
}

//DEVUELVE LAS UBICACIONES DE LOS BARCOS DEL JUGADOR
function myShipsLocations(obj){
    let locations= [];
    let shipLocations= obj.ships;

    for(let location of shipLocations){
        for(cell in location.locations){
            locations.push(location.locations[cell]);
        }
    }

    return locations;
}

//DIBUJA EL GOLPE A UN BARCO DEL JUGADOR
function hitShip(cell){
    document.getElementById(`ships${cell}`).classList.add('hittedShip')
}

//DEVUELVE LAS CELDAS DONDE SE APUNTO PARA DISPARAR
function getSalvosAimedCells(){
    let celdas= document.querySelectorAll('.aimedSalvo')
    let ubicaciones=[]

    celdas.forEach(celda => ubicaciones.push(`${celda.dataset.y}${celda.dataset.x}`))

    return ubicaciones;
}

//MUESTRA EN ICONOS CUANTOS TIROS SE PUEDEN REALIZAR EN EL TURNO
function showAvailableSalvo(json){
    let available= howManyShipsAfloatDoIHave(json)
    let salvos= document.createElement('DIV')
    salvos.id= 'available_salvo'

    if(available == 0){
        available++
        document.querySelector('#display p').innerText= 'Your last Chance'
    }

    if(jsonGamePlayer["state"] == "EnterSalvo"){
        for(let i=0; i<available;i++){
            let salvo=document.createElement('IMG')
            salvo.classList.add('remainingSalvo')
            salvo.id= 'salvo'+i
            salvo.src= 'assets/icons/salvo.png'
            salvos.append(salvo)
        }
        document.getElementById('dock').append(salvos)
    }
}

//MARCA CUANDO YA HUNDISTE UN BARCO CONTRARIO
function setSunks(json){

    for(sunk in json.sunks[0].rival){
        for(cell in json.sunks[0].rival[sunk].locations){
            document.getElementById(`salvo${json.sunks[0].rival[sunk].locations[cell]}`).classList.remove('hitSpot')
            document.getElementById(`salvo${json.sunks[0].rival[sunk].locations[cell]}`).classList.add('sunk')
        }
    }

}