package me.huynhducphu.talent_bridge.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.huynhducphu.talent_bridge.dto.request.user.SelfUserUpdatePasswordRequestDto;
import me.huynhducphu.talent_bridge.dto.request.user.SelfUserUpdateProfileRequestDto;
import me.huynhducphu.talent_bridge.dto.request.user.UserCreateRequestDto;
import me.huynhducphu.talent_bridge.dto.request.user.UserUpdateRequestDto;
import me.huynhducphu.talent_bridge.dto.response.user.DefaultUserResponseDto;
import me.huynhducphu.talent_bridge.model.Company;
import me.huynhducphu.talent_bridge.model.Resume;
import me.huynhducphu.talent_bridge.model.Role;
import me.huynhducphu.talent_bridge.model.User;
import me.huynhducphu.talent_bridge.repository.CompanyRepository;
import me.huynhducphu.talent_bridge.repository.RoleRepository;
import me.huynhducphu.talent_bridge.repository.UserRepository;
import me.huynhducphu.talent_bridge.service.S3Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Admin 6/7/2025
 **/
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements me.huynhducphu.talent_bridge.service.UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;

    private final S3Service s3Service;

    private final PasswordEncoder passwordEncoder;

    @Override
    public DefaultUserResponseDto saveUser(UserCreateRequestDto userCreateRequestDto) {

        if (userRepository.existsByEmail(userCreateRequestDto.getEmail()))
            throw new DataIntegrityViolationException("Email đã tồn tại");

        User user = new User(
                userCreateRequestDto.getEmail().trim(),
                userCreateRequestDto.getName(),
                passwordEncoder.encode(userCreateRequestDto.getPassword().trim()),
                userCreateRequestDto.getDob(),
                userCreateRequestDto.getAddress(),
                userCreateRequestDto.getGender()
        );

        if (userCreateRequestDto.getCompany() != null)
            handleSetCompany(user, userCreateRequestDto.getCompany().getId());

        if (userCreateRequestDto.getRole() != null)
            handleSetRole(user, userCreateRequestDto.getRole().getId());


        User savedUser = userRepository.saveAndFlush(user);

        return mapToResponseDto(savedUser);
    }

    @Override
    public Page<DefaultUserResponseDto> findAllUser(Specification<User> spec, Pageable pageable) {
        return userRepository
                .findAll(spec, pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public DefaultUserResponseDto findUserById(Long id) {
        return userRepository
                .findById(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng")
                );
    }

    @Override
    public DefaultUserResponseDto updateUser(UserUpdateRequestDto userUpdateRequestDto) {

        User user = userRepository
                .findById(userUpdateRequestDto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        user.setName(userUpdateRequestDto.getName());
        user.setDob(userUpdateRequestDto.getDob());
        user.setAddress(userUpdateRequestDto.getAddress());
        user.setGender(userUpdateRequestDto.getGender());

        if (userUpdateRequestDto.getCompany() != null) {
            Long companyId = userUpdateRequestDto.getCompany().getId();
            if (companyId == -1) user.setCompany(null);
            else handleSetCompany(user, companyId);
        }

        if (userUpdateRequestDto.getRole() != null) {
            Long roleId = userUpdateRequestDto.getRole().getId();
            if (roleId == -1) user.setRole(null);
            else handleSetRole(user, roleId);
        }

        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    @Override
    public DefaultUserResponseDto deleteUserById(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng")
                );

        user.setRole(null);
        user.setCompany(null);

        Company company = companyRepository.findByOwnerEmail(user.getEmail());
        if (company != null) company.setOwner(null);

        List<Resume> resumes = user.getResumes();
        resumes.forEach(x -> s3Service.deleteFileByKey(x.getFileKey()));

        userRepository.delete(user);
        return mapToResponseDto(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
    }

    @Override
    public DefaultUserResponseDto updateSelfUserProfile(SelfUserUpdateProfileRequestDto selfUserUpdateProfileRequestDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = findByEmail(email);

        user.setName(selfUserUpdateProfileRequestDto.getName());
        user.setDob(selfUserUpdateProfileRequestDto.getDob());
        user.setAddress(selfUserUpdateProfileRequestDto.getAddress());
        user.setGender(selfUserUpdateProfileRequestDto.getGender());

        User savedUser = userRepository.save(user);

        return mapToResponseDto(savedUser);
    }

    @Override
    public DefaultUserResponseDto updateSelfUserPassword(SelfUserUpdatePasswordRequestDto selfUserUpdatePasswordRequestDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = findByEmail(email);

        if (!passwordEncoder.matches(
                selfUserUpdatePasswordRequestDto.getOldPassword(),
                user.getPassword())
        ) throw new DataIntegrityViolationException("Mật khẩu hiện tại không chính xác");

        String encodedPassword = passwordEncoder.encode(
                selfUserUpdatePasswordRequestDto.getNewPassword()
        );

        user.setPassword(encodedPassword);

        User savedUser = userRepository.saveAndFlush(user);

        return mapToResponseDto(savedUser);
    }

    @Override
    public void updateSelfUserAvatar(MultipartFile avatarFile) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = findByEmail(email);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String url = s3Service.uploadFile(avatarFile, "avatar", user.getId().toString(), true);

            user.setLogoUrl(url);
        }

        user.setUpdatedAt(Instant.now());
        userRepository.saveAndFlush(user);
    }


    private DefaultUserResponseDto mapToResponseDto(User user) {
        DefaultUserResponseDto.CompanyInformationDto company = null;
        if (user.getCompany() != null)
            company = new DefaultUserResponseDto.CompanyInformationDto(
                    user.getCompany().getId(),
                    user.getCompany().getName(),
                    user.getCompany().getAddress(),
                    (user.getCompany().getCompanyLogo() == null ? "" : user.getCompany().getCompanyLogo().getLogoUrl())
            );

        DefaultUserResponseDto.RoleInformationDto role = null;
        if (user.getRole() != null)
            role = new DefaultUserResponseDto.RoleInformationDto(
                    user.getRole().getId(),
                    user.getRole().getName(),
                    user.getRole().getDescription()
            );

        return new DefaultUserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDob(),
                user.getAddress(),
                user.getGender(),
                user.getLogoUrl(),
                company,
                role,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private void handleSetCompany(User user, Long id) {
        Company company = companyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy không công ty"));
        user.setCompany(company);
    }

    private void handleSetRole(User user, Long id) {
        Role role = roleRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy không chức vụ"));
        user.setRole(role);
    }


}
