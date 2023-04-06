package com.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.exception.BusinessException;
import com.example.content.mapper.TeachPlanMapper;
import com.example.content.mapper.TeachPlanMediaMapper;
import com.example.content.model.dto.BindTeachPlanMediaDto;
import com.example.content.model.dto.SaveTeachPlanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.TeachPlanMedia;
import com.example.content.model.po.TeachPlan;
import com.example.content.service.ITeachPlanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachPlanServiceImp implements ITeachPlanService {
    private final TeachPlanMapper teachplanMapper;
    private final TeachPlanMediaMapper teachplanMediaMapper;

    @Autowired
    public TeachPlanServiceImp(TeachPlanMapper teachplanMapper, TeachPlanMediaMapper teachplanMediaMapper) {
        this.teachplanMapper = teachplanMapper;
        this.teachplanMediaMapper = teachplanMediaMapper;
    }

    @Override
    public List<TeachPlanDto> selectTeachPlanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachPlan(SaveTeachPlanDto dto) {
        //先查询是否存在数据
        TeachPlan teachplan = teachplanMapper.selectById(dto.getId());
        if (teachplan == null) {
            //添加到数据库中
            teachplan = new TeachPlan();
            BeanUtils.copyProperties(dto, teachplan);
            //设置orderBy的的值
            int orderBy = getTeachPlanCount(dto.getCourseId(), dto.getParentid());
            orderBy += 1;
            teachplan.setOrderby(orderBy);
            teachplanMapper.insert(teachplan);
        } else {//如果存在则更新
            BeanUtils.copyProperties(dto, teachplan);
            //更新
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    public TeachPlanMedia bindMedia(BindTeachPlanMediaDto dto) {
        //约束校验
        Long teachplanId = dto.getTeachplanId();
        TeachPlan teachplan = teachplanMapper.selectById(teachplanId);
        //教学计划不存在
        if (teachplan == null) {
            BusinessException.cast("课程计划不存在！");
        }
        //只有二级目录可以绑定关系
        if (teachplan.getGrade() != 2) {
            BusinessException.cast("只有二级目录可以绑定！");
        }
        //删除原来绑定关系
        LambdaQueryWrapper<TeachPlanMedia> deleteMapper = new LambdaQueryWrapper<>();
        deleteMapper.eq(TeachPlanMedia::getTeachplanId, teachplanId);
        teachplanMediaMapper.delete(deleteMapper);
        //添加关系
        TeachPlanMedia teachplanMedia = new TeachPlanMedia();
        teachplanMedia.setMediaId(dto.getMediaId());
        teachplanMedia.setMediaFilename(dto.getFileName());
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }

    private int getTeachPlanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<TeachPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(courseId != null, TeachPlan::getCourseId, courseId);
        queryWrapper.eq(parentid != null, TeachPlan::getParentid, parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count.intValue();
    }

}
