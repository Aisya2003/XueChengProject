package com.example.content.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class EditCourseDto extends AddCourseDto {
    /**
     * 课程ID
     */
    private Long id;
}
