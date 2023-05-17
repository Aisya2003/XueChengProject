package com.example.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learning.model.po.LearnRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface LearnRecordMapper extends BaseMapper<LearnRecord> {

    @Insert("insert into xc_learn_record(course_id,course_name,user_id,learn_date,learn_length,teachplan_id,teachplan_name) select #{courseId},'',#{userId},now(),0,#{teachplanId},'' where not exists(select 1 from xc_learn_record t where t.course_id=#{courseId} and t.user_id=#{userId} and t.teachplan_id=#{teachplanId})")
    public void initLearnRecord(@Param("userId") String userId, @Param("courseId") Long courseId, @Param("teachplanId") Long teachplanId);


}
