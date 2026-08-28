package ec.edu.uteq.appweb.biblioteca.config;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.uteq.appweb.biblioteca.security.JwtAuthenticationFilter;

/**
 * TODO-U4-2: cadena de seguridad final. API stateless: sin csrf, sin sesion,
 * JWT delante del filtro de usuario/clave, 401 sin autenticacion y 403 sin
 * permisos, ambos en Problem Details.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper mapeador = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers("/api/v1/auth/login", "/swagger-ui/**", "/api/swagger-ui/**",
                                "/v3/api-docs/**", "/api/docs", "/api/docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint((peticion, respuesta, ex) ->
                                escribirProblema(respuesta, HttpStatus.UNAUTHORIZED, "No autenticado",
                                        "Se requiere un token valido en la cabecera Authorization"))
                        .accessDeniedHandler((peticion, respuesta, ex) ->
                                escribirProblema(respuesta, HttpStatus.FORBIDDEN, "Acceso denegado",
                                        "No tiene permisos suficientes para ejecutar esta operacion")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirProblema(jakarta.servlet.http.HttpServletResponse respuesta, HttpStatus estado,
                                  String titulo, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://uteq.edu.ec/errores/" + estado.value()));
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.getWriter().write(mapeador.writeValueAsString(problema));
    }
}
