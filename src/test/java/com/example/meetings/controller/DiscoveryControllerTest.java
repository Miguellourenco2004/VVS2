package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = DiscoveryController.class)
@Import(SecurityConfig.class)
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscoveryService discoveryService;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    /**
     * Testa se a página de descoberta carrega
     * corretamente com resultados de pesquisa.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void discoverPageLoadsAndPerformsSearch() throws Exception {

        // providers
        when(discoveryService.providers())
                .thenReturn(List.of());

        // evento mock para teste
        DiscoveredEvent mockEvent = new DiscoveredEvent(
                "Ticketmaster",
                "123",
                "Concerto",
                null,
                Instant.now(),
                null,
                "http://url",
                "Estádio"
        );


        when(discoveryService.search("rock"))
                .thenReturn(List.of(mockEvent));

        //  GET com pesquisa pelo rock
        mockMvc.perform(get("/discover").param("q", "rock"))

                .andExpect(status().isOk())     //  200

                .andExpect(view().name("discover"))   // view retornada é correta

                // verificar se os atributos esta corretos
                .andExpect(model().attributeExists(
                        "providers",
                        "anyConfigured",
                        "q",
                        "results"
                ));
    }

    /**
     * Testa a  copiar um evento para o calendario propio
     */
    @Test
    @WithMockUser(username = "Miguel")
    void copyEventToCalendar() throws Exception {

        // user autenticado
        User mockUser = new User(
                "Miguel",
                "Miguelou04@email.com",
                "benfica"
        );


        when(userService.requireByUsername("Miguel"))
                .thenReturn(mockUser);

        //  POST para copiar o evento
        mockMvc.perform(post("/discover/copy")
                        .param("source", "SeatGeek")
                        .param("externalId", "sg-123")
                        .param("title", "Jogo de Futebol")
                        .param("start", "2026-05-24T19:00:00Z")
                        .with(csrf()))

                // foi redirecion para o valendario
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }


    /**
     * Testa o acesso à página sem pesquisa.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void discoverPageLoadsWithoutSearchQuery() throws Exception {

        // providers configurados
        when(discoveryService.providers())
                .thenReturn(List.of());

        // GET sem query
        mockMvc.perform(get("/discover"))

                .andExpect(status().isOk())   //200
                .andExpect(view().name("discover"))
                .andExpect(model().attribute("q", ""))  // query = " "
                .andExpect(model().attribute("results", List.of()));   // lista vazia de resultado uma vez que pesquisou em branco
    }

    /**
     * Testa  providers  nao configurados.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void discoverPageNoProvidersAreConfigured() throws Exception {

        //  providers  vazios = sem eventos
        when(discoveryService.providers())
                .thenReturn(List.of());

        //  GET com  rock
        mockMvc.perform(get("/discover").param("q", "rock"))

                .andExpect(status().isOk())    //  200
                .andExpect(view().name("discover"))  // view = discover
                .andExpect(model().attribute("anyConfigured", false))     // nenum provider  ativo
                .andExpect(model().attribute("results", List.of())); // resultado vazio
    }

    /**
     * Testa falta de  parâmetros obrigatórios.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void copyEventFailsWithBadRequestWhenRequiredParamsAreMissing() throws Exception {

        //  POST sem todos ps parametros obrigatorios
        mockMvc.perform(post("/discover/copy")
                        .param("externalId", "sg-123")
                        .param("title", "Evento Inválido")
                        .with(csrf()))
                .andExpect(status().isBadRequest());       //  400 Bad Request
    }
}