package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Dizemos ao Spring para testar apenas o CalendarController e injetamos uma propriedade necessária
@WebMvcTest(controllers = CalendarController.class, properties = "app.base-url=http://localhost:8080")
@Import(SecurityConfig.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Criamos Mocks dos serviços porque não queremos usar a base de dados real aqui!
    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "joao_teste") // Simula um utilizador autenticado
    void calendarPageLoadsSuccessfullyForAuthenticatedUser() throws Exception {
        // ARRANGE: Preparamos os nossos Mocks
        User mockUser = new User("joao_teste", "joao@email.com", "senha123");
        when(userService.requireByUsername("joao_teste")).thenReturn(mockUser);
        when(meetingService.calendarFor(mockUser)).thenReturn(List.of()); // Calendário vazio
        when(meetingService.pendingInvitesFor(mockUser)).thenReturn(List.of()); // Sem convites

        // ACT & ASSERT: Fazemos o pedido GET e verificamos a resposta
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk()) // Espera HTTP 200 OK
                .andExpect(view().name("calendar")) // Espera que o ficheiro HTML devolvido seja o "calendar"
                .andExpect(model().attributeExists("user", "meetings", "pendingInvites", "icalHttpUrl")) // Verifica se as variáveis foram enviadas para o Thymeleaf
                .andExpect(model().attribute("icalHttpUrl", "http://localhost:8080/ical/" + mockUser.getIcalToken() + ".ics"));
    }

    @Test
    void calendarPageRedirectsToLoginWhenUnauthenticated() throws Exception {
        // Como não usamos @WithMockUser aqui, o pedido é feito por um visitante anónimo.

        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection()) // Espera um redirecionamento HTTP 302
                .andExpect(redirectedUrlPattern("**/login")); // Espera ser atirado para a página de login
    }
}