package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.UserResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.application.mapper.UserMapper;
import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;

import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;

    public UserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO create(UserRequestDTO dto) {
        return UserMapper.toResponse(userRepository.save(UserMapper.toDomain(dto)));
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
        User updated = UserMapper.toDomain(findUser(id).getId(), dto);
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
