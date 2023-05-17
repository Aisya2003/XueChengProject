package com.example.content.model.dto;

import com.example.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

@Data
public class CourseCategoryDto extends CourseCategory {
    public List childrenTreeNodes;
}
