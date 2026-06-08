package com.example.meetings.service;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeetingServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MeetingService meetingService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    /**
     * Testa Branch error na validação de datas do  propose.
     * Cobre a linha 'if (!end.isAfter(start))'  exceção se a  data fim < data inicio
     */
    @Test
    void proposeMeetingInvalidDatesFails() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "benfica");
        Instant inicio = Instant.now();
        Instant fim = inicio.minusSeconds(3600); // o fim esta no passado

        // Verificar se o resultaco contem  a execao por o fim < inicio
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.propose(user, "Titulo", "Desc", inicio, fim, List.of());
        });

        // Verifica que o save não é chamdo logo nao foi guardo o propose nao foi guardado
        verify(meetingRepository, never()).save(any());
    }

    /**
     * Testa o Branch de susseful do propose.
     * Cobre o loop 'for', a linha do 'if (normalized.isEmpty() || !seen.add(normalized))' para convidados válidos
     * e garante a Line Coverage da persistência na base de dados.
     */
    @Test
    void proposeMeetingSuccess() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        User convite = new User("pedro", "pedro@mail.com", "Pedro");

        // Devolve o  user pedro quando for procurado
        when(userRepository.findByUsername("pedro")).thenReturn(Optional.of(convite));

        // Devolve a própria reunião guardada
        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // resultado
        Meeting resultado = meetingService.propose(user, "Título", "Desc",
                Instant.now(), Instant.now().plusSeconds(3600), List.of("pedro"));

        // Verifica se o o resultado nao é null
        assertNotNull(resultado);
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    /**
     * Testa o Branch de error de validação de Status no  respond.
     * Cobre as linhas de decisão onde um estado  pending  não pode ser enviado na resposta.
     */
    @Test
    void respondMeetingInvalidStatusFails() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");


        // Verificar se ao tentar responder com pend ,  envia a illigalException
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.respond(1L, user, InviteStatus.PENDING);
        });
    }

    /**
     * Testa o Branch  de sucesseful no  respond.
     * Cobre as linhas onde o convite é encontrado e o Status é atualizado.
     */
    @Test
    void respondMeetingAcceptedSuccess() {

        // Preparar o utilizador user
        User user = mock(User.class);
        when(user.getId()).thenReturn(100L);

        // Preparar o convite da reunião
        MeetingParticipant convite = mock(MeetingParticipant.class);

        // Simula a procura do convite pelo id da reunião e utilizador
        when(participantRepository.findByMeetingIdAndUserId(1L, 100L))
                .thenReturn(Optional.of(convite));

        // Executa a resposta ao convite
        meetingService.respond(1L, user, InviteStatus.ACCEPTED);

        // Verifica se o estado do convite foi atualizado
        verify(convite).setStatus(InviteStatus.ACCEPTED);
    }

    /**
     * Testa o Branch de sucesso do copyFromDiscovered com dados completos.
     * Cobre o caminho onde o evento tem data de fim e ativa todos os ramos 'true' no buildDescription (título, descrição, venue e url).
     */
    @Test
    void copyFromDiscoveredSuccess() {
        // Preparar os dados
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        DiscoveredEvent event = mock(DiscoveredEvent.class);
        Instant inicio = Instant.now();
        Instant fim = inicio.plusSeconds(7200); // Reunião de 2 horas

        when(event.title()).thenReturn("eleicoes no real madrid");
        when(event.start()).thenReturn(inicio);
        when(event.end()).thenReturn(fim);
        when(event.description()).thenReturn("Cristiano");
        when(event.venue()).thenReturn("barnabeu porta 24 ");
        when(event.source()).thenReturn("13 champions");
        when(event.url()).thenReturn("https://vvs.realmadrid.es");

        // Devolve a própria reunião guardada
        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // copiar
        Meeting resultado = meetingService.copyFromDiscovered(user, event);

        // Verificar se o resultado não é null e se o save foi chamado 1 vez
        assertNotNull(resultado);
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    /**
     * Testa os Branches  do copyFromDiscovered e buildDescription com dados mínimos.
     * Cobre o ramo onde 'event.end()' é null  e contorna os blocos 'if' devido aos valores vazios ou null.
     */
    @Test
    void copyFromDiscoveredMinimalDataSuccess() {
        // Preparar os dados com campos vazios l para testar os ramos falsos dos if
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        DiscoveredEvent event = mock(DiscoveredEvent.class);
        Instant inicio = Instant.now();

        when(event.title()).thenReturn("Reunião ");
        when(event.start()).thenReturn(inicio);
        when(event.end()).thenReturn(null);
        when(event.description()).thenReturn("");
        when(event.venue()).thenReturn(null);
        when(event.source()).thenReturn("Manual");
        when(event.url()).thenReturn(null);

        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Executar a copia
        Meeting resultado = meetingService.copyFromDiscovered(user, event);

        // Verificar se a reunião foi criada e guardada com sucesso
        assertNotNull(resultado);
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    /**
     * Testa o caso de sucess do calendarForIcalToken.
     * Cobre a linha onde o token é válido, o utilizador é encontrado e as reuniões são listadas.
     */
    @Test
    void calendarForIcalTokenSuccess() {
        // Preparar o teste
        User user = mock(User.class);
        String tokenValido = "token_seguro_123";

        when(userRepository.findByIcalToken(tokenValido)).thenReturn(Optional.of(user));
        when(meetingRepository.findCalendarMeetings(user)).thenReturn(List.of());

        // Executar a pesquisa
        List<Meeting> resultado = meetingService.calendarForIcalToken(tokenValido);

        // Verifica se o resultado não é nulo e se procurou pelo token corretamente
        assertNotNull(resultado);
        verify(userRepository, times(1)).findByIcalToken(tokenValido);
    }

    /**
     * Testa o Branch de erro do calendarForIcalToken.
     * Cobre o orElseThrow lançando IllegalArgumentException se o token não existir.
     */
    @Test
    void calendarForIcalTokenInvalidFails() {
        // Preparar o teste
        String tokenInvalido = "token_inexistente";
        when(userRepository.findByIcalToken(tokenInvalido)).thenReturn(Optional.empty());

        // Verificar se é lançada a exceção devido ao token inválido
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.calendarForIcalToken(tokenInvalido);
        });
    }

    /**
     * Testa o caso de sucess do método calendarFor.
     * cobre a  consulta de reuniões associadas a um utilizador.
     */
    @Test
    void calendarForSuccess() {
        // Preparar os dados
        User user = mock(User.class);
        when(meetingRepository.findCalendarMeetings(user)).thenReturn(List.of());

        // Executar a pesquisa
        List<Meeting> resultado = meetingService.calendarFor(user);

        // Verificar se o repositório foi chamado para procurar o calendário do utilizador
        assertNotNull(resultado);
        verify(meetingRepository, times(1)).findCalendarMeetings(user);
    }

    /**
     * Testa o caso de sucesso do método pendingInvitesFor.
     * cobre a  consulta de convites pendentes de um utilizador.
     */
    @Test
    void pendingInvitesForSuccess() {
        // Preparar os dados
        User user = mock(User.class);
        when(participantRepository.findByUserAndStatus(user, InviteStatus.PENDING)).thenReturn(List.of());

        // Executar a pesquisa
        List<MeetingParticipant> resultado = meetingService.pendingInvitesFor(user);

        // Verificar se o repositório de participantes foi acionado com o filtro PENDING
        assertNotNull(resultado);
        verify(participantRepository, times(1)).findByUserAndStatus(user, InviteStatus.PENDING);
    }

}