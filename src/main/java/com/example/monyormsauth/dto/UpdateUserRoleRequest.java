package com.example.monyormsauth.dto;

import com.example.monyormsauth.model.enumerator.ERole;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    private ERole role;
}
