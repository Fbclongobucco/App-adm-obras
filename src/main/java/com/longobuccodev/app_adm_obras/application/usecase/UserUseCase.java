package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.UserMapper;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.exception.InvalidUserException;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;

import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserResponseDTO create(UserRequestDTO dto) {
        if (dto.password() == null || dto.password().isBlank()) {
            throw InvalidUserException.missingPassword();
        }
        if (userRepository.findByEmail(dto.email()) != null) {
            throw InvalidUserException.duplicateEmail(dto.email());
        }
        return UserMapper.toResponse(userRepository.save(UserMapper.toDomain(dto, passwordHasher)));
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

    public UserResponseDTO update(UUID id, UserRequestDTO dto) {
        User current = findUser(id);
        User updated = UserMapper.toDomain(current.getId(), dto, passwordHasher);
        if (updated.getPasswordHash() == null) {
            updated.setPasswordHash(current.getPasswordHash());
        }
        userRepository.update(updated);
        return UserMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findUser(id);
        userRepository.deleteById(id);
    }

    private User findUser(UUID id) {
        return EntityLookup.require(id, userRepository::findById, ResourceNotFoundException::user);
    }
}
