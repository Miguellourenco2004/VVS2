package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = {
        "app.discover.seatgeek.client-id=fake-client-id",
        "app.discover.seatgeek.base-url=http://localhost:8089"
})
class SeatGeekProviderTest {

    @Autowired
    private SeatGeekProvider seatGeekProvider;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        // Inicializa o servidor WireMock
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {

        wireMockServer.stop();
    }


    /**
     * Testa a pesquisa de eventos
     * quando a API responde corretamente.
     */
    @Test
    void searchReturnsDiscoveredEventsWhenApiIsSuccessful() {
        // Preparar resposta JSON da API
        String jsonResponse = """
            {
              "events": [
                {
                  "id": 67,
                  "title": "Concerto de Jazz",
                  "datetime_utc": "2024-12-01T20:00:00",
                  "url": "https://seatgeek.com/evento",
                  "venue": {
                    "name": "Sala 6"
                  }
                }
              ]
            }
            """;

        // GET com "q=rock", devolve o JSON
        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("q", equalTo("jazz"))
                .withQueryParam("client_id", equalTo("fake-client-id"))  // OQUE È O FAKE ID
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // Executa a pesquisa
        List<DiscoveredEvent> results = seatGeekProvider.search("jazz");

        //  Verifica se  processou o JSON corretamente
        assertEquals(1, results.size());

        DiscoveredEvent event = results.get(0);
        assertEquals("SeatGeek", event.source());
        assertEquals("67", event.externalId());
        assertEquals("Concerto de Jazz", event.title());
        assertEquals("Sala 6", event.venue());
        assertNotNull(event.start());
    }

    /**
     * Testa provider
     * não está configurado.
     */
    @Test
    void searchReturnsEmptyListWhenProviderIsNotConfigured() {
        // Preparar provider sem client id
        // Assim testamos a proteção inicial do método search().
        SeatGeekProvider unconfiguredProvider = new SeatGeekProvider(
                "", // Client ID vazio!
                "http://localhost:8089"
        );

        // pesquisa
        List<DiscoveredEvent> results = unconfiguredProvider.search("lakers");

        //verificar se  isConfigured() bloqueia  e devolve uma lista vazia.
        assertTrue(results.isEmpty());
    }


    /**
     * Testa o o evento
     * não que nao possui data de início.
     */
    @Test
    void searchIgnoresEventsWithoutStartDate() {
        // Preparar  JSON sem datetime
        String jsonResponseWithoutDate = """
            {
              "events": [
                {
                  "id": 752,
                  "title": "Peladinha fut",
                  "url": "https://seatgeek.com/jogo"
                }
              ]
            }
            """;

        //  resposta API para pesquisa
        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("q", equalTo("fut"))
                .withQueryParam("client_id", equalTo("fake-client-id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponseWithoutDate)));

        // pesquisa
        List<DiscoveredEvent> results = seatGeekProvider.search("basquete");

        // datetime_utc não existe, o parseStart devolve null e lista retorna vazia
        assertTrue(results.isEmpty());
    }


    /**
     * Testa  API devolve erro.
     */
    @Test
    void searchReturnsEmptyListWhenApiFails() {
        // Simula erro 500 da API
        stubFor(get(urlPathMatching("/events.*"))
                .willReturn(aResponse().withStatus(500)));

        // pesquisa
        List<DiscoveredEvent> results = seatGeekProvider.search("qualquercoisa");

        //  O catch captura a falha e devolve lista vazia
        assertTrue(results.isEmpty());
    }
}