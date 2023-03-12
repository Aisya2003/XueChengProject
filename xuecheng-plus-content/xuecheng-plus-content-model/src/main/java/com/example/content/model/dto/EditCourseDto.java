package com.example.content.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class EditCourseDto extends AddCourseDto{
    //课程ID
    @NotEmpty(message = "课程Id不能为空")
    private Long id;
}
