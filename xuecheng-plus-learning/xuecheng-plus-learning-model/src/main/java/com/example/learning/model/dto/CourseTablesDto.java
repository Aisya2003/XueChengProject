package com.example.learning.model.dto;

import com.example.learning.model.po.CourseTables;
import lombok.Data;
import lombok.ToString;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的课程表模型类
 * @date 2022/10/2 16:09
 */
@Data
@ToString
public class CourseTablesDto extends CourseTables {
    //学习资格，[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    public String learnStatus;
}