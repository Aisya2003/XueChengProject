package com.example.content.service;

import com.example.content.model.dto.CoursePreviewDto;
import com.example.content.model.po.CoursePublish;

import java.io.File;

public interface ICoursePublishService {

    /**
     * 根据课程ID取出课程的基本信息和营销信息和师资信息
     *
     * @param courseId 课程ID
     * @return 课程预览信息
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 提交课程信息到预发布表等待审核
     *
     * @param courseId  课程id
     * @param companyId 机构id
     */
    void commitToPublishPre(Long courseId, Long companyId);

    /**
     * 发布课程
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    void coursePublish(Long companyId, Long courseId);

    /**
     * 根据课程ID生成静态页面
     *
     * @param courseId 课程ID
     * @return 文件
     */
    File generateHtml(Long courseId);

    /**
     * 上传静态页面到MinIO
     *
     * @param file     静态页面
     * @param courseId 课程ID
     */
    void uploadHTMLToMinIo(File file, Long courseId);

    /**
     * 保存数据到Message表
     *
     * @param courseId 课程ID
     */
    void saveToMessage(Long courseId);

    /**
     * 保存信息到发布表
     *
     * @param courseId 课程ID
     */
    void saveToCoursePublish(Long courseId);

    /**
     * 远程调用创建课程索引
     *
     * @param courseId 课程ID
     */
    Boolean saveCourseIndex(Long courseId);

    /**
     * 通过课程id
     * 查询课程发布表中的信息
     *
     * @param courseId 课程id
     * @return 课程发布表实体
     */
    CoursePublish getCoursePublishByCourseId(Long courseId);

    /**
     * 获取课程发布信息，以预览的格式返回
     *
     * @param courseId 课程id
     * @return 预览信息
     */
    CoursePreviewDto getCoursePublishPreivewInfo(Long courseId);
}
