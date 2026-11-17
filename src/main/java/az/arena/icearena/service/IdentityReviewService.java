package az.arena.icearena.service;

import az.arena.icearena.dto.admin.IdentityVerificationAdminDto;
import java.time.LocalDate;
import java.util.List;

public interface IdentityReviewService {

    List<IdentityVerificationAdminDto> list(String status);

    List<IdentityVerificationAdminDto> list(String status, LocalDate date);

    IdentityVerificationAdminDto approve(Long userId, String reviewerUsername);

    IdentityVerificationAdminDto reject(Long userId, String reason, String reviewerUsername);

    IdentityVerificationAdminDto approveTicket(String ticketCode, String reviewerUsername);

    IdentityVerificationAdminDto rejectTicket(String ticketCode, String reason, String reviewerUsername);
}
