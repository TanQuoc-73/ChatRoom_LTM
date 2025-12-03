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

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository appUserRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final UserCoverPhotoRepository userCoverPhotoRepository;
    private final MediaRepository mediaRepository;

    public UserProfileDTO getUserProfile(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy người dùng"));

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

    public UserProfileDTO updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy người dùng"));

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

    public UserProfileDTO updateAvatar(Long userId, Long mediaId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy người dùng"));
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy Media"));

        userAvatarRepository.setAllAvatarsNotCurrent(userId);

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
                .orElseThrow(() -> new IllegalArgumentException("Không thấy người dùng"));
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Không thấy Media"));

        userCoverPhotoRepository.setAllCoverPhotosNotCurrent(userId);

        UserCoverPhoto newCover = new UserCoverPhoto();
        newCover.setUser(user);        
        newCover.setMedia(media);
        newCover.setCurrent(true);
        newCover.setSetAsCoverAt(Instant.now());
        userCoverPhotoRepository.save(newCover);

        return getUserProfile(userId);
    }

    public Optional<UserProfileDTO> findUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> getUserProfile(user.getId()));
    }

    public List<UserProfileDTO> searchUsers(String keyword, int limit) {
        List<AppUser> users = appUserRepository.searchUsers(keyword, PageRequest.of(0, limit));
        return users.stream()
                .map(this::convertToProfileDTO)
                .collect(Collectors.toList());
    }

    // Thêm method convertToProfileDTO
    private UserProfileDTO convertToProfileDTO(AppUser user) {
        Optional<UserAvatar> currentAvatar = userAvatarRepository.findFirstByUserIdAndCurrentTrue(user.getId());
        Optional<UserCoverPhoto> currentCover = userCoverPhotoRepository.findFirstByUserIdAndCurrentTrue(user.getId());

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
}