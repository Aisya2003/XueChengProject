package com.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.content.mapper.TeachplanMapper;
import com.example.content.model.dto.SaveTeachplanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.Teachplan;
import com.example.content.service.ITeachPlanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachPlanServiceImp implements ITeachPlanService {
    private TeachplanMapper teachplanMapper;

    @Autowired
    public TeachPlanServiceImp(TeachplanMapper teachplanMapper) {
        this.teachplanMapper = teachplanMapper;
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
            BeanUtils.copyProperties(dto,teachplan);
            //设置orderBy的的值
            int orderBy = getTeachPlanCount(dto.getCourseId(),dto.getParentid());
            orderBy += 1;
            teachplan.setOrderby(orderBy);
            teachplanMapper.insert(teachplan);
        }else {//如果存在则更新
            BeanUtils.copyProperties(dto,teachplan);
            //更新
            teachplanMapper.updateById(teachplan);
        }


    }

    private int getTeachPlanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(courseId !=null,Teachplan::getCourseId,courseId);
        queryWrapper.eq(parentid!=null,Teachplan::getParentid,parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count.intValue();
    }

}
