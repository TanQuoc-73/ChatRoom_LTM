package com.nhom8.chat.service;

import com.nhom8.chat.dto.UserProfileDTO;
import com.nhom8.chat.dto.UpdateProfileRequest;
import com.nhom8.chat.entity.AppUser;
import com.nhom8.chat.entity.UserAvatar;
import com.nhom8.chat.entity.UserCoverPhoto;
import com.nhom8.chat.entity.Media;
import com.nhom8.chat.repository.AppUserRepository;
import com.nhom8.chat.repository.UserAvatarRepository;
import com.nhom8.chat.repository.UserCoverPhotoRepository;
import com.nhom8.chat.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository appUserRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final UserCoverPhotoRepository userCoverPhotoRepository;
    private final MediaRepository mediaRepository;

    // Lấy thông tin profile user
    public UserProfileDTO getUserProfile(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Lấy avatar hiện tại
        Optional<UserAvatar> currentAvatar = userAvatarRepository.findFirstByUserIdAndCurrentTrue(userId);
        Optional<UserCoverPhoto> currentCover = userCoverPhotoRepository.findFirstByUserIdAndCurrentTrue(userId);

        return UserProfileDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .website(user.getWebsite())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .verified(user.getVerified())
                .lastActive(user.getLastActive())
                .avatarUrl(currentAvatar.map(avatar -> avatar.getMedia().getFileUrl()).orElse(null))
                .coverUrl(currentCover.map(cover -> cover.getMedia().getFileUrl()).orElse(null))
                .build();
    }

    // Cập nhật thông tin profile
    public UserProfileDTO updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Cập nhật các field được phép thay đổi
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        user.setUpdatedAt(Instant.now());
        appUserRepository.save(user);

        return getUserProfile(userId);
    }

    // Đổi avatar
public UserProfileDTO updateAvatar(Long userId, Long mediaId) {
    // Lấy user và media entities
    AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new IllegalArgumentException("Media not found"));

    // Set tất cả avatar cũ thành không current
    userAvatarRepository.setAllAvatarsNotCurrent(userId);

    // Tạo avatar mới 
    UserAvatar newAvatar = new UserAvatar();
    newAvatar.setUser(user);       
    newAvatar.setMedia(media);
    newAvatar.setCurrent(true);
    newAvatar.setSetAsAvatarAt(Instant.now());
    userAvatarRepository.save(newAvatar);

    return getUserProfile(userId);
}

public UserProfileDTO updateCoverPhoto(Long userId, Long mediaId) {
    AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new IllegalArgumentException("Media not found"));

    // Set tất cả cover cũ thành không current
    userCoverPhotoRepository.setAllCoverPhotosNotCurrent(userId);

    // Tạo cover mới 
    UserCoverPhoto newCover = new UserCoverPhoto();
    newCover.setUser(user);        
    newCover.setMedia(media);
    newCover.setCurrent(true);
    newCover.setSetAsCoverAt(Instant.now());
    userCoverPhotoRepository.save(newCover);

    return getUserProfile(userId);
}

    // Tìm user theo username
    public Optional<UserProfileDTO> findUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> getUserProfile(user.getId()));
    }
}