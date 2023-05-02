package com.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.constant.RedisConstant;
import com.example.base.exception.BusinessException;
import com.example.content.mapper.TeachPlanMapper;
import com.example.content.mapper.TeachPlanMediaMapper;
import com.example.content.model.dto.BindTeachPlanMediaDto;
import com.example.content.model.dto.SaveTeachPlanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.TeachPlanMedia;
import com.example.content.model.po.TeachPlan;
import com.example.content.service.ITeachPlanService;
import com.example.content.util.CacheUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class TeachPlanServiceImp implements ITeachPlanService {
    private final TeachPlanMapper teachplanMapper;
    private final TeachPlanMediaMapper teachplanMediaMapper;
    private final CacheUtil cacheUtil;

    @Autowired
    public TeachPlanServiceImp(TeachPlanMapper teachplanMapper, TeachPlanMediaMapper teachplanMediaMapper, CacheUtil cacheUtil) {
        this.teachplanMapper = teachplanMapper;
        this.teachplanMediaMapper = teachplanMediaMapper;
        this.cacheUtil = cacheUtil;
    }

    @Override
    public List<TeachPlanDto> selectTeachPlanTree(Long courseId) {
        //建立教学计划缓存
        return cacheUtil.buildArrayCache(
                RedisConstant.TEACH_PLAN_QUERY_PREFIX + courseId,
                courseId,
                TeachPlanDto.class,
                new Function<Long, List<TeachPlanDto>>() {
                    @Override
                    public List<TeachPlanDto> apply(Long aLong) {
                        return teachplanMapper.selectTreeNodes(aLong);
                    }
                },
                365L, TimeUnit.DAYS
        );
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
            //更新缓存
            cacheUtil.removeCache(RedisConstant.TEACH_PLAN_QUERY_PREFIX + dto.getId());
        }
    }

    @Override
    public TeachPlanMedia bindMedia(BindTeachPlanMediaDto dto) {
        //约束校验
        Long teachPlanId = dto.getTeachplanId();
        TeachPlan teachplan = teachplanMapper.selectById(teachPlanId);
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
        deleteMapper.eq(TeachPlanMedia::getTeachplanId, teachPlanId);
        teachplanMediaMapper.delete(deleteMapper);
        //添加关系
        TeachPlanMedia teachplanMedia = new TeachPlanMedia();
        teachplanMedia.setMediaId(dto.getMediaId());
        teachplanMedia.setMediaFilename(dto.getFileName());
        teachplanMedia.setTeachplanId(teachPlanId);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }

    private int getTeachPlanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<TeachPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(courseId != null, TeachPlan::getCourseId, courseId);
        queryWrapper.eq(parentid != null, TeachPlan::getParentid, parentid);
        return teachplanMapper.selectCount(queryWrapper);
    }

}
