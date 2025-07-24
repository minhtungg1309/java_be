package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.request.UserCreationRequest;
import com.minhtung.java_be.dto.request.UserUpdateRequest;
import com.minhtung.java_be.dto.response.UserResponse;
import com.minhtung.java_be.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toUser(UserCreationRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.username( request.getUsername() );
        user.password( request.getPassword() );
        user.firstName( request.getFirstName() );
        user.dob( request.getDob() );
        user.lastName( request.getLastName() );

        return user.build();
    }

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.username( user.getUsername() );
        userResponse.firstName( user.getFirstName() );
        userResponse.lastName( user.getLastName() );
        userResponse.dob( user.getDob() );

        return userResponse.build();
    }

    @Override
    public void updateUser(User user, UserUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        user.setPassword( request.getPassword() );
        user.setFirstName( request.getFirstName() );
        user.setDob( request.getDob() );
        user.setLastName( request.getLastName() );
    }
}
