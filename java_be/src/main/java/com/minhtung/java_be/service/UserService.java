package com.minhtung.java_be.service;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.minhtung.java_be.constant.PredefinedRole;
import com.minhtung.java_be.dto.request.SearchUserRequest;
import com.minhtung.java_be.dto.request.UserCreationRequest;
import com.minhtung.java_be.dto.request.UserUpdateRequest;
import com.minhtung.java_be.dto.response.UserResponse;
import com.minhtung.java_be.entity.User;
import com.minhtung.java_be.exception.AppException;
import com.minhtung.java_be.exception.ErrorCode;
import com.minhtung.java_be.mapper.UserMapper;
import com.minhtung.java_be.repository.RoleRepository;
import com.minhtung.java_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.minhtung.java_be.entity.Role;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);

        try {
          user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

   public UserResponse getMyInfo() {
       var context = SecurityContextHolder.getContext();
       String userId = context.getAuthentication().getName();

       User user = userRepository.findById(userId)
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
       return userMapper.toUserResponse(user);
   }

//    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public List<UserResponse> search(SearchUserRequest request) {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<User> user = userRepository.findAllByUsernameLike(request.getKeyword());
        return user.stream()
                .filter(userName -> !userId.equals(userName.getId()))
                .map(userMapper::toUserResponse)
                .toList();
    }
}
