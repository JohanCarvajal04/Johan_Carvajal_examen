package ec.edu.uteq.appweb.biblioteca.web.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

import ec.edu.uteq.appweb.biblioteca.domain.Libro;
import ec.edu.uteq.appweb.biblioteca.integration.OpenLibraryClient;
import ec.edu.uteq.appweb.biblioteca.integration.OpenLibraryResponse;
import ec.edu.uteq.appweb.biblioteca.service.LibroService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroEnriquecidoResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.mapper.LibroMapper;

/**
 * API REST del catalogo de libros. Replica el patron de AutorController:
 * exito en ApiResponse, error en ProblemDetail (via GlobalExceptionHandler).
 */
@RestController
@RequestMapping("/api/v1/libros")
public class LibroController {

    private final LibroService servicio;
    private final LibroMapper mapper;
    private final OpenLibraryClient open;

    public LibroController(LibroService servicio, LibroMapper mapper, OpenLibraryClient open) {
        this.servicio = servicio;
        this.mapper = mapper;
        this.open = open;
    }

    // Autor/Editorial/Categoria son LAZY y open-in-view esta deshabilitado
    // (application.yml): sin esta transaccion, mapper.aRespuesta() dispara
    // LazyInitializationException al acceder a esas asociaciones fuera de sesion.
    @Transactional(readOnly = true)
    @GetMapping
    public ApiResponse<List<LibroResponse>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer anioDesde,
            @PageableDefault(size = 20) Pageable paginacion) {
        Page<Libro> pagina = servicio.buscar(titulo, categoriaId, anioDesde, paginacion);
        List<LibroResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Libros listados", PageMeta.de(pagina));
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> buscar(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Libro encontrado");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody LibroRequest solicitud) {
        Libro creado = servicio.crear(solicitud);
        LibroResponse cuerpo = mapper.aRespuesta(creado);
        return ResponseEntity
                .created(URI.create("/api/v1/libros/" + creado.getId()))
                .body(ApiResponse.ok(cuerpo, "Libro creado"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LibroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest solicitud) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.actualizar(id, solicitud)), "Libro actualizado");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}/enriquecido")
    public ApiResponse<LibroEnriquecidoResponse> enriquecido(@PathVariable Long id) {
        Libro libro = servicio.buscarPorId(id);
        OpenLibraryResponse externo = open.consultarPorIsbn(libro.getIsbn());
        LibroEnriquecidoResponse cuerpo = new LibroEnriquecidoResponse(
                mapper.aRespuesta(libro),
                externo != null ? externo.title() : null,
                externo != null ? externo.urlPortada() : null,
                externo != null ? externo.number_of_pages() : null,
                externo != null ? externo.publish_date() : null);
        return ApiResponse.ok(cuerpo, "Libro enriquecido");
    }
}
