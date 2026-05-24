package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.ICalService;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ICalController.class, properties = "app.base-url=http://localhost:8080")
@Import(SecurityConfig.class)
class ICalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private ICalService icalService;

    @Test
    void feedReturnsCalendarFileForValidToken() throws Exception {
        // ARRANGE
        User mockUser = new User("joao", "joao@email.com", "senha123");
        String fakeToken = "abc-123-token";

        when(userRepository.findByIcalToken(fakeToken)).thenReturn(Optional.of(mockUser));
        when(meetingService.calendarFor(mockUser)).thenReturn(List.of());
        when(icalService.render(eq(mockUser), any())).thenReturn("BEGIN:VCALENDAR\nEND:VCALENDAR");

        // ACT & ASSERT: Fazemos o GET público
        mockMvc.perform(get("/ical/" + fakeToken + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar; charset=UTF-8")) // Verifica se é ficheiro de calendário
                .andExpect(header().string("Content-Disposition", "inline; filename=\"meetings.ics\"")) // Verifica o cabeçalho de download
                .andExpect(content().string("BEGIN:VCALENDAR\nEND:VCALENDAR")); // Verifica o conteúdo bruto
    }

    @Test
    void feedReturns404NotFoundForInvalidToken() throws Exception {
        // ARRANGE: A base de dados não encontra nenhum utilizador com este token falso
        when(userRepository.findByIcalToken("token-inventado")).thenReturn(Optional.empty());

        // ACT & ASSERT: Como o Controller atira uma ResponseStatusException(HttpStatus.NOT_FOUND), esperamos o status 404
        mockMvc.perform(get("/ical/token-inventado.ics"))
                .andExpect(status().isNotFound()); // HTTP 404
    }
}