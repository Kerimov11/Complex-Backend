package az.arena.icearena.service;

import az.arena.icearena.dto.admin.IdentityVerificationAdminDto;
import java.util.List;

public interface IdentityReviewService {

    List<IdentityVerificationAdminDto> list(String status);

    IdentityVerificationAdminDto approve(Long userId, String reviewerUsername);

    IdentityVerificationAdminDto reject(Long userId, String reason, String reviewerUsername);
}
