package com.example.meetings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.EventProvider;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;

/**
 * End-to-End tests using Selenium WebDriver and a dedicated test database
 * Chrome runs in headless mode
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/e2edb;AUTO_SERVER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.base-url=http://localhost"
})
public class EndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingParticipantRepository participantRepository;

    // 👇 O Spring agora sabe exatamente qual o bean a substituir!
    @MockBean(name = "ticketmasterProvider")
    private EventProvider mockEventProvider;

    private WebDriver driver;
    private WebDriverWait wait;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        // Clear database manually — avoids restarting the Spring context
        // which would change the port and invalidate the browser session
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception ignored) {}
            driver.quit();
        }
    }

    // ================= HELPERS =================

    private void register(String username, String email, String password) {
        driver.get(baseUrl() + "/register");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    private void login(String username, String password) {
        driver.get(baseUrl() + "/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.clear();
        passwordField.clear();

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        WebElement submit = driver.findElement(By.cssSelector("button[type=submit]"));
        wait.until(ExpectedConditions.elementToBeClickable(submit));
        submit.click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/calendar"),
                ExpectedConditions.urlContains("error")
        ));
    }

    private void registerAndLogin(String username, String email, String password) {
        register(username, email, password);
        wait.until(ExpectedConditions.urlContains("/login"));
        login(username, password);
        wait.until(ExpectedConditions.urlContains("/calendar"));
    }

    private void setDateTimeLocal(String id, String value) {
        WebElement el = driver.findElement(By.id(id));
        ((JavascriptExecutor) driver).executeScript(
                // 1. Change type to text to completely disable HTML5 browser validation
                "arguments[0].type = 'text';" +
                        // 2. Set the value
                        "arguments[0].value = arguments[1];",
                el, value
        );
    }

    private void submitProposalForm() {
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelector('form[action*=\"meetings\"]').submit();"
        );
    }

    // ================= TESTES =================

    @Test
    void register_Register_Success() {
        register("nuno", "nuno@gmail.pt", "password123");

        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getPageSource().contains("Account created"));
    }

    @Test
    void register_UsernameAlreadyExists() {
        register("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/register");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("nuno");
        driver.findElement(By.id("email")).sendKeys("other@gmail.pt");
        driver.findElement(By.id("password")).sendKeys("password456");
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("Username already taken"));
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }

    @Test
    void login_RedirectsToCalendar() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        assertTrue(driver.getCurrentUrl().contains("/calendar"));
        assertTrue(driver.getPageSource().contains("Your calendar"));
    }

    @Test
    void login_WrongCredentials() {
        register("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("nuno");
        driver.findElement(By.id("password")).sendKeys("wrongpassword");
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        wait.until(ExpectedConditions.urlContains("error"));
        assertTrue(driver.getPageSource().contains("Invalid username or password"));
    }

    @Test
    void calendar_ShouldRedirectToLogin_WhenUnauthenticated() {
        driver.get(baseUrl() + "/calendar");

        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void calendar_DisplayUsername() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("nuno"));
        assertTrue(driver.getPageSource().contains("Your calendar"));
    }

    @Test
    void proposeMeeting_Success() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("Meeting 1");
        driver.findElement(By.id("description")).sendKeys("Description");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        submitProposalForm();

        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Meeting 1"));
    }

    @Test
    void proposeMeeting_EndBeforeStart() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("Meeting 2");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T08:00");
        submitProposalForm();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("End time must be after start time"));
    }

    @Test
    void pendingInvite_ShouldAppearAndBeAccepted() {
        register("organizer", "organizer@gmail.pt", "password123");
        register("invitee", "invitee@gmail.pt", "password123");

        // Organizer proposes a meeting and invites the invitee
        login("organizer", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Meeting 3");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        driver.findElement(By.id("invitees")).sendKeys("invitee");
        submitProposalForm();
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // Create a completly fresh WebDriver session
        driver.quit();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Invitee logs in and sees the pending invite
        login("invitee", "password123");
        assertTrue(driver.getPageSource().contains("Meeting 3"));
        assertTrue(driver.getPageSource().contains("pending"));

        // Invitee accepts
        WebElement acceptButton = driver.findElement(
                By.xpath("//input[@value='accept']/following-sibling::button | //input[@name='action'][@value='accept']/../button"));
        acceptButton.click();

        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Meeting 3"));
    }

    @Test
    void pendingInvite_Decline() {
        register("organizer2", "organizer2@gmail.pt", "password123");
        register("invitee2", "invitee2@gmail.pt", "password123");

        // Organizer proposes a meeting and invites the invitee
        login("organizer2", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Meeting 4");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        driver.findElement(By.id("invitees")).sendKeys("invitee2");
        submitProposalForm();
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // Invitee declines
        login("invitee2", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Meeting 4"));

        WebElement declineButton = driver.findElement(
                By.xpath("//input[@name='action'][@value='decline']/../button"));
        declineButton.click();

        wait.until(ExpectedConditions.urlContains("/calendar"));
        // After declining, the meeting should not appear in the calendar
        assertFalse(driver.getPageSource().contains("Meeting 4"));
    }

    @Test
    void signOut_RedirectToLogin() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        // 1. Garantir que a página de calendário carregou completamente
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // 2. Submeter o formulário de logout de forma direta com Javascript
        // Isto procura a <form th:action="@{/logout}"> e faz submit diretamente
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelector('form[action*=\"logout\"]').submit();"
        );

        // 3. Confirmar que somos redirecionados
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"), "Deve ser redirecionado para o login após o logout");
    }

    @Test
    void discoverEvent_WithMockedApi_ShouldDisplayAndCopyToCalendar() {
        // 1. CONFIGURAR O MOCK
        Mockito.when(mockEventProvider.isConfigured()).thenReturn(true);

        String testEventTitle = "Conferencia Tech Mockada";

        DiscoveredEvent fakeEvent = new DiscoveredEvent(
                "Ticketmaster",
                "ext-999",
                testEventTitle,
                "Descricao incrivel do evento falso",
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T18:00:00Z"),
                "http://eventofalso.com",
                "Lisboa (Virtual)"
        );
        Mockito.when(mockEventProvider.search("Java")).thenReturn(List.of(fakeEvent));

        // 2. REGISTAR E FAZER LOGIN
        registerAndLogin("discover_user", "discover@gmail.com", "password123");

        // 3. PESQUISAR NA PÁGINA DISCOVER
        driver.get(baseUrl() + "/discover?q=Java");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String discoverPageSource = driver.getPageSource();
        assertTrue(discoverPageSource.contains(testEventTitle), "O evento fictício deve estar visível na pesquisa");

        // 4. COPIAR PARA O CALENDÁRIO (Submissão exata da form correta)
        String specificFormXPath = "//form[.//input[@name='title' and @value='" + testEventTitle + "']]";
        WebElement form = driver.findElement(By.xpath(specificFormXPath));
        ((JavascriptExecutor) driver).executeScript("arguments[0].submit();", form);

        // 5. VALIDAR SE ESTÁ NO CALENDÁRIO
        wait.until(ExpectedConditions.urlContains("/calendar"));

        String calendarPageSource = driver.getPageSource();
        assertTrue(calendarPageSource.contains(testEventTitle),
                "O evento deve aparecer no calendário após ser copiado. HTML recebido:\n" + calendarPageSource);
    }
}