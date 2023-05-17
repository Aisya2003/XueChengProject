package com.example.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.content.model.dto.CourseCategoryDto;
import com.example.content.model.po.CourseCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {
    public List<CourseCategoryDto> selectTreeNodes(@Param("id") String id);
}
