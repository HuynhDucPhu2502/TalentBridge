package me.huynhducphu.talent_bridge.dto.response.skill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 6/23/2025
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DefaultSkillResponseDto {

    private Long id;
    private String name;
    private String createdAt;
    private String updatedAt;

}
