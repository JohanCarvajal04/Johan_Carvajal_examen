package ec.edu.uteq.appweb.biblioteca.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uteq.appweb.biblioteca.BaseIntegracionTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integracion de LibroController, siguiendo el patron de
 * AutorControllerIT.
 */
class LibroControllerIT extends BaseIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/libros responde 200 con envoltorio y metadatos de paginacion")
    void listarLibrosDevuelveEnvoltorio() throws Exception {
        mockMvc.perform(get("/api/v1/libros").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/libros/999999 responde 404 en formato Problem Details")
    void buscarLibroInexistenteDevuelveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/libros/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("POST /api/v1/libros con titulo vacio responde 400 con el arreglo errors poblado")
    void crearLibroConTituloVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "1234567890",
                                  "titulo": "",
                                  "anioPublicacion": 2020,
                                  "ejemplaresTotales": 1,
                                  "autorId": 1,
                                  "editorialId": 1,
                                  "categoriaId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
