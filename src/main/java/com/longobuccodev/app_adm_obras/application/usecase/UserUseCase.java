package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.user.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.user.UserResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.UserMapper;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidUserException;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public UserUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                       RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    public UserResponseDTO create(UserRequestDTO dto) {
        if (dto.password() == null || dto.password().isBlank()) {
            throw InvalidUserException.missingPassword();
        }
        User user = UserMapper.toDomain(dto, passwordHasher);
        requireUniqueEmail(user);
        user.markCreated(clock.instant());
        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserResponseDTO findById(UUID id) {
        return UserMapper.toResponse(findUser(id));
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<UserResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(userRepository.findAll(request), UserMapper::toResponse);
    }

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<UserResponseDTO> findAll(PageRequest request, String query) {
        return PageMapper.toResponse(userRepository.findAll(request, SearchTerm.of(query)), UserMapper::toResponse);
    }

    public UserResponseDTO update(UUID id, UserRequestDTO dto) {
        User current = findUser(id);
        User updated = UserMapper.toDomain(current.getId(), dto, passwordHasher);
        boolean passwordChanged = updated.getPasswordHash() != null;
        if (!passwordChanged) {
            updated.setPasswordHash(current.getPasswordHash());
        }
        requireUniqueEmail(updated);
        updated.touchFrom(current, clock.instant());
        userRepository.update(updated);
        // Troca de senha e desativacao encerram as sessoes existentes
        if (passwordChanged || (current.isActive() && !updated.isActive())) {
            refreshTokenRepository.revokeAllByUser(id, RevokeReason.USER_CHANGED, clock.instant());
        }
        return UserMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findUser(id);
        userRepository.deleteById(id);
    }

    private void requireUniqueEmail(User user) {
        Conflicts.requireUnique(userRepository.findByEmail(user.getEmail()), User::getId, user.getId(),
                () -> ConflictException.duplicate("user", "email", user.getEmail()));
    }

    private User findUser(UUID id) {
        return EntityLookup.require(id, userRepository::findById, ResourceNotFoundException::user);
    }
}
