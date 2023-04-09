package com.example.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.base.constant.Dictionary;
import com.example.base.exception.BusinessException;
import com.example.content.config.MultipartSupportConfig;
import com.example.content.feignclient.CourseIndex;
import com.example.content.feignclient.MediaServiceClient;
import com.example.content.feignclient.SearchServiceClient;
import com.example.content.mapper.CourseBaseMapper;
import com.example.content.mapper.CourseMarketMapper;
import com.example.content.mapper.CoursePublishMapper;
import com.example.content.mapper.CoursePublishPreMapper;
import com.example.content.model.dto.CourseBaseInfoDto;
import com.example.content.model.dto.CoursePreviewDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.CourseBase;
import com.example.content.model.po.CourseMarket;
import com.example.content.model.po.CoursePublish;
import com.example.content.model.po.CoursePublishPre;
import com.example.content.service.ICourseBaseInfoService;
import com.example.content.service.ICoursePublishService;
import com.example.content.service.ITeachPlanService;
import com.example.messagesdk.model.po.MqMessage;
import com.example.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class CoursePublishService extends ServiceImpl<CoursePublishPreMapper, CoursePublishPre> implements ICoursePublishService {
    private final ICoursePublishService coursePublishService;
    private final ICourseBaseInfoService courseBaseInfoService;
    private final ITeachPlanService teachPlanService;
    private final CourseMarketMapper courseMarketMapper;
    private final CoursePublishPreMapper coursePublishPreMapper;
    private final CourseBaseMapper courseBaseMapper;
    private final CoursePublishMapper coursePublishMapper;
    private final MqMessageService mqMessageService;
    private final MediaServiceClient mediaServiceClient;
    private final SearchServiceClient searchServiceClient;

    @Autowired
    @Lazy
    public CoursePublishService(ICourseBaseInfoService courseBaseInfoService,
                                ITeachPlanService teachPlanService,
                                CourseMarketMapper courseMarketMapper,
                                CoursePublishPreMapper coursePublishPreMapper,
                                CourseBaseMapper courseBaseMapper,
                                CoursePublishMapper coursePublishMapper,
                                MqMessageService mqMessageService,
                                MediaServiceClient mediaServiceClient,
                                ICoursePublishService coursePublishService,
                                SearchServiceClient searchServiceClient) {
        this.courseBaseInfoService = courseBaseInfoService;
        this.teachPlanService = teachPlanService;
        this.courseMarketMapper = courseMarketMapper;
        this.coursePublishPreMapper = coursePublishPreMapper;
        this.courseBaseMapper = courseBaseMapper;
        this.coursePublishMapper = coursePublishMapper;
        this.mqMessageService = mqMessageService;
        this.mediaServiceClient = mediaServiceClient;
        this.coursePublishService = coursePublishService;
        this.searchServiceClient = searchServiceClient;
    }

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        //基本信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfoDto(courseId);

        //设置分类名称
        courseBaseInfoService.getCategoryName(courseBaseInfoDto);

        List<TeachPlanDto> teachPlanDtos = teachPlanService.selectTeachPlanTree(courseId);
        //组装数据
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachPlans(teachPlanDtos);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitToPublishPre(Long courseId, Long companyId) {
        //校验课程基本信息
        CourseBaseInfoDto courseBase = courseBaseInfoService.getCourseBaseInfoDto(courseId);
        if (courseBase == null) {
            BusinessException.cast("课程不存在！");
            return;
        }
        if (!courseBase.getCompanyId().equals(companyId)) {
            BusinessException.cast("非本机构课程不能提交！");
            return;
        }
        if (StringUtils.isEmpty(courseBase.getPic())) {
            BusinessException.cast("课程图片不能为空！");
            return;
        }
        String auditStatus = courseBase.getAuditStatus();
        if (Dictionary.AUDIT_COURSE_COMMIT.getCode().equals(auditStatus)) {
            BusinessException.cast("课程正在审核中，等待审核完成才可再次提交！");
        }
        //校验课程计划
        List<TeachPlanDto> teachPlanDtos = teachPlanService.selectTeachPlanTree(courseId);
        if (teachPlanDtos.size() <= 0) {
            BusinessException.cast("该课程还没有设置课程计划！");
            return;
        }

        //封装数据，基本信息、营销信息、课程计划、师资信息
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //基本信息
        BeanUtils.copyProperties(courseBase, coursePublishPre);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String marketJson = JSON.toJSONString(courseMarket);
        //课程计划
        String teachPlanJson = JSON.toJSONString(teachPlanDtos);

        coursePublishPre.setMarket(marketJson);
        coursePublishPre.setTeachplan(teachPlanJson);

        //师资信息

        //设置初始审核状态
        coursePublishPre.setStatus(Dictionary.AUDIT_COURSE_COMMIT.getCode());

        //是否已存在相同的预发布字段
        saveOrUpdate(coursePublishPre);

        //更新课程基本表状态
        CourseBase courseBaseUpdate = new CourseBase();
        courseBaseUpdate.setAuditStatus(Dictionary.AUDIT_COURSE_COMMIT.getCode());

        //局部更新课程基本信息表
        updateCourseBase(courseId, courseBaseUpdate);

    }

    /**
     * 更新课程基本信息表
     *
     * @param courseId         课程ID
     * @param courseBaseUpdate 需要更新的内容
     */
    private void updateCourseBase(Long courseId, CourseBase courseBaseUpdate) {
        LambdaQueryWrapper<CourseBase> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(CourseBase::getId, courseId);
        courseBaseMapper.update(courseBaseUpdate, updateWrapper);
    }

    @Override
    @Transactional
    public void coursePublish(Long companyId, Long courseId) {
        //校验是否提交审核
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            BusinessException.cast("请先提交审核！");
        }
        //校验机构
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            BusinessException.cast("非本机构课程不允许发布！");
        }
        //校验审核状态
        if (!coursePublishPre.getStatus().equals(Dictionary.AUDIT_COURSE_PASS.getCode())) {
            BusinessException.cast("课程审核通过后才可以发布！");
        }

        //保存课程到发布表
        coursePublishService.saveToCoursePublish(courseId);
        //保存到消息队列
        coursePublishService.saveToMessage(courseId);

        //删除预发布表信息
        coursePublishPreMapper.deleteById(courseId);

    }

    @Override
    public File generateHtml(Long courseId) {
        try {
            //配置FreeMarker
            Configuration configuration = new Configuration(Configuration.getVersion());
            //获取classPath路径
            String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
            //设置FreeMarker加载templates的路径
            configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");
            //加载指定模板
            Template template = configuration.getTemplate("course_template.ftl");
            //获取模板数据
            CoursePreviewDto modelData = this.getCoursePreviewInfo(courseId);
            //封装数据
            HashMap<String, Object> map = new HashMap<>();
            map.put("model", modelData);


            //模板静态化
            String pageContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //创建临时文件
            File targetHtmlFile = File.createTempFile("course", ".html");
            log.debug("课程静态化临时文件生成,位置:{}", targetHtmlFile.getAbsolutePath());
            try (InputStream inputStream = IOUtils.toInputStream(pageContent);
                 FileOutputStream fileOutputStream = new FileOutputStream(targetHtmlFile);) {
                IOUtils.copy(inputStream, fileOutputStream);
                return targetHtmlFile;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void uploadHTMLToMinIo(File file, Long courseId) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        //远程调用
        String result = mediaServiceClient.upLoad(multipartFile, "course", courseId + ".html");
        //校验结果
        if (result == null) {
            BusinessException.cast("远程调用失败！");
        }

    }

    /**
     * 保存数据到Message表
     *
     * @param courseId 课程ID
     */
    @Transactional
    public void saveToMessage(Long courseId) {
        MqMessage course_publish = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (course_publish == null) {
            BusinessException.cast("添加消息失败！");
        }
    }

    /**
     * 保存信息到发布表
     *
     * @param courseId 课程ID
     */
    @Transactional
    public void saveToCoursePublish(Long courseId) {
        //获取预发布表数据
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);

        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        //设置状态为已发布
        coursePublish.setStatus(Dictionary.PUBLISH_PUB.getCode());
        //如果发布表中已存在则更新
        CoursePublish coursePublishOld = coursePublishMapper.selectById(courseId);
        if (coursePublishOld == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }
        //更新课程基本信息表为已发布
        CourseBase courseBaseUpdate = new CourseBase();
        courseBaseUpdate.setStatus(Dictionary.PUBLISH_PUB.getCode());
        updateCourseBase(courseId, courseBaseUpdate);
    }

    @Override
    public Boolean saveCourseIndex(Long courseId) {
        //查询课程发表表中的数据
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        if (coursePublish == null) {
            log.error("添加索引的记录不存在,索引ID:{}", courseId);
            BusinessException.cast("添加索引的记录不存在！");
        }
        CourseIndex courseIndex = new CourseIndex();
        //拷贝
        BeanUtils.copyProperties(coursePublish, courseIndex);

        Boolean result = searchServiceClient.add(courseIndex);
        if (result == null) {
            BusinessException.cast("创建课程索引失败！");
        }
        return result;
    }

    @Override
    public CoursePublish getCoursePublishByCourseId(Long courseId) {
        if (courseId == null) {
            BusinessException.cast("课程号不能为空！");
        }
        return coursePublishMapper.selectById(courseId);
    }

    @Override
    public CoursePreviewDto getCoursePublishPreivewInfo(Long courseId) {
        CoursePublish coursePublish = this.getCoursePublishByCourseId(courseId);
        if (coursePublish == null) BusinessException.cast("不存在课程！");

        //组装返回对象
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish, courseBaseInfoDto);
        String teachPlanJson = coursePublish.getTeachplan();
        List<TeachPlanDto> teachPlanDtoList = JSON.parseArray(teachPlanJson, TeachPlanDto.class);


        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachPlans(teachPlanDtoList);

        return coursePreviewDto;
    }
}
