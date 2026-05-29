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

    //  Mocks dos serviços
    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;



    /**
     * Testa se a página do calendário é carregada
     * corretamente para um utilizador autenticado.
     */
    @Test
    @WithMockUser(username = "miguel")
    void calendarPageLoadsSuccess() throws Exception {

        // Preparar o utilizador autenticado
        User mockUser = new User("miguel", "miguelou04@email.com", "benfica123");

        // Simula a procura do utilizador autenticado
        when(userService.requireByUsername("miguel")).thenReturn(mockUser);

        when(meetingService.calendarFor(mockUser)).thenReturn(List.of()); // Calendário vazio
        when(meetingService.pendingInvitesFor(mockUser)).thenReturn(List.of()); // Sem convites

        //  GET para o calendário
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk()) //  200
                .andExpect(view().name("calendar"))  // view retornada é correta
                .andExpect(model().attributeExists("user", "meetings", "pendingInvites", "icalHttpUrl")) // atributios exitem
                .andExpect(model().attribute("icalHttpUrl", "http://localhost:8080/ical/" + mockUser.getIcalToken() + ".ics")); // se for criado corretamen
    }


    /**
     * Testa o acesso ao calendário  sem estar autenticado
     */
    @Test
    void calendarPageRedirectsToLogin() throws Exception {

        //  GET sem user autenticado
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection()) // 302
                .andExpect(redirectedUrlPattern("**/login")); // redirect para login
    }
}