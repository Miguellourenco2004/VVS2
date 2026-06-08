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

        // user
        mockOrganizer = new User(
                "Miguel",
                "miguelou04@email.com",
                "benfica"
        );


        when(userService.requireByUsername("Niguel"))
                .thenReturn(mockOrganizer);
    }

    /**
     * Testa a criação de uma reunião com dados válidos.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void proposeMeetingSuccess() throws Exception {

        // POST para criar reunião
        mockMvc.perform(post("/meetings/new")
                        .param("title", "Reunião para lol")
                        .param("description", "novas tecnoligoas portuguesas ")
                        .param("start", "2026-06-05T16:00")
                        .param("end", "2026-06-05T17:30")
                        .param("invitees", "Pai, mae")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));    //  redireciona para o calendário
    }

    /**
     * Testa oa data final é inválida é anterior a inicial
     */
    @Test
    @WithMockUser(username = "Miguel")
    void proposeMeetingInvalidDates() throws Exception {

        // data inválida
        when(meetingService.propose(
                any(),
                anyString(),
                anyString(),
                any(),
                any(),
                anyList()
        )).thenThrow(new IllegalArgumentException(
                "o tempo final nao pode ser anterior "
        ));

        //  POST com a data inválida
        mockMvc.perform(post("/meetings/new")
                        .param("title", "CSGO ?")
                        .param("description", "cs com os cria")
                        .param("start", "2026-06-15T16:00")
                        .param("end", "2026-06-15T15:00")
                        .param("invitees", "roberto")
                        .with(csrf()))
                .andExpect(status().isOk())  // 200
                .andExpect(view().name("propose"))
                // verifica se a mensagem de erro existe
                .andExpect(model().attribute("error", "o tempo final nao pode ser anterior "))

                .andExpect(model().attribute("title", "CSGO ?"));
    }

    /**
     * Testa convidar um convidado que não existente
     */
    @Test
    @WithMockUser(username = "Miguel")
    void proposeMeetingInvalidInvitee() throws Exception {

        // convidado desconhecido
        when(meetingService.propose(
                any(),
                anyString(),
                any(),
                any(),
                any(),
                anyList()
        )).thenThrow(new IllegalArgumentException(
                "Convidado nao existe"
        ));

        //  POST com convidado inválido
        mockMvc.perform(post("/meetings/new")
                        .param("title", "Projeto VVS")
                        .param("start", "2026-06-12T12:00")
                        .param("end", "2026-06-12T14:00")
                        .param("invitees", "pino")
                        .with(csrf()))
                .andExpect(status().isOk())  // 200
                .andExpect(view().name("propose"))

                // Verifica se a mensagem de erro esta presente
                .andExpect(model().attribute("error", "Convidado nao existe"));
    }

    /**
     * Testa o carregamento da página de proposta.
     */
    @Test
    @WithMockUser(username = "Miguel")
    void proposeFormLoads() throws Exception {

        // GET para o formulário
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk()) // 200
                .andExpect(view().name("propose"));
    }

    /**
     * Testa a aceitar contive de reuniao
     */
    @Test
    @WithMockUser(username = "Miguel")
    void acceptInviteSuccess() throws Exception {

        //  user
        User mockUser = new User(
                "Miguel",
                "Miguelou04.com",
                "benfica123"
        );


        when(userService.requireByUsername("Miguel"))
                .thenReturn(mockUser);

        //  POST para aceitar convite
        mockMvc.perform(post("/meetings/99/respond")
                        .param("action", "accept")
                        .with(csrf()))
                // Verifica se redireciona para o calendário
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
}