package com.codeoftheweb.salvo;

import com.codeoftheweb.salvo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@SpringBootApplication
public class SalvoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}


	@Bean
	public CommandLineRunner initData(PlayerRepository playerRepository, GameRepository gameRepository,
                                      GamePlayerRepository gamePlayerRepository, SalvoRepository salvoRepository,
                                      ScoreRepository scoreRepository) {
		return (args) -> {
			Player barbanegra= playerRepository.save(new Player("Barbanegra"));
			Player bauer= playerRepository.save(new Player("Bauer"));
			Player obrian= playerRepository.save(new Player("O'Brian"));
			Player bauer2= playerRepository.save(new Player("Bauer2"));
			Player palmer= playerRepository.save(new Player("Palmer"));
			playerRepository.save(new Player("Dessler"));
			barbanegra.setPassword(passwordEncoder().encode("rum"));
			bauer.setPassword(passwordEncoder().encode("24"));
			obrian.setPassword(passwordEncoder().encode("42"));
			bauer2.setPassword(passwordEncoder().encode("kb"));
			palmer.setPassword(passwordEncoder().encode("mole"));
			playerRepository.save(barbanegra);
			playerRepository.save(bauer);
            playerRepository.save(obrian);
            playerRepository.save(bauer2);
            playerRepository.save(palmer);
			Game game1= gameRepository.save(new Game());
			Game game2= gameRepository.save(new Game(LocalDateTime.now().plusHours(1)));
			Game game3= gameRepository.save(new Game(LocalDateTime.now().plusHours(2)));
			Ship ship1= new Ship(ShipTypes.BERGANTIN);
			Ship ship2= new Ship(ShipTypes.CARABELA);
			Ship ship3= new Ship(ShipTypes.GALEON);
			Ship ship4= new Ship(ShipTypes.FRAGATA);
			Salvo salvo1= new Salvo();
			Salvo salvo2= new Salvo();
			String[] sLocation1= new String[]{"H5", "A2", "J8"};
			String[] sLocation2= new String[]{"A1","A5","F7"};
			salvo1.addLocation(sLocation1);
			salvo2.addLocation(sLocation2);
			GamePlayer gamePlayer1= gamePlayerRepository.save(new GamePlayer(LocalDateTime.now(),game1, bauer,ship1));
            GamePlayer gamePlayer2= gamePlayerRepository.save(new GamePlayer(LocalDateTime.now().plusHours(3), game1, obrian, ship2));
            GamePlayer gamePlayer3= gamePlayerRepository.save(new GamePlayer(LocalDateTime.now(), game2, bauer2,ship3));
            GamePlayer gamePlayer4= gamePlayerRepository.save(new GamePlayer(LocalDateTime.now(), game3, obrian,ship4));
            String[] locations1= {"A1","A2","A3"};
            String[] locations2= {"F6","F7"};
            String[] locations3= {"A1","B1","C3","C4"};
            String[] locations4= {"A1","A2","A3","A4","A5"};
           	ship3.addLocation(locations4);
           	ship1.addLocation(locations2);
           	ship2.addLocation(locations1);
            gamePlayer1.addShip(ship1);
           	gamePlayer1.addShip(ship3);
           	gamePlayer1.addSalvo(salvo1);
           	gamePlayer2.addSalvo(salvo2);
           	gamePlayerRepository.save(gamePlayer1);
           	gamePlayerRepository.save(gamePlayer2);
           	Score score1= new Score(bauer,game1,LocalDateTime.now(), Score.scoreType.WIN);
           	scoreRepository.save(score1);
		};
	}

}

@EnableWebSecurity
@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	PlayerRepository playerRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName -> {
			Player player = playerRepository.findByUserName(inputName);
			if (player != null) {
				return new User(player.getUserName(), player.getPassword(),
						AuthorityUtils.createAuthorityList("USER"));
			} else {
				throw new UsernameNotFoundException("Unknown user: " + inputName);
			}
		}).passwordEncoder(passwordEncoder);
	}
}

@EnableWebSecurity
@Configuration
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
    	http.headers().frameOptions().disable();

	    http.authorizeRequests()
                .antMatchers("/api/game_view").hasAuthority("USER")
				.antMatchers("/rest/**").hasAuthority("ADMIN")
				.antMatchers("/web/game.html").hasAuthority("USER");

		http.csrf().disable();

		http.formLogin()
				.usernameParameter("username")
				.passwordParameter("password")
				.loginPage("/api/login");

		http.logout().logoutUrl("/api/logout");

		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));

		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());



	}

	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}
}

