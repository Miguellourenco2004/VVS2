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

// Injetamos um clientId falso e apontamos o SUT para o nosso WireMock!
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
        // Levanta o servidor falso na porta 8089 antes de cada teste
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        // Desliga o servidor após o teste
        wireMockServer.stop();
    }

    @Test
    void searchReturnsDiscoveredEventsWhenApiIsSuccessful() {
        // ARRANGE: Preparar a resposta falsa (Mock) do WireMock
        String jsonResponse = """
            {
              "events": [
                {
                  "id": 12345,
                  "title": "Concerto de Rock",
                  "datetime_utc": "2024-12-01T20:00:00",
                  "url": "https://seatgeek.com/evento",
                  "venue": {
                    "name": "Estádio da Luz"
                  }
                }
              ]
            }
            """;

        // Dizemos ao WireMock: Quando fizerem um GET com "q=rock", devolve o JSON acima.
        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("q", equalTo("rock"))
                .withQueryParam("client_id", equalTo("fake-client-id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // ACT: Executar o método real do nosso código
        List<DiscoveredEvent> results = seatGeekProvider.search("rock");

        // ASSERT: Verificar se o nosso código processou o JSON corretamente
        assertEquals(1, results.size());

        DiscoveredEvent event = results.get(0);
        assertEquals("SeatGeek", event.source());
        assertEquals("12345", event.externalId());
        assertEquals("Concerto de Rock", event.title());
        assertEquals("Estádio da Luz", event.venue());
        assertNotNull(event.start()); // Garante que a data foi "parseada" corretamente
    }


    @Test
    void searchReturnsEmptyListWhenProviderIsNotConfigured() {
        // ARRANGE: Criamos uma instância manual com o Client ID vazio.
        // Assim testamos a proteção inicial do método search().
        SeatGeekProvider unconfiguredProvider = new SeatGeekProvider(
                "", // Client ID vazio!
                "http://localhost:8089"
        );

        // ACT
        List<DiscoveredEvent> results = unconfiguredProvider.search("lakers");

        // ASSERT: O método isConfigured() bloqueia a chamada e devolve uma lista vazia.
        assertTrue(results.isEmpty());
    }

    @Test
    void searchIgnoresEventsWithoutStartDate() {
        // ARRANGE: Um evento onde a API do SeatGeek não envia o campo 'datetime_utc'
        // ou o envia a null.
        String jsonResponseWithoutDate = """
            {
              "events": [
                {
                  "id": 555,
                  "title": "Jogo de Basquetebol",
                  "url": "https://seatgeek.com/jogo"
                }
              ]
            }
            """;

        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("q", equalTo("basquete"))
                .withQueryParam("client_id", equalTo("fake-client-id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponseWithoutDate)));

        // ACT
        List<DiscoveredEvent> results = seatGeekProvider.search("basquete");

        // ASSERT: Como o datetime_utc não existe, o parseStart devolve null,
        // o código faz "continue" e a lista volta vazia.
        assertTrue(results.isEmpty());
    }

    @Test
    void searchReturnsEmptyListWhenApiFails() {
        // ARRANGE: Simula um erro 500 do servidor do SeatGeek
        stubFor(get(urlPathMatching("/events.*"))
                .willReturn(aResponse().withStatus(500)));

        // ACT
        List<DiscoveredEvent> results = seatGeekProvider.search("qualquercoisa");

        // ASSERT: O catch (Exception ex) no teu código captura a falha e devolve lista vazia,
        // evitando que a aplicação inteira vá abaixo.
        assertTrue(results.isEmpty());
    }
}