package com.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.base.exception.XuechengPlusException;
import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.content.mapper.CourseBaseMapper;
import com.example.content.mapper.CourseCategoryMapper;
import com.example.content.mapper.CourseMarketMapper;
import com.example.content.model.dto.AddCourseDto;
import com.example.content.model.dto.CourseBaseInfoDto;
import com.example.content.model.dto.EditCourseDto;
import com.example.content.model.dto.QueryCourseParamsDto;
import com.example.content.model.po.CourseBase;
import com.example.content.model.po.CourseCategory;
import com.example.content.model.po.CourseMarket;
import com.example.content.service.ICourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements ICourseBaseInfoService {
    private CourseBaseMapper courseBaseMapper;
    private CourseMarketMapper courseMarketMapper;
    private CourseCategoryMapper courseCategoryMapper;
    private CourseMarketServiceImpl courseMarketService;

    @Autowired
    public CourseBaseInfoServiceImpl(
            CourseBaseMapper courseBaseMapper,
            CourseMarketMapper courseMarketMapper,
            CourseCategoryMapper courseCategoryMapper,
            CourseMarketServiceImpl courseMarketService) {
        this.courseBaseMapper = courseBaseMapper;
        this.courseMarketMapper = courseMarketMapper;
        this.courseCategoryMapper = courseCategoryMapper;
        this.courseMarketService = courseMarketService;
    }

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams params, QueryCourseParamsDto dto) {
        //1.创建查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //2.拼接查询条件
        //2.1根据课程名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(dto.getCourseName()), CourseBase::getName, dto.getCourseName());
        //2.2根据发布状态
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getPublishStatus()),CourseBase::getStatus,dto.getPublishStatus());
        //2.3根据审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getAuditStatus()),CourseBase::getAuditStatus,dto.getAuditStatus());


        //3.创建分页参数
        Page<CourseBase> page = new Page<>();
        page.setCurrent(params.getPageNo());
        page.setSize(params.getPageSize());

        //E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        //4.查询得到分页结果
        Page<CourseBase> pageResultRaw = courseBaseMapper.selectPage(page, queryWrapper);
        //5.封装为PageResult对象
        PageResult<CourseBase> pageResult = new PageResult<>(
                pageResultRaw.getRecords(),//查询结果
                pageResultRaw.getTotal(),//总数
                params.getPageNo(),//页码
                params.getPageSize()//每页最大记录数
        );

        return pageResult;

    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //校验参数
        /*if (StringUtils.isBlank(dto.getName())) {
            throw new XuechengPlusException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new XuechengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new XuechengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new XuechengPlusException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new XuechengPlusException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new XuechengPlusException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new XuechengPlusException("收费规则为空");
        }*/
        //封装成mapper调用的对象
        CourseBase courseBase = new CourseBase();

        BeanUtils.copyProperties(dto,courseBase);

        //设置机构id
        courseBase.setCompanyId(companyId);
        //设置创建时间
        courseBase.setCreateDate(LocalDateTime.now());

        //设置审核状态默认为未提交
        courseBase.setAuditStatus("202002");
        //设置发布状态为未发布
        courseBase.setStatus("203001");
        //插入base表
        //插入结束后courseBase对象的id会生成
        int courseBaseInsertResult = courseBaseMapper.insert(courseBase);

        //获取课程id
        Long courseId = courseBase.getId();

        //设置market课程id
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseId);
        BeanUtils.copyProperties(dto,courseMarket);

        int courseMarketInsertResult = saveCourseMarket(courseMarket);


        //判断成功
        if (courseBaseInsertResult <= 0 || courseMarketInsertResult <= 0){
            throw new RuntimeException("添加课程失败");
        }
        //组装返回结果，需要返回一个CourseBaseInfoDto对象
        CourseBaseInfoDto courseBaseInfoDto = getCourseBaseInfoDto(courseId);
        return courseBaseInfoDto;
    }

    /**
     *根据课程id查询并封装返回对象
     * @param courseId 课程id
     * @return CourseBaseInfoDto
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfoDto(Long courseId) {
        //获取两个基本对象
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //填充dto
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        //课程营销信息可能为空
        if (courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        }

        //设置分类名称
        String mtName = courseBaseInfoDto.getMtName();
        String stName = courseBaseInfoDto.getStName();

        //根据categoryMapper查询对应名称
        CourseCategory categoryMtName = courseCategoryMapper.selectById(mtName);
        CourseCategory categoryStName = courseCategoryMapper.selectById(stName);

        if (categoryStName != null){
            //小分类名称
            courseBaseInfoDto.setStName(categoryStName.getName());
        }
        if (categoryMtName != null){
            //大分类名称
            courseBaseInfoDto.setMtName(categoryMtName.getName());
        }
        return courseBaseInfoDto;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBaseInfo(Long companyId, EditCourseDto dto) {
        //校验
        //课程id
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase == null){
            throw new XuechengPlusException("课程不存在！");
        }
        //机构id
        if (courseBase.getCompanyId() != companyId){
            throw new XuechengPlusException("只能修改本机构的课程！");
        }
        //封装基本信息的数据
        BeanUtils.copyProperties(dto,courseBase);
        //设置最新更改时间
        courseBase.setChangeDate(LocalDateTime.now());

        //更新课程基本信息表
        int baseResult = courseBaseMapper.updateById(courseBase);

        //封装营销信息的数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

        saveCourseMarket(courseMarket);


        //查询课程基本信息
        CourseBaseInfoDto courseBaseInfoDto = this.getCourseBaseInfoDto(courseBase.getId());

        return courseBaseInfoDto;
    }

    private int saveCourseMarket(CourseMarket courseMarket) {
        //判断课程是否收费，如果是则必须设置价格
        if ("201001".equals(courseMarket.getCharge())){
            if (courseMarket.getPrice() == null || courseMarket.getPrice() <= 0){
                throw new RuntimeException("收费课程请设置价格!");
            }
        }


        //对营销表有则更新，没有则创建
        boolean marketResult = courseMarketService.saveOrUpdate(courseMarket);
        return marketResult ? 1 : 0;
    }
}
