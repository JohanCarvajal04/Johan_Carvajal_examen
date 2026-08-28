package ec.edu.uteq.appweb.biblioteca.web.controller;

import java.net.ResponseCache;
import java.net.URI;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.uteq.appweb.biblioteca.domain.Autor;
import ec.edu.uteq.appweb.biblioteca.domain.Libro;
import ec.edu.uteq.appweb.biblioteca.integration.OpenLibraryClient;
import ec.edu.uteq.appweb.biblioteca.service.LibroService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.AutorRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.AutorResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.mapper.LibroMapper;

/**
 * ============================================================================
 * TODO-U4-1 (Objetivo especifico 2 de la Guia): API REST DEL CATALOGO
 * ============================================================================
 *
 * Replique el patron de AutorController, que ya esta implementado y comentado.
 * LibroService y LibroMapper estan completos: usted solo expone, no reimplementa.
 *
 * Endpoints exigidos:
 *   GET    /api/v1/libros                 paginado, con meta; parametros opcionales
 *                                         titulo, categoriaId y anioDesde -> LibroService.buscar
 *   GET    /api/v1/libros/{id}            200 o 404 con ProblemDetail
 *   POST   /api/v1/libros                 201 + Location, rol ADMIN
 *   PUT    /api/v1/libros/{id}            200, rol ADMIN
 *   DELETE /api/v1/libros/{id}            204, rol ADMIN, borrado logico
 *   GET    /api/v1/libros/{id}/enriquecido combina el libro local con Open Library
 *                                         (depende del TODO-U4-4)
 *
 * Recuerde: exito en ApiResponse, error en ProblemDetail, nunca los dos mezclados.
 */
@RestController
@RequestMapping("/api/v1/libros")
public class LibroController {

    // TODO-U4-1: inyectar LibroService, LibroMapper y OpenLibraryClient, e implementar los endpoints.
    private final LibroService servicio;
    private final LibroMapper mapper;
    private final OpenLibraryClient open;

    public LibroController(LibroService libroService, LibroMapper libroMapper){
        this.servicio = libroService;
        this.mapper = libroMapper;
    }
    @GetMapping
    public ApiResponse<List<LibroResponse>> Listar (@PageableDefault(size= 20 ) Pageable paginacion){
        Page<Libro> pagina = servicio.listarActivos(paginacion);
        List<LibroResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Libros listados", PageMeta.de(pagina));
    }

    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> buscar(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Autor encontrado");
    }

    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> buscar(@PathVariable String titulo,@PathVariable Long categoriaId,@PathVariable Integer anioDesde) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscar(titulo,categoriaId,anioDesde)), "Autor encontrado");
    }


    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody LibroRequest solicitud) {
        Libro creado = servicio.crear(solicitud.titulo(),solicitud.categoriaId(),solicitud.anioPublicacion());
        LibroResponse cuerpo = mapper.aRespuesta(creado);
        return ResponseEntity
                .created(URI.create("/api/v1/libros/" + creado.getId()))
                .body(ApiResponse.ok(cuerpo, "Libro creado"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LibroResponse>> desactivar(@Valid @PathVariable Long id){
        Libro desactivar = servicio.desactivar(id);
        LibroResponse cuerpo=mapper.aRespuesta(desactivar);
        return ResponseEntity
                .body(ApiResponse.ok(cuerpo,"Libro desactivado"));
    }

    @GetMapping("/{id}/enriquecido")
    public ApiResponse<List<LibroResponse>> ListaEnriquecida (@PageableDefault(size= 20 ) Pageable paginacion,@PathVariable Sting isbn){
        Page<Libro> pagina = open.consultarPorIsbn(isbn);
        List<LibroResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Libros listados", PageMeta.de(pagina));
    }

}
