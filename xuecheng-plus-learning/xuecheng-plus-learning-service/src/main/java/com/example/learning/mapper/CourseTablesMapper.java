package com.example.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learning.model.dto.MyCourseTableItemDto;
import com.example.learning.model.dto.MyCourseTableParams;
import com.example.learning.model.po.CourseTables;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface CourseTablesMapper extends BaseMapper<CourseTables> {

    public List<MyCourseTableItemDto> myCourseTables(MyCourseTableParams params);

    public int myCourseTablesCount(MyCourseTableParams params);

}
