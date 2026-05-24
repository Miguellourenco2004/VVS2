package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MeetingController.class)
@Import(SecurityConfig.class)
class MeetingControllerPostTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    private User mockOrganizer;

    @BeforeEach
    void setUp() {
        // Como o controller exige um organizador vindo do principal, preparamos este mock globalmente
        mockOrganizer = new User("joao_teste", "joao@email.com", "hash_senha");
        when(userService.requireByUsername("joao_teste")).thenReturn(mockOrganizer);
    }

    /**
     * TESTE 1: Caminho Feliz - Submissão bem-sucedida de uma proposta de reunião.
     */
    @Test
    @WithMockUser(username = "joao_teste")
    void proposeMeetingSuccessfullyRedirectsToCalendar() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(post("/meetings/new")
                        .param("title", "Reunião de Alinhamento")
                        .param("description", "Discussão do Projeto")
                        .param("start", "2026-06-15T14:00")
                        .param("end", "2026-06-15T15:30")
                        .param("invitees", "alice, bob")
                        .with(csrf())) // OBRIGATÓRIO: Contorna a proteção CSRF do Spring Security

                .andExpect(status().is3xxRedirection()) // Espera HTTP 302
                .andExpect(redirectedUrl("/calendar")); // Redireciona para o calendário
    }

    /**
     * TESTE 2: Captura de Erro - Data de fim anterior à data de início.
     */
    @Test
    @WithMockUser(username = "joao_teste")
    void proposeMeetingReturnsErrorViewWhenDatesAreInvalid() throws Exception {
        // ARRANGE: Forçamos o serviço a lançar a exceção de negócio quando as datas estão erradas
        when(meetingService.propose(any(), anyString(), anyString(), any(), any(), anyList()))
                .thenThrow(new IllegalArgumentException("End time must be after start time"));

        // ACT & ASSERT
        mockMvc.perform(post("/meetings/new")
                        .param("title", "Reunião de Retrospetiva")
                        .param("description", "Erro de digitação nas datas")
                        .param("start", "2026-06-15T16:00")
                        .param("end", "2026-06-15T15:00") // Fim antes do Início!
                        .param("invitees", "alice")
                        .with(csrf()))

                .andExpect(status().isOk()) // Não dá crash 500, devolve a página (200 OK)
                .andExpect(view().name("propose")) // Mantém o utilizador no formulário
                .andExpect(model().attribute("error", "End time must be after start time")) // Mensagem de erro capturada!
                .andExpect(model().attribute("title", "Reunião de Retrospetiva")); // Dados originais mantidos nos inputs
    }

    /**
     * TESTE 3: Captura de Erro - Convidado não registado no sistema.
     */
    @Test
    @WithMockUser(username = "joao_teste")
    void proposeMeetingReturnsErrorViewWhenInviteeIsUnknown() throws Exception {
        // ARRANGE: Forçamos o serviço a lançar o erro de utilizador desconhecido
        when(meetingService.propose(any(), anyString(), any(), any(), any(), anyList()))
                .thenThrow(new IllegalArgumentException("Unknown invitee: carlos_fantasma"));

        // ACT & ASSERT
        mockMvc.perform(post("/meetings/new")
                        .param("title", "Sessão Brainstorm")
                        .param("start", "2026-06-16T10:00")
                        .param("end", "2026-06-16T11:00")
                        .param("invitees", "carlos_fantasma")
                        .with(csrf()))

                .andExpect(status().isOk())
                .andExpect(view().name("propose"))
                .andExpect(model().attribute("error", "Unknown invitee: carlos_fantasma"));
    }


    /**
     * TESTE EXTRA 4: Carregamento inicial da página de propor reunião
     */
    @Test
    @WithMockUser(username = "joao")
    void proposeFormLoadsSuccessfully() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("propose")); // Devolve o HTML "propose.html"
    }

    /**
     * TESTE EXTRA 5: Responder a um convite (Aceitar)
     */
    @Test
    @WithMockUser(username = "joao")
    void respondToInviteSuccessfullyRedirectsToCalendar() throws Exception {
        // ARRANGE: Mock do utilizador
        User mockUser = new User("joao", "joao@email.com", "senha123");
        when(userService.requireByUsername("joao")).thenReturn(mockUser);

        // ACT & ASSERT: Simula o POST para aceitar a reunião com ID 99
        mockMvc.perform(post("/meetings/99/respond")
                        .param("action", "accept")
                        .with(csrf()))

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar")); // Depois de responder, volta ao calendário
    }
}