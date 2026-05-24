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

    @Test
    @WithMockUser(username = "joao")
    void discoverPageLoadsAndPerformsSearch() throws Exception {
        // ARRANGE: Simulamos que o serviço tem provedores e devolve resultados
        when(discoveryService.providers()).thenReturn(List.of()); // Lista vazia serve para o teste passar

        DiscoveredEvent mockEvent = new DiscoveredEvent("Ticketmaster", "123", "Concerto", null, Instant.now(), null, "http://url", "Estádio");
        when(discoveryService.search("rock")).thenReturn(List.of(mockEvent));

        // ACT & ASSERT: Fazemos um GET com o parâmetro 'q'
        mockMvc.perform(get("/discover").param("q", "rock"))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attributeExists("providers", "anyConfigured", "q", "results"));
    }

    @Test
    @WithMockUser(username = "joao")
    void copyEventRedirectsToCalendar() throws Exception {
        // ARRANGE
        User mockUser = new User("joao", "joao@email.com", "senha");
        when(userService.requireByUsername("joao")).thenReturn(mockUser);

        // ACT & ASSERT: Simulamos o clique no botão "Copy to my calendar"
        mockMvc.perform(post("/discover/copy")
                        .param("source", "SeatGeek")
                        .param("externalId", "sg-123")
                        .param("title", "Jogo de Futebol")
                        .param("start", "2026-05-24T19:00:00Z")
                        .with(csrf())) // OBRIGATÓRIO em POSTs

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    /**
     * TESTE EXTRA 1: Acesso à página de descoberta sem pesquisa (query vazia)
     */
    @Test
    @WithMockUser(username = "joao")
    void discoverPageLoadsWithoutSearchQuery() throws Exception {
        // ARRANGE: Simulamos que há provedores configurados, mas o utilizador não pesquisou nada
        when(discoveryService.providers()).thenReturn(List.of());

        // ACT & ASSERT: Fazemos o GET sem o parâmetro 'q'
        mockMvc.perform(get("/discover"))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attribute("q", "")) // A query no modelo deve ser string vazia
                .andExpect(model().attribute("results", List.of())); // A lista de resultados tem de ser vazia (não chama o search)
    }

    /**
     * TESTE EXTRA 2: Acesso à página quando nenhum provider está configurado (sem API Keys)
     */
    @Test
    @WithMockUser(username = "joao")
    void discoverPageLoadsWhenNoProvidersAreConfigured() throws Exception {
        // ARRANGE: O mock do DiscoveryService devolve uma lista de provedores vazia
        when(discoveryService.providers()).thenReturn(List.of());

        // ACT & ASSERT: Mesmo que o utilizador tente pesquisar "rock", o sistema não deve pesquisar
        mockMvc.perform(get("/discover").param("q", "rock"))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attribute("anyConfigured", false)) // O Thymeleaf vai mostrar o aviso de erro
                .andExpect(model().attribute("results", List.of())); // Não devolve resultados
    }

    /**
     * TESTE EXTRA 3: Submissão de cópia de evento a falhar por falta de parâmetros
     * O Spring MVC valida automaticamente os @RequestParam. Se faltarem, atira HTTP 400.
     */
    @Test
    @WithMockUser(username = "joao")
    void copyEventFailsWithBadRequestWhenRequiredParamsAreMissing() throws Exception {
        // ACT & ASSERT: Tentamos fazer POST, mas 'esquecemo-nos' de enviar o "start" e o "source"
        mockMvc.perform(post("/discover/copy")
                        .param("externalId", "sg-123")
                        .param("title", "Evento Inválido")
                        // Faltam os obrigatórios: source e start!
                        .with(csrf()))

                .andExpect(status().isBadRequest()); // O Spring devolve automaticamente HTTP 400 Bad Request
    }
}