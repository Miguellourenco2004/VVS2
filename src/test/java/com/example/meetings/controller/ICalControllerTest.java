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
@WebMvcTest(
        controllers = ICalController.class,
        properties = "app.base-url=http://localhost:8080"
)
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

    /**
     * Testa a geração do ficheiro iCal
     * para um token válido.
     */
    @Test
    void ReturnsCalendarForValidToken() throws Exception {

        // user
        User mockUser = new User(
                "Miguel",
                "miguelou04@email.com",
                "benfica"
        );

        // token
        String fakeToken = "toker";


        // preprar teste
        when(userRepository.findByIcalToken(fakeToken))
                .thenReturn(Optional.of(mockUser));


        when(meetingService.calendarFor(mockUser))
                .thenReturn(List.of());


        when(icalService.render(eq(mockUser), any()))
                .thenReturn("BEGIN:VCALENDAR\nEND:VCALENDAR");



        // GET para o  iCal
        mockMvc.perform(get("/ical/" + fakeToken + ".ics"))

                .andExpect(status().isOk())  // 200
                .andExpect(content().contentTypeCompatibleWith("text/calendar; charset=UTF-8"))   //  content type ==  calendário
                .andExpect(header().string("Content-Disposition", "inline; filename=\"meetings.ics\""))
                //conteúdo do ficheiro
                .andExpect(content().string("BEGIN:VCALENDAR\nEND:VCALENDAR"));

    }

    /**
     * Testa  o token iCal invalido.
     */
    @Test
    void ReturnsNotFoundForInvalidToken() throws Exception {

        // tockeen invalido
        when(userRepository.findByIcalToken("token-inventado"))
                .thenReturn(Optional.empty());

        // GET com token inválido
        mockMvc.perform(get("/ical/token-inventado.ics"))
                .andExpect(status().isNotFound()); // 404
    }
}