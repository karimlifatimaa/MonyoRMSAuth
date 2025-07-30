package com.example.monyormsauth.auth.dto;

import com.example.monyormsauth.auth.model.enumerator.ERole;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    private ERole role;
}
