package com.example.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learning.model.dto.CourseTableItemDto;
import com.example.learning.model.dto.CourseTableRequestParams;
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

    public List<CourseTableItemDto> myCourseTables(CourseTableRequestParams params);

    public int myCourseTablesCount(CourseTableRequestParams params);

}
