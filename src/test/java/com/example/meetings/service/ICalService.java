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
     * Testa múltiplos Branches  simultaneamente :
     *  Cobre a linha onde o 'if' da descrição é true.
     *  Cobre a linha onde isConfirmed() é true (STATUS:CONFIRMED).
     *  Cobre a conversão do estado InviteStatus.ACCEPTED.
     */
    @Test
    void render_reuniaocroadoacomcalendario() {
        // Preprar os dados
        User user = new User("Miguel", "Miguelou04@mail.com", "benfica");
        Meeting reuniao = new Meeting("Projeto VVS", "Reuniao proff", Instant.now(), Instant.now().plusSeconds(3600), user);
        reuniao.addParticipant(new MeetingParticipant(reuniao, user, InviteStatus.ACCEPTED));

        // executar a pesquisa
        String resultado = iCalService.render(user, List.of(reuniao));


        // Verificar se o resultaco contem : os a descricao e a confimacao de sucesso
        assertTrue(resultado.contains("DESCRIPTION:Reuniao proff"));
        assertTrue(resultado.contains("STATUS:CONFIRMED"));
        assertTrue(resultado.contains("PARTSTAT=ACCEPTED"));
    }

    /**
     * Testa os Branches  alternativos:
     * Cobre a linha onde o 'if' da descrição é false quando reunião sem descrição.
     * Cobre a linha onde isConfirmed() é falso quando ainda nao esta confirmacada.
     * Cobre a conversão do estado InviteStatus.PENDING.
     */
    @Test
    void render_ReuniaoPendenteSemDescricao_GeraTextoVCalendarTentativo() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        User convidado = new User("Pedro", "Pedro@mail.com", "Pedro");

        Meeting reuniao = new Meeting("Lolzinho", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        reuniao.addParticipant(new MeetingParticipant(reuniao, user, InviteStatus.ACCEPTED));
        reuniao.addParticipant(new MeetingParticipant(reuniao, convidado, InviteStatus.PENDING));

        // executar a pesquisa
        String resultado = iCalService.render(user, List.of(reuniao));

        // Verificar se o resultaco contem : descricao vazia , e p statos de ainda nao confirmado
        assertFalse(resultado.contains("DESCRIPTION:")); // Ramo false da descrição
        assertTrue(resultado.contains("STATUS:TENTATIVE")); // Ramo false do isConfirmed
        assertTrue(resultado.contains("PARTSTAT=NEEDS-ACTION")); // Ramo do switch para PENDING
    }


    /**
     * Testa o Branch  de recusar .
     * Cobre a linha do switch onde ocorre a conversão do estado InviteStatus.DECLINED.
     */
    @Test
    void render_ConvidadoRecusou_GeraTextoVCalendarDeclined() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        Meeting reuniao = new Meeting("Reunião de Condomínio", "Pintar paredes", Instant.now(), Instant.now().plusSeconds(3600), user);

        // Adiciona o participante com o convite DECLINED
        reuniao.addParticipant(new MeetingParticipant(reuniao, user, InviteStatus.DECLINED));

        // executar a pesquisa
        String resultado = iCalService.render(user, List.of(reuniao));

        // Verificar se o resultado contém o status de participação como DECLINED
        assertTrue(resultado.contains("PARTSTAT=DECLINED"));
    }
}