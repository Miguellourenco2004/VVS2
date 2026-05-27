package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ICalServiceTest {

    private final ICalService iCalService = new ICalService();

    /**
     * Testa múltiplos ramos (Branches) simultaneamente para manter o teste curto:
     * - Cobre a linha onde o 'if' da descrição é VERDADEIRO.
     * - Cobre a linha onde isConfirmed() é VERDADEIRO (STATUS:CONFIRMED).
     * - Cobre a conversão do estado InviteStatus.ACCEPTED.
     */
    @Test
    void render_ReuniaoConfirmadaComDescricao_GeraTextoVCalendar() {
        // Preparação
        User dono = new User("dono", "dono@mail.com", "pass");
        Meeting reuniao = new Meeting("Projeto X", "Discussão importante", Instant.now(), Instant.now().plusSeconds(3600), dono);
        reuniao.addParticipant(new MeetingParticipant(reuniao, dono, InviteStatus.ACCEPTED));

        // Ação
        String resultado = iCalService.render(dono, List.of(reuniao));

        // Verificação: Garantimos que as linhas formatadas apareceram (Line Coverage dos blocos if/switch)
        assertTrue(resultado.contains("DESCRIPTION:Discussão importante"));
        assertTrue(resultado.contains("STATUS:CONFIRMED"));
        assertTrue(resultado.contains("PARTSTAT=ACCEPTED"));
    }

    /**
     * Testa os ramos (Branches) alternativos:
     * - Cobre a linha onde o 'if' da descrição é FALSO (reunião sem descrição).
     * - Cobre a linha onde isConfirmed() é FALSO (STATUS:TENTATIVE).
     * - Cobre a conversão do estado InviteStatus.PENDING.
     */
    @Test
    void render_ReuniaoPendenteSemDescricao_GeraTextoVCalendarTentativo() {
        // Preparação
        User dono = new User("dono", "dono@mail.com", "pass");
        User convidado = new User("convidado", "convidado@mail.com", "pass");

        Meeting reuniao = new Meeting("Almoço", "", Instant.now(), Instant.now().plusSeconds(3600), dono);
        reuniao.addParticipant(new MeetingParticipant(reuniao, dono, InviteStatus.ACCEPTED));
        reuniao.addParticipant(new MeetingParticipant(reuniao, convidado, InviteStatus.PENDING));

        // Ação
        String resultado = iCalService.render(dono, List.of(reuniao));

        // Verificação: Verifica os ramos alternativos processados
        assertFalse(resultado.contains("DESCRIPTION:")); // Ramo falso da descrição
        assertTrue(resultado.contains("STATUS:TENTATIVE")); // Ramo falso do isConfirmed
        assertTrue(resultado.contains("PARTSTAT=NEEDS-ACTION")); // Ramo do switch para PENDING
    }
}