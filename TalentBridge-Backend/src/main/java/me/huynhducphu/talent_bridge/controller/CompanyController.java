package me.huynhducphu.talent_bridge.controller;

import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.huynhducphu.talent_bridge.annotation.ApiMessage;
import me.huynhducphu.talent_bridge.dto.request.company.DefaultCompanyRequestDto;
import me.huynhducphu.talent_bridge.dto.request.user.RecruiterRequestDto;
import me.huynhducphu.talent_bridge.dto.response.PageResponseDto;
import me.huynhducphu.talent_bridge.dto.response.ApiResponse;
import me.huynhducphu.talent_bridge.dto.response.company.DefaultCompanyExtendedResponseDto;
import me.huynhducphu.talent_bridge.dto.response.company.DefaultCompanyResponseDto;
import me.huynhducphu.talent_bridge.model.Company;
import me.huynhducphu.talent_bridge.service.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Company")
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ApiMessage(value = "Tạo Company")
    @PreAuthorize("hasAuthority('POST /companies')")
    @Operation(
            summary = "Tạo Company",
            description = "Yêu cầu quyền: <b>POST /companies</b>"
    )
    public ResponseEntity<?> saveCompany(
            @Valid @RequestPart("company") DefaultCompanyRequestDto defaultCompanyRequestDto,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(companyService.saveCompany(defaultCompanyRequestDto, logoFile, false));
    }

    @PostMapping("/me")
    @ApiMessage(value = "Tạo Company cho người dùng hiện tại")
    @PreAuthorize("hasAuthority('POST /companies/me')")
    @Operation(
            summary = "Tạo Company cho người dùng hiện tại",
            description = "Yêu cầu quyền: <b>POST /companies/me</b>"
    )
    public ResponseEntity<?> saveSelfCompany(
            @Valid @RequestPart("company") DefaultCompanyRequestDto defaultCompanyRequestDto,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(companyService.saveCompany(defaultCompanyRequestDto, logoFile, true));
    }

    @GetMapping
    @ApiMessage(value = "Lấy danh sách Company")
    @PreAuthorize("hasAuthority('GET /companies') OR isAnonymous()")
    @Operation(
            summary = "Lấy danh sách Company",
            description = "Yêu cầu quyền: <b>GET /companies</b>"
    )
    @SecurityRequirements()
    public ResponseEntity<?> findAllCompanies(
            @Filter Specification<Company> spec,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        Page<DefaultCompanyResponseDto> page = companyService.findAllCompanies(spec, pageable);

        PageResponseDto<DefaultCompanyResponseDto> res = new PageResponseDto<>(
                page.getContent(),
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(res);
    }

    @GetMapping("/with-jobs-count")
    @ApiMessage(value = "Lấy danh sách Company kèm với số lượng nghề")
    @PreAuthorize("hasAuthority('GET /companies/with-jobs-count') OR isAnonymous()")
    @Operation(
            summary = "Lấy danh sách Company kèm với số lượng nghề",
            description = "Yêu cầu quyền: <b>GET /companies/with-jobs-count</b>"
    )
    @SecurityRequirements()
    public ResponseEntity<?> findAllCompaniesWithJobsCount(
            @Filter Specification<Company> spec,
            @PageableDefault(size = 9) Pageable pageable
    ) {
        Page<DefaultCompanyExtendedResponseDto> page = companyService.findAllCompaniesWithJobsCount(spec, pageable);

        PageResponseDto<DefaultCompanyExtendedResponseDto> res = new PageResponseDto<>(
                page.getContent(),
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    @ApiMessage(value = "Lấy Company theo id")
    @PreAuthorize("hasAuthority('GET /companies/{id}') OR isAnonymous()")
    @Operation(
            summary = "Lấy Company theo id",
            description = "Yêu cầu quyền: <b>GET /companies/{id}</b>"
    )
    @SecurityRequirements()
    public ResponseEntity<DefaultCompanyResponseDto> findCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findCompanyById(id));
    }

    @GetMapping("/me")
    @ApiMessage(value = "Lấy Company theo người dùng hiện tại")
    @PreAuthorize("hasAuthority('GET /companies/me')")
    @Operation(
            summary = "Lấy Company theo người dùng hiện tại",
            description = "Yêu cầu quyền: <b>GET /companies/me</b>"
    )
    public ResponseEntity<?> findSelfCompany() {
        return ResponseEntity.ok(
                companyService.findSelfCompany()
        );
    }

    @GetMapping("/me/users")
    @ApiMessage(value = "Lấy danh sách users recruiter của người dùng hiện tại")
    @PreAuthorize("hasAuthority('GET /companies/me/users')")
    @Operation(
            summary = "Lấy danh sách users recruiter của người dùng hiện tại",
            description = "Yêu cầu quyền: <b>GET /companies/me/users</b>"
    )
    public ResponseEntity<?> findAllRecruitersBySelfCompany() {
        return ResponseEntity.ok(
                companyService.findAllRecruitersBySelfCompany()
        );
    }

    @PutMapping(value = "/{id}")
    @ApiMessage(value = "Cập nhật Company theo id")
    @PreAuthorize("hasAuthority('PUT /companies/{id}')")
    @Operation(
            summary = "Cập nhật Company theo id",
            description = "Yêu cầu quyền: <b>PUT /companies/{id}</b>"
    )
    public ResponseEntity<?> updateCompanyById(
            @Valid @RequestPart("company") DefaultCompanyRequestDto defaultCompanyRequestDto,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(companyService.updateCompany(defaultCompanyRequestDto, id, logoFile, false));
    }

    @PutMapping(value = "/me")
    @ApiMessage(value = "Cập nhật Company của người dùng hiện tại")
    @PreAuthorize("hasAuthority('PUT /companies/me')")
    @Operation(
            summary = "Cập nhật Company của người dùng hiện tại",
            description = "Yêu cầu quyền: <b>PUT /companies/me</b>"
    )
    public ResponseEntity<?> updateSelfCompany(
            @Valid @RequestPart("company") DefaultCompanyRequestDto defaultCompanyRequestDto,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile
    ) {
        return ResponseEntity.ok(companyService.updateCompany(
                defaultCompanyRequestDto, null, logoFile, true
        ));
    }

    @DeleteMapping("/{id}")
    @ApiMessage(value = "Xóa company theo id")
    @PreAuthorize("hasAuthority('DELETE /companies/{id}')")
    @Operation(
            summary = "Xóa company theo id",
            description = "Yêu cầu quyền: <b>DELETE /companies/{id}</b>"
    )
    public ResponseEntity<?> deleteCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Xóa công ty",
                        companyService.deleteCompanyById(id)
                )
        );
    }


    @PostMapping("/me/users")
    @ApiMessage(value = "Thêm người dùng khác vào company của người dùng hiện tại")
    @PreAuthorize("hasAuthority('POST /companies/me/users')")
    @Operation(
            summary = "Thêm người dùng khác vào company của người dùng hiện tại",
            description = "Yêu cầu quyền: <b>POST /companies/me/users</b>"
    )
    public ResponseEntity<Void> addMemberToCompany(
            @Valid @RequestBody RecruiterRequestDto recruiterRequestDto
    ) {
        companyService.addMemberToCompany(recruiterRequestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/users")
    @ApiMessage(value = "Loại bỏ người dùng khác khỏi company của người dùng hiện tại")
    @PreAuthorize("hasAuthority('PUT /companies/me/users')")
    @Operation(
            summary = "Loại bỏ người dùng khác khỏi company của người dùng hiện tại",
            description = "Yêu cầu quyền: <b>PUT /companies/me/users</b>"
    )
    public ResponseEntity<Void> removeMemberFromCompany(
            @Valid @RequestBody RecruiterRequestDto recruiterRequestDto
    ) {
        companyService.removeMemberFromCompany(recruiterRequestDto);
        return ResponseEntity.ok().build();
    }

}
