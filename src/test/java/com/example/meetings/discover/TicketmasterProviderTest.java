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
        "app.discover.ticketmaster.api-key=fake-api-key",
        "app.discover.ticketmaster.country-code=PT",
        "app.discover.ticketmaster.base-url=http://localhost:8089"
})
class TicketmasterProviderTest {

    @Autowired
    private TicketmasterProvider ticketmasterProvider;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void searchReturnsDiscoveredEventsWhenApiIsSuccessful() {
        // ARRANGE: O JSON aninhado típico do Ticketmaster
        String jsonResponse = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "tm-123",
                    "name": "Concerto Taylor Swift",
                    "url": "https://ticketmaster.pt/taylor",
                    "info": "The Eras Tour",
                    "dates": {
                      "start": {
                        "dateTime": "2026-05-24T19:00:00Z"
                      }
                    },
                    "_embedded": {
                      "venues": [
                        { "name": "Estádio da Luz" }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        stubFor(get(urlPathEqualTo("/events.json"))
                .withQueryParam("keyword", equalTo("taylor"))
                .withQueryParam("apikey", equalTo("fake-api-key"))
                .withQueryParam("countryCode", equalTo("PT"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // ACT
        List<DiscoveredEvent> results = ticketmasterProvider.search("taylor");

        // ASSERT
        assertEquals(1, results.size());
        DiscoveredEvent event = results.get(0);

        assertEquals("Ticketmaster", event.source());
        assertEquals("tm-123", event.externalId());
        assertEquals("Concerto Taylor Swift", event.title());
        assertEquals("Estádio da Luz", event.venue());
        assertNotNull(event.start());
    }

    @Test
    void searchReturnsEmptyListWhenApiFails() {
        // Simula um erro 500 do servidor do Ticketmaster
        stubFor(get(urlPathMatching("/events.json.*"))
                .willReturn(aResponse().withStatus(500)));

        List<DiscoveredEvent> results = ticketmasterProvider.search("taylor");

        // O código não deve rebentar (crash), mas sim devolver uma lista vazia
        assertTrue(results.isEmpty());
    }


    @Test
    void searchReturnsEmptyListWhenProviderIsNotConfigured() {
        // ARRANGE: Criamos uma instância manual com a API Key vazia.
        // Como o Spring injeta as propriedades no arranque, a melhor forma de testar
        // a ausência da chave num teste específico é instanciar a classe diretamente.
        TicketmasterProvider unconfiguredProvider = new TicketmasterProvider(
                "", // API Key vazia!
                "PT",
                "http://localhost:8089"
        );

        // ACT
        List<DiscoveredEvent> results = unconfiguredProvider.search("taylor");

        // ASSERT: O método isConfigured() deve bloquear a execução e devolver lista vazia
        assertTrue(results.isEmpty());
    }

    @Test
    void searchIgnoresEventsWithoutStartDate() {
        // ARRANGE: Um evento "TBA" (To Be Announced), onde o Ticketmaster não envia
        // o bloco 'dates' ou a 'dateTime' está ausente.
        String jsonResponseWithoutDate = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "tm-999",
                    "name": "Concerto Secreto",
                    "url": "https://ticketmaster.pt/secreto"
                  }
                ]
              }
            }
            """;

        stubFor(get(urlPathEqualTo("/events.json"))
                .withQueryParam("keyword", equalTo("secreto"))
                .withQueryParam("apikey", equalTo("fake-api-key"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponseWithoutDate)));

        // ACT
        List<DiscoveredEvent> results = ticketmasterProvider.search("secreto");

        // ASSERT: Como o start é null, o código faz "continue" e a lista volta vazia.
        assertTrue(results.isEmpty());
    }
}