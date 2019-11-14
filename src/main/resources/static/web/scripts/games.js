const urlAPI= {
                    games: '/api/games',
                    leaderboards: '/api/leaderboards',
                    login: '/api/login',
                    logout: '/api/logout',
                    register: '/api/players'
}

let isLoggued= false;
let jsonGames= "";
let jsonLeaders= "";
let errorMsg="";
var options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };

//ALTERNA LOS BOTONES DE LOG
function btnsDisplay(loggued){
    switch(loggued){
        case false:
            document.getElementById('logInBot').style.display='block'
            document.getElementById('logOutBot').style.display='none'
            document.querySelector('input[name=username]').disabled= false
            document.querySelector('input[name=password]').disabled= false
            document.getElementById('registerDiv').style.display= 'block'
            break;
        case true:
            document.getElementById('logInBot').style.display='none'
            document.getElementById('logOutBot').style.display='block'
            document.querySelector('input[name=username]').disabled= true
            document.querySelector('input[name=password]').disabled= true
            document.getElementById('registerDiv').style.display= 'none'
            break;
    }
}

//PIDE EL JSON DE LOS JUEGOS
function getGames(){
    fetch(urlAPI.games)
        .then(function(response) {
            return response.json()
        }).then(function(json) {
            jsonGames= json;
            listarJuegos();
            btnsDisplay((isLoggued || (jsonGames.player !== "guest")))
            disposeLoader();
        })
}

//PIDE EL JSON DE LEADERS
function getLeaders(){
    fetch(urlAPI.leaderboards)
            .then(function(response) {
                return response.json()
            }).then(function(json) {
                jsonLeaders= json;
                listarLeaders();
            })
}

//REALIZA LOS FETCHS AL MOMENTO DE CARGAR LA PAGINA
function getFetchs(){
    getGames();
    getLeaders();
    $('#createButton').tooltip('disable');

}

//REALIZA EL LOGIN CON EL CAMPO DE USER Y PASS
function login(){
    let formData= new FormData(document.getElementById('login'));

    let user= document.querySelector('input[name=username]')
    let pass= document.querySelector('input[name=password]')

    fetch(urlAPI.login,
                {method:'POST', body: formData})
        .then(function(response){
            if(response.ok){
                console.log('Login Successful')
                user.value="";
                pass.value="";
                $('#login_modal').modal('hide')
                isLoggued= true;
                btnsDisplay(isLoggued);
                getGames();
                $('#createButton').tooltip('disable');

            }else{
                return Promise.reject(response.json());
            }
        }).catch(function(json){
            return json;
        }).then(function(json){
            errorMsg= json['error'];
            console.log(errorMsg)
            document.getElementById('error_modal_body').innerHTML= `<h4>${errorMsg}: INCORRECT USER OR PASSWORD</h4>`
            $('#error_modal').modal('show')
        })
}

//FUNCION PARA REALIZAR EL LOGOUT
function logout(){

    fetch(urlAPI.logout)
        .then(function(response){
            if(response.ok) {
                console.log('Logout Successful')
                isLoggued= false
                btnsDisplay(isLoggued)
                $('#login_modal').modal('hide')
                getGames();
            }else{
                console.log('Something went wrong')
            }
        }).catch(function(error) {
            console.log(error.message)
        })
}

//CREA LA LISTA DE LOS JUEGOS JUGADOS Y EN JUEGO
function listarJuegos() {
    let lista= document.getElementById('listaGames');
    let str="";

    let reverseArray= jsonGames.games.reverse()

    for (let i=0;i<jsonGames.games.length;i++){

        let fecha= reverseArray[i].creationDate;
        let añoMesDia= fecha.split('T')[0].split('-');
        let horaMinSeg= fecha.split('T')[1].split(':');
        let date= Date(añoMesDia[0],añoMesDia[1]-1,añoMesDia[2],[horaMinSeg[0],horaMinSeg[1],horaMinSeg[2]]);

        str += `<li>Fecha de Creación: ${date}</li><ul>`;
            
            if(reverseArray[i].gamePlayers.length === 2){
                for (gameplayer in reverseArray[i].gamePlayers){
                    str += `<li>Player: ${reverseArray[i].gamePlayers[gameplayer].player.usrName}</li>`;
                    
                    if(jsonGames.player.id === reverseArray[i].gamePlayers[gameplayer].player.id){
                        str += `<li><a href="/web/game.html?gp=${reverseArray[i].gamePlayers[gameplayer].id}"><button id="gp_${reverseArray[i].gamePlayers[gameplayer].id}" class="btn btn-danger" type="button" >Enter!</button></li></a>`
                    }
                }
            }
            if(reverseArray[i].gamePlayers.length === 1){
                for (gameplayer in reverseArray[i].gamePlayers){
                    str += `<li>Player: ${reverseArray[i].gamePlayers[gameplayer].player.usrName}</li>`;
                    
                    if(jsonGames.player.id === reverseArray[i].gamePlayers[gameplayer].player.id){
                        str += `<li><a href="/web/game.html?gp=${reverseArray[i].gamePlayers[gameplayer].id}"><button id="gp_${reverseArray[i].gamePlayers[gameplayer].id}" class="btn btn-danger" type="button" >Enter!</button></a></li>`
                    }else{
                        if(jsonGames.player !== "guest"){
                            str += `<li><button id="g_${reverseArray[i].id}" data-gameid="${reverseArray[i].id}" class="btn btn-danger" type="button" onclick="joinGame(this)" >Join Game!</button></li>`
                        }
                    }
                }
            }    
        str+= `</ul>`;
    }

    lista.innerHTML= str;
}

