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
        //1.??????????????????
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //2.??????????????????
        //2.1??????????????????????????????
        queryWrapper.like(StringUtils.isNotEmpty(dto.getCourseName()), CourseBase::getName, dto.getCourseName());
        //2.2??????????????????
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getPublishStatus()),CourseBase::getStatus,dto.getPublishStatus());
        //2.3??????????????????
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getAuditStatus()),CourseBase::getAuditStatus,dto.getAuditStatus());


        //3.??????????????????
        Page<CourseBase> page = new Page<>();
        page.setCurrent(params.getPageNo());
        page.setSize(params.getPageSize());

        //E page ????????????, @Param("ew") Wrapper<T> queryWrapper ????????????
        //4.????????????????????????
        Page<CourseBase> pageResultRaw = courseBaseMapper.selectPage(page, queryWrapper);
        //5.?????????PageResult??????
        PageResult<CourseBase> pageResult = new PageResult<>(
                pageResultRaw.getRecords(),//????????????
                pageResultRaw.getTotal(),//??????
                params.getPageNo(),//??????
                params.getPageSize()//?????????????????????
        );

        return pageResult;

    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //????????????
        /*if (StringUtils.isBlank(dto.getName())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new XuechengPlusException("??????????????????");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new XuechengPlusException("??????????????????");
        }*/
        //?????????mapper???????????????
        CourseBase courseBase = new CourseBase();

        BeanUtils.copyProperties(dto,courseBase);

        //????????????id
        courseBase.setCompanyId(companyId);
        //??????????????????
        courseBase.setCreateDate(LocalDateTime.now());

        //????????????????????????????????????
        courseBase.setAuditStatus("202002");
        //??????????????????????????????
        courseBase.setStatus("203001");
        //??????base???
        //???????????????courseBase?????????id?????????
        int courseBaseInsertResult = courseBaseMapper.insert(courseBase);

        //????????????id
        Long courseId = courseBase.getId();

        //??????market??????id
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseId);
        BeanUtils.copyProperties(dto,courseMarket);

        int courseMarketInsertResult = saveCourseMarket(courseMarket);


        //????????????
        if (courseBaseInsertResult <= 0 || courseMarketInsertResult <= 0){
            throw new RuntimeException("??????????????????");
        }
        //???????????????????????????????????????CourseBaseInfoDto??????
        CourseBaseInfoDto courseBaseInfoDto = getCourseBaseInfoDto(courseId);
        return courseBaseInfoDto;
    }

    /**
     *????????????id???????????????????????????
     * @param courseId ??????id
     * @return CourseBaseInfoDto
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfoDto(Long courseId) {
        //????????????????????????
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //??????dto
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        //??????????????????????????????
        if (courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        }

        //??????????????????
        String mtName = courseBaseInfoDto.getMtName();
        String stName = courseBaseInfoDto.getStName();

        //??????categoryMapper??????????????????
        CourseCategory categoryMtName = courseCategoryMapper.selectById(mtName);
        CourseCategory categoryStName = courseCategoryMapper.selectById(stName);

        if (categoryStName != null){
            //???????????????
            courseBaseInfoDto.setStName(categoryStName.getName());
        }
        if (categoryMtName != null){
            //???????????????
            courseBaseInfoDto.setMtName(categoryMtName.getName());
        }
        return courseBaseInfoDto;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBaseInfo(Long companyId, EditCourseDto dto) {
        //??????
        //??????id
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase == null){
            throw new XuechengPlusException("??????????????????");
        }
        //??????id
        if (courseBase.getCompanyId() != companyId){
            throw new XuechengPlusException("?????????????????????????????????");
        }
        //???????????????????????????
        BeanUtils.copyProperties(dto,courseBase);
        //????????????????????????
        courseBase.setChangeDate(LocalDateTime.now());

        //???????????????????????????
        int baseResult = courseBaseMapper.updateById(courseBase);

        //???????????????????????????
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

        saveCourseMarket(courseMarket);


        //????????????????????????
        CourseBaseInfoDto courseBaseInfoDto = this.getCourseBaseInfoDto(courseBase.getId());

        return courseBaseInfoDto;
    }

    private int saveCourseMarket(CourseMarket courseMarket) {
        //?????????????????????????????????????????????????????????
        if ("201001".equals(courseMarket.getCharge())){
            if (courseMarket.getPrice() == null || courseMarket.getPrice() <= 0){
                throw new RuntimeException("???????????????????????????!");
            }
        }


        //??????????????????????????????????????????
        boolean marketResult = courseMarketService.saveOrUpdate(courseMarket);
        return marketResult ? 1 : 0;
    }
}
