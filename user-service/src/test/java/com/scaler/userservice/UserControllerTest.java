package com.scaler.userservice;

import com.scaler.userservice.controller.UserController;
import com.scaler.userservice.dto.LoginRequestDto;
import com.scaler.userservice.dto.TokenResponseDto;
import com.scaler.userservice.dto.UserProfileResponseDto;
import com.scaler.userservice.dto.UserRegistrationRequestDto;
import com.scaler.userservice.service.TokenPair;
import com.scaler.userservice.service.UserService;
import com.scaler.userservice.model.enums.UserRole;
import com.scaler.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private UserService userService;

    private UserController controller;

    @BeforeEach
    void setup() {
        controller = new UserController(userService, "");
    }

    @Test
    void registerReturnsProfile() {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("StrongPass123");

        User user = new User();
        user.setId(1L);
        user.setName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setRoles(java.util.Set.of(UserRole.CUSTOMER));

        when(userService.registerLocal(request)).thenReturn(user);

        UserProfileResponseDto response = controller.register(request);

        assertEquals(1L, response.getId());
        assertEquals("jane@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("CUSTOMER"));
        verify(userService).registerLocal(request);
    }

    @Test
    void loginReturnsTokens() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("jane@example.com");
        request.setPassword("StrongPass123");

        TokenPair tokenPair = new TokenPair();
        tokenPair.setAccessToken("access-token");
        tokenPair.setRefreshToken("refresh-token");
        tokenPair.setTokenType("Bearer");
        tokenPair.setExpiresInSeconds(3600);

        when(userService.login(request)).thenReturn(tokenPair);

        TokenResponseDto response = controller.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresInSeconds());
        verify(userService).login(request);
    }
}
