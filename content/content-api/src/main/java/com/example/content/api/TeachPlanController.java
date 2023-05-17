package com.example.content.api;

import com.example.content.model.dto.BindTeachPlanMediaDto;
import com.example.content.model.dto.SaveTeachPlanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.service.ITeachPlanService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划接口
 */
@Api(value = "课程计划接口", tags = "课程计划接口")
@RestController
@Slf4j
public class TeachPlanController {
    private final ITeachPlanService teachPlanService;

    @Autowired
    public TeachPlanController(ITeachPlanService teachPlanService) {
        this.teachPlanService = teachPlanService;
    }

    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachPlanDto> getTreeNodes(@PathVariable("courseId") Long courseId) {
        return teachPlanService.selectTeachPlanTree(courseId);
    }

    @PostMapping("/teachplan")
    public List<TeachPlanDto> saveTeachPlan(@RequestBody SaveTeachPlanDto dto) {
        teachPlanService.saveTeachPlan(dto);
        return teachPlanService.selectTeachPlanTree(dto.getCourseId());
    }

    @PostMapping("/teachplan/association/media")
    public void bindMedia(@RequestBody BindTeachPlanMediaDto dto) {
        teachPlanService.bindMedia(dto);
    }
}
