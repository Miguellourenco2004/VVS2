package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerPostTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;



    /**
     * Testa se a página de login é carregada com sucesso
     */
    @Test
    void loginPagesucces() throws Exception {
        // Faz o get na pagina de login
        mockMvc.perform(get("/login"))
                // 200
                .andExpect(status().isOk())
                // view retornada = login
                .andExpect(view().name("login"));
    }

    /**
     * Testa se a página de registo é carregada com sucesso.
     */
    @Test
    void registerPageSuccess() throws Exception {
        // Faz get na pagina de registo
        mockMvc.perform(get("/register"))
                // 200
                .andExpect(status().isOk())
                // view retornda = register
                .andExpect(view().name("register"));
    }

    /**
     * Testa se a rota raiz redireciona para o calendário.
     */
    @Test
    void rootUrlRedirect() throws Exception {
        // faz  GET para a rota padrao
        mockMvc.perform(get("/"))
                // redireciado para calendario
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    /**
     * Testa o registo de um utilizador com dados válidos.
     */
    @Test
    void registerUserSuccessRedirecLogin() throws Exception {

        //  POST de registo com os parametros validos
        mockMvc.perform(post("/register")
                        .param("username", "Miguel")
                        .param("email", "miguelou04@email.com")
                        .param("password", "benfica123")
                        .with(csrf()))
                // redirect para a pagina de login
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }


    /**
     * Testa o username já existe.
     */
    @Test
    void registerUserReturnsError() throws Exception {

        // Simula erro ao tentar registar um username já existente
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        // POST de registo
        mockMvc.perform(post("/register")
                        .param("username", "miguelexiste")
                        .param("email", "miguel04@email.com")
                        .param("password", "benfica123")
                        .with(csrf()))
                // 200
                .andExpect(status().isOk())
                // view registo mas com o erro de ja existir
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already taken"));
    }
}