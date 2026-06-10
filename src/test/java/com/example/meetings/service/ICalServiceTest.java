package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Testes unitários para o ICalService.
 *
 * Sendo uma classe de utilidade sem dependências externas, o serviço é instanciado diretamente
 * para testar a lógica de formatação de ficheiros iCal (.ics), validando a correta tradução
 * das descrições das reuniões e dos estados dos convites Aceite, Pendente e  Recusado.
 */
class ICalServiceTest {

    private final ICalService iCalService = new ICalService();

    /**
     * Testa múltiplos cenários simultaneamente
     * Cobre o cenário em que a reunião tem descrição.
     * Cobre o cenário onde a reunião está confirmada (STATUS:CONFIRMED).
     * Cobre a troca do estado InviteStatus.ACCEPTED.
     */
    @Test
    void renderCalendarWithMeetingSuccess() {
        // Preprar os dados
        User user = new User("Miguel", "Miguelou04@mail.com", "benfica");
        Meeting reuniao = new Meeting("Projeto VVS", "Reuniao proff", Instant.now(), Instant.now().plusSeconds(3600), user);
        reuniao.addParticipant(new MeetingParticipant(reuniao, user, InviteStatus.ACCEPTED));

        // executar a pesquisa
        String resultado = iCalService.render(user, List.of(reuniao));


        // Verificar se o resultaco contem  os a descricao e a confimacao de sucesso
        assertTrue(resultado.contains("DESCRIPTION:Reuniao proff"));
        assertTrue(resultado.contains("STATUS:CONFIRMED"));
        assertTrue(resultado.contains("PARTSTAT=ACCEPTED"));
    }


    /**
     * Testa os cenários alternativos.
     * Cobre o cenário em que a reunião não tem descrição,'if' da descrição é false
     * Cobre o cenário onde a reunião ainda não está confirmada, isConfirmed() é falso.
     * Cobre a troca do estado InviteStatus.PENDING.
     */
    @Test
    void renderCalendarPendingNoDescription() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        User convidado = new User("Pedro", "Pedro@mail.com", "Pedro");

        Meeting reuniao = new Meeting("Lolzinho", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        reuniao.addParticipant(new MeetingParticipant(reuniao, user, InviteStatus.ACCEPTED));
        reuniao.addParticipant(new MeetingParticipant(reuniao, convidado, InviteStatus.PENDING));

        // executar a pesquisa
        String resultado = iCalService.render(user, List.of(reuniao));

        // Verificar se o resultaco contem descricao vazia , e os statos de ainda nao confirmado
        assertFalse(resultado.contains("DESCRIPTION:")); // false da descrição
        assertTrue(resultado.contains("STATUS:TENTATIVE")); //  false do isConfirmed
        assertTrue(resultado.contains("PARTSTAT=NEEDS-ACTION")); //  switch para PENDING
    }


    /**
     * Testa o cenário de convite recusado.
     * Cobre a linha do switch onde ocorre a troca do estado InviteStatus.DECLINED.
     */
    @Test
    void renderCalendarDeclinedStatus() {
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