package com.back.global.websocket;

import com.back.domain.studyroom.dto.RoomBroadcastMessage;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 통합 테스트
 * - 실제 WebSocket 연결 테스트
 * - 방 입장/퇴장 시 브로드캐스트 테스트
 * - Redis 세션 관리 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("WebSocket 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private User testUser;
    private String accessToken;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // WebSocket URL 설정
        wsUrl = String.format("ws://localhost:%d/ws", port);

        // STOMP 클라이언트 설정
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // 테스트 사용자 생성
        testUser = createTestUser("testuser", "test@test.com");
        accessToken = jwtTokenProvider.createAccessToken(
                testUser.getId(), 
                testUser.getUsername(), 
                testUser.getRole().name()
        );

        // 테스트 방 생성
        testRoom = roomService.createRoom(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                testUser.getId()
        );
    }

    @AfterEach
    void tearDown() {
        // Redis 세션 정리
        if (sessionManager.isUserConnected(testUser.getId())) {
            try {
                sessionManager.leaveRoom(testUser.getId(), testRoom.getId());
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("WebSocket 연결 및 인증 테스트")
    void testWebSocketConnection() throws Exception {
        // given
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);

        // when - 명시적으로 5개 파라미터 메서드 호출
        StompSession session = stompClient.connectAsync(
                wsUrl,
                (WebSocketHttpHeaders) null, // WebSocketHttpHeaders
                connectHeaders, // StompHeaders
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        messageQueue.add("CONNECTED");
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command, 
                                              StompHeaders headers, byte[] payload, Throwable exception) {
                        messageQueue.add("ERROR: " + exception.getMessage());
                    }
                },
                new Object[0] // varargs
        ).get(5, TimeUnit.SECONDS);

        // then
        String message = messageQueue.poll(3, TimeUnit.SECONDS);
        assertThat(message).isEqualTo("CONNECTED");
        assertThat(session.isConnected()).isTrue();

        // cleanup
        session.disconnect();
    }

    @Test
    @Order(2)
    @DisplayName("방 입장 시 브로드캐스트 메시지 수신 테스트")
    void testRoomJoinBroadcast() throws Exception {
        // given
        BlockingQueue<RoomBroadcastMessage> messageQueue = new LinkedBlockingQueue<>();
        
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);

        // WebSocket 연결
        StompSession session = stompClient.connectAsync(
                wsUrl,
                (WebSocketHttpHeaders) null,
                connectHeaders,
                new StompSessionHandlerAdapter() {},
                new Object[0]
        ).get(5, TimeUnit.SECONDS);

        // 방 업데이트 채널 구독
        session.subscribe("/topic/rooms/" + testRoom.getId() + "/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomBroadcastMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((RoomBroadcastMessage) payload);
            }
        });

        // when - 방에 입장
        RoomMember member = roomService.joinRoom(testRoom.getId(), null, testUser.getId());

        // then - 브로드캐스트 메시지 수신 확인
        RoomBroadcastMessage message = messageQueue.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.getType()).isEqualTo(RoomBroadcastMessage.BroadcastType.MEMBER_JOINED);
        assertThat(message.getRoomId()).isEqualTo(testRoom.getId());

        // 온라인 멤버 목록 업데이트 메시지도 수신
        RoomBroadcastMessage onlineMembersMessage = messageQueue.poll(5, TimeUnit.SECONDS);
        assertThat(onlineMembersMessage).isNotNull();
        assertThat(onlineMembersMessage.getType())
                .isEqualTo(RoomBroadcastMessage.BroadcastType.ONLINE_MEMBERS_UPDATED);

        // cleanup
        roomService.leaveRoom(testRoom.getId(), testUser.getId());
        session.disconnect();
    }

    @Test
    @Order(3)
    @DisplayName("방 퇴장 시 브로드캐스트 메시지 수신 테스트")
    void testRoomLeaveBroadcast() throws Exception {
        // given
        BlockingQueue<RoomBroadcastMessage> messageQueue = new LinkedBlockingQueue<>();
        
        // 먼저 방에 입장
        roomService.joinRoom(testRoom.getId(), null, testUser.getId());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);

        // WebSocket 연결
        StompSession session = stompClient.connectAsync(
                wsUrl,
                (WebSocketHttpHeaders) null,
                connectHeaders,
                new StompSessionHandlerAdapter() {},
                new Object[0]
        ).get(5, TimeUnit.SECONDS);

        // 방 업데이트 채널 구독
        session.subscribe("/topic/rooms/" + testRoom.getId() + "/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomBroadcastMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((RoomBroadcastMessage) payload);
            }
        });

        // when - 방에서 퇴장
        roomService.leaveRoom(testRoom.getId(), testUser.getId());

        // then - 퇴장 브로드캐스트 메시지 수신 확인
        RoomBroadcastMessage message = messageQueue.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.getType()).isEqualTo(RoomBroadcastMessage.BroadcastType.ROOM_UPDATED);
        assertThat(message.getRoomId()).isEqualTo(testRoom.getId());

        // cleanup
        session.disconnect();
    }

    @Test
    @Order(4)
    @DisplayName("Redis 세션 관리 테스트 - 방 입장/퇴장")
    void testRedisSessionManagement() throws Exception {
        // given - 초기 상태 확인
        assertThat(sessionManager.isUserConnected(testUser.getId())).isFalse();
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(0);

        // when - 방에 입장
        roomService.joinRoom(testRoom.getId(), null, testUser.getId());

        // then - Redis 세션 확인
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(1);
        assertThat(sessionManager.getOnlineUsersInRoom(testRoom.getId()))
                .contains(testUser.getId());
        assertThat(sessionManager.getUserCurrentRoomId(testUser.getId()))
                .isEqualTo(testRoom.getId());

        // when - 방에서 퇴장
        roomService.leaveRoom(testRoom.getId(), testUser.getId());

        // then - 세션 정리 확인
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(0);
        assertThat(sessionManager.getUserCurrentRoomId(testUser.getId())).isNull();
    }

    @Test
    @Order(5)
    @DisplayName("여러 사용자 동시 입장 테스트")
    void testMultipleUsersJoin() throws Exception {
        // given - 추가 사용자 생성
        User user2 = createTestUser("testuser2", "test2@test.com");
        User user3 = createTestUser("testuser3", "test3@test.com");

        // when - 3명의 사용자가 방에 입장
        roomService.joinRoom(testRoom.getId(), null, testUser.getId());
        roomService.joinRoom(testRoom.getId(), null, user2.getId());
        roomService.joinRoom(testRoom.getId(), null, user3.getId());

        // then - Redis에서 온라인 사용자 수 확인
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(3);
        assertThat(sessionManager.getOnlineUsersInRoom(testRoom.getId()))
                .containsExactlyInAnyOrder(testUser.getId(), user2.getId(), user3.getId());

        // cleanup
        roomService.leaveRoom(testRoom.getId(), testUser.getId());
        roomService.leaveRoom(testRoom.getId(), user2.getId());
        roomService.leaveRoom(testRoom.getId(), user3.getId());
    }

    @Test
    @Order(6)
    @DisplayName("Heartbeat 타임아웃 테스트")
    void testHeartbeatTimeout() throws Exception {
        // given - 방에 입장
        roomService.joinRoom(testRoom.getId(), null, testUser.getId());
        
        // 초기 상태 확인
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(1);

        // when - Heartbeat 없이 대기 (실제 TTL은 10분이지만 테스트에서는 확인만)
        // 실제 프로덕션에서는 TTL이 지나면 자동으로 세션이 만료됨
        
        // Heartbeat 갱신
        sessionManager.updateLastActivity(testUser.getId());

        // then - 여전히 온라인 상태
        assertThat(sessionManager.isUserConnected(testUser.getId())).isTrue();
        assertThat(sessionManager.getRoomOnlineUserCount(testRoom.getId())).isEqualTo(1);

        // cleanup
        roomService.leaveRoom(testRoom.getId(), testUser.getId());
    }

    // ==================== Helper Methods ====================

    private User createTestUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UserProfile profile = new UserProfile();
        profile.setNickname(username);
        user.setUserProfile(profile);

        return userRepository.save(user);
    }
}
