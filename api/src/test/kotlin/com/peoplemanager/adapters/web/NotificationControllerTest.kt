package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.NotificationNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.NotificationCommandPort
import com.peoplemanager.application.port.input.NotificationQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [NotificationController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class NotificationControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun notificationQueryPort(): NotificationQueryPort = mockk()

        @Bean
        fun notificationCommandPort(): NotificationCommandPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var notificationQueryPort: NotificationQueryPort

    @Autowired
    private lateinit var notificationCommandPort: NotificationCommandPort

    private val userId = UserId(UUID.randomUUID())
    private val notificationId = NotificationId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private fun authenticatedJwt(userId: UserId = this.userId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = userId
        return token
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should list notifications with pagination`() {
        val notification = Notification(
            id = notificationId,
            userId = userId,
            type = NotificationType.ACTION_ITEM_OVERDUE,
            title = "Action item overdue",
            message = "Task X is overdue",
            referenceId = UUID.randomUUID().toString(),
            personId = personId,
            createdAt = Instant.parse("2026-05-10T10:00:00Z")
        )
        val page = PageImpl(listOf(notification), PageRequest.of(0, 20), 1)

        every { notificationQueryPort.getNotifications(any()) } returns page

        mockMvc.perform(
            get("/api/v1/notifications")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(notificationId.value.toString()))
            .andExpect(jsonPath("$.content[0].type").value("ACTION_ITEM_OVERDUE"))
            .andExpect(jsonPath("$.content[0].title").value("Action item overdue"))
            .andExpect(jsonPath("$.content[0].message").value("Task X is overdue"))
            .andExpect(jsonPath("$.content[0].isRead").value(false))
            .andExpect(jsonPath("$.content[0].personId").value(personId.value.toString()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    @Test
    fun `should filter unread only notifications`() {
        every { notificationQueryPort.getNotifications(any()) } returns PageImpl(emptyList())

        mockMvc.perform(
            get("/api/v1/notifications")
                .param("unreadOnly", "true")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        verify {
            notificationQueryPort.getNotifications(match { it.unreadOnly })
        }
    }

    @Test
    fun `should return unread count`() {
        every { notificationQueryPort.getUnreadCount(any()) } returns 5L

        mockMvc.perform(
            get("/api/v1/notifications/unread-count")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(5))
    }

    @Test
    fun `should return zero unread count when no notifications`() {
        every { notificationQueryPort.getUnreadCount(any()) } returns 0L

        mockMvc.perform(
            get("/api/v1/notifications/unread-count")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `should mark notification as read`() {
        val readNotification = Notification(
            id = notificationId,
            userId = userId,
            type = NotificationType.ACTION_ITEM_OVERDUE,
            title = "Action item overdue",
            message = "Task X is overdue",
            readAt = Instant.parse("2026-05-10T12:00:00Z"),
            createdAt = Instant.parse("2026-05-10T10:00:00Z")
        )

        every { notificationCommandPort.markAsRead(any()) } returns readNotification

        mockMvc.perform(
            post("/api/v1/notifications/${notificationId.value}/read")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(notificationId.value.toString()))
            .andExpect(jsonPath("$.isRead").value(true))
    }

    @Test
    fun `should return 404 when marking non-existent notification as read`() {
        val nonExistentId = NotificationId(UUID.randomUUID())
        every { notificationCommandPort.markAsRead(any()) } throws NotificationNotFoundException(nonExistentId)

        mockMvc.perform(
            post("/api/v1/notifications/${nonExistentId.value}/read")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should mark all notifications as read`() {
        every { notificationCommandPort.markAllAsRead(any()) } returns 3

        mockMvc.perform(
            post("/api/v1/notifications/read-all")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.markedCount").value(3))
    }

    @Test
    fun `should return 401 for unread-count when not authenticated`() {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 for mark-as-read when not authenticated`() {
        mockMvc.perform(post("/api/v1/notifications/${UUID.randomUUID()}/read"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 for mark-all-read when not authenticated`() {
        mockMvc.perform(post("/api/v1/notifications/read-all"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should pass pagination parameters correctly`() {
        every { notificationQueryPort.getNotifications(any()) } returns PageImpl(emptyList())

        mockMvc.perform(
            get("/api/v1/notifications")
                .param("page", "2")
                .param("size", "10")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        verify {
            notificationQueryPort.getNotifications(match {
                it.pageable.pageNumber == 2 && it.pageable.pageSize == 10
            })
        }
    }

    @Test
    fun `should list read notifications with readAt timestamp`() {
        val readAt = Instant.parse("2026-05-10T11:00:00Z")
        val notification = Notification(
            id = notificationId,
            userId = userId,
            type = NotificationType.STALE_ONE_ON_ONE,
            title = "1:1 overdue",
            message = "You haven't met with Alice in 14 days",
            personId = personId,
            readAt = readAt,
            createdAt = Instant.parse("2026-05-10T10:00:00Z")
        )
        val page = PageImpl(listOf(notification), PageRequest.of(0, 20), 1)

        every { notificationQueryPort.getNotifications(any()) } returns page

        mockMvc.perform(
            get("/api/v1/notifications")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].isRead").value(true))
            .andExpect(jsonPath("$.content[0].readAt").exists())
    }
}
