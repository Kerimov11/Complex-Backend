package az.arena.icearena.service;

import az.arena.icearena.dto.admin.CreateStaffRequest;
import az.arena.icearena.dto.admin.UpdateStaffRequest;
import az.arena.icearena.dto.admin.UserAdminDto;
import az.arena.icearena.dto.admin.UserRoleUpdateRequest;
import az.arena.icearena.entity.RoleName;
import java.util.List;

public interface UserAdminService {
    List<UserAdminDto> listUsers(String query);

    List<UserAdminDto> listStaff(String query);

    UserAdminDto getUser(Long id);

    UserAdminDto createStaff(CreateStaffRequest request);

    UserAdminDto createStaffMember(CreateStaffRequest request);

    UserAdminDto updateStaffRole(Long id, RoleName roleName);

    UserAdminDto updateStaffMember(Long id, UpdateStaffRequest request);

    void deleteStaffMember(Long id, String actorUsername);

    UserAdminDto updateRoles(Long id, UserRoleUpdateRequest request, String actorUsername);

    UserAdminDto addRole(Long id, RoleName roleName);

    UserAdminDto removeRole(Long id, RoleName roleName, String actorUsername);
}