//CREA LA TABLA DE LEADERS
function listarLeaders(){
    let tabla= document.getElementById('leaders');
    let str="";

    sortLeaders();

    for (let i=0;i<jsonLeaders.length;i++){
            str += `<tr>
                        <td>${jsonLeaders[i].player}</td>
                        <td>${jsonLeaders[i].score}</td>
                        <td>${jsonLeaders[i].totalWins}</td>
                        <td>${jsonLeaders[i].totalTies}</td>
                        <td>${jsonLeaders[i].totalLosses}</td>
                    </tr>`
        }

        tabla.innerHTML= str;
}

//ARREGLA LOS JUGADORES DE MAYOR A MENOR PUNTAJE
function sortLeaders(){
    let leaders= jsonLeaders;

    jsonLeaders.sort((x,y) => y.score-x.score)
}

//ABRE LA PESTAÑA DE REGISTRO
function openRegister(){
    $('#login_modal').modal('hide');
}

//FUNCION PARA REGISTRAR UN NUEVO USUARIO
function registerUser(){
    let formData= new FormData(document.getElementById('regForm'))
        fetch(urlAPI.register,
                {method: 'POST', body: formData})
        .then(function(response){
            if(response.ok){
                $('#register_modal').modal('hide')
                $('#login_modal').modal('show')
            }
        })
}

//SE FIJA QUE EL PASSWORD ESTE CORRECTO
function checkPasswords(){
    let pass= document.querySelector('input[name=regPassword]').value
    let confirmPass= document.querySelector('input[name=regConPassword]').value
    let rtn;

    if(pass !== confirmPass){
        rtn= false
    }else{
        rtn=true
    }

    return rtn
}

//CREA UN JUEGO NUEVO
function createGame(){

    if(jsonGames.player !== "guest"){

        fetch(urlAPI.games, {method: "POST"})
            .then(function(response){
                
                return response.json();
                
            }).then(function(json){
                if(json['GamePlayer_Id']){
                    window.location.href = `/web/game.html?gp=${json['GamePlayer_Id']}`;
                }else{
                    errorMsg= json['error'];
                    document.getElementById('error_modal_body').innerHTML= `<h4>${errorMsg}</h4>`
                    $('#error_modal_body').modal('show')
                }
            }).catch(function(error){
                console.log(error.message)
            })

    }else{

        $('#createButton').tooltip('enable');
        $('#createButton').tooltip('show');

    }
}

//UNE AL USUARIO A UNA PARTIDA YA CREADA, DE UN JUGADOR
function joinGame(gameId){

    let id= gameId.getAttribute('data-gameid')

    fetch(`/api/game/${id}/players`,{method: 'POST'})
        .then(function(response){
            if(response.ok){
                return response.json()
            }else{
                return Promise.reject(response.json())
            }
        }).then(function(json){
            let gpId= json["GamePLayerId"]
            window.location.href= "/web/game.html?gp="+gpId
        }).catch(function(json){
            return json;
        }).then(function(json){
            errorMsg= json['Error'];
            console.log(errorMsg)
            document.getElementById('error_modal_body').innerHTML= `<h4>${errorMsg}</h4>`
            $('#error_modal_body').modal('show')
        })
}

function disposeLoader(){
    document.getElementById('carga').style.display= 'none';
}

function actionModal(modalname, action){
    $(modalname).modal(action)
}


var timerRefresh

function anyNewGames(){
    timerRefresh= setTimeout(function() { 
                                refreshGameList()
                                }, 10000);
}

function refreshGameList(){
    function getGames(){
    fetch(urlAPI.games)
        .then(function(response) {
            return response.json()
        }).then(function(json) {
            jsonGames= json;
            listarJuegos();
        })
}
/*
[{type: bergantin, gamePlayer: 5, locations: ["A1","A2","A3"]},{type: carabela, gamePlayer: 5, locations: ["B1","B2","B3"]},
{type: galeon, gamePlayer: 5, locations: ["C1","C2","C3","C4","C5"]}, {type: fragata, gamePlayer: 5, locations: ["D1","D2","D3","D4"]},
{type: goleta, gamePlayer: 5, locations: ["E1","E2","E3"]}]*/