package com.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.exception.XuechengPlusException;
import com.example.content.mapper.TeachplanMapper;
import com.example.content.mapper.TeachplanMediaMapper;
import com.example.content.model.dto.BindTeachPlanMediaDto;
import com.example.content.model.dto.SaveTeachplanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.Teachplan;
import com.example.content.model.po.TeachplanMedia;
import com.example.content.service.ITeachPlanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachPlanServiceImp implements ITeachPlanService {
    private final TeachplanMapper teachplanMapper;
    private final TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    public TeachPlanServiceImp(TeachplanMapper teachplanMapper, TeachplanMediaMapper teachplanMediaMapper) {
        this.teachplanMapper = teachplanMapper;
        this.teachplanMediaMapper = teachplanMediaMapper;
    }

    @Override
    public List<TeachPlanDto> selectTeachPlanTree(Long courseId) {
        List<TeachPlanDto> teachPlanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachPlanDtos;
    }

    @Override
    public void saveTeachPlan(SaveTeachplanDto dto) {
        //先查询是否存在数据
        Teachplan teachplan = teachplanMapper.selectById(dto.getId());
        if (teachplan == null) {
            //添加到数据库中
            teachplan = new Teachplan();
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
    public TeachplanMedia bindMedia(BindTeachPlanMediaDto dto) {
        //约束校验
        Long teachplanId = dto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        //教学计划不存在
        if (teachplan == null) {
            XuechengPlusException.cast("课程计划不存在！");
        }
        //只有二级目录可以绑定关系
        if (teachplan.getGrade() != 2) {
            XuechengPlusException.cast("只有二级目录可以绑定！");
        }
        //删除原来绑定关系
        LambdaQueryWrapper<TeachplanMedia> deleteMapper = new LambdaQueryWrapper<>();
        deleteMapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
        teachplanMediaMapper.delete(deleteMapper);
        //添加关系
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setMediaId(dto.getMediaId());
        teachplanMedia.setMediaFilename(dto.getFileName());
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }

    private int getTeachPlanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(courseId != null, Teachplan::getCourseId, courseId);
        queryWrapper.eq(parentid != null, Teachplan::getParentid, parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count.intValue();
    }

}
