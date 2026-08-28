package ec.edu.uteq.appweb.biblioteca.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import ec.edu.uteq.appweb.biblioteca.repository.UsuarioRepository;
import ec.edu.uteq.appweb.biblioteca.security.JwtService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginResponse;

/**
 * TODO-U4-2: autenticacion. Un login fallido responde 401 (BadCredentialsException
 * la traduce el GlobalExceptionHandler), nunca 200 con success=false.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest solicitud) {
        Usuario usuario = usuarios.findByUsernameAndActivoTrue(solicitud.username())
                .filter(u -> passwordEncoder.matches(solicitud.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Usuario o contrasena invalidos"));

        String token = jwtService.generar(usuario);
        LoginResponse cuerpo = new LoginResponse(usuario.getUsername(), usuario.getRol().name(),
                "Bearer", jwtService.expiracionEnSegundos());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(ApiResponse.ok(cuerpo, "Autenticacion exitosa"));
    }
}
