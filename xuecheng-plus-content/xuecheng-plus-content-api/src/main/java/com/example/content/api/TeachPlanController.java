package com.example.content.api;

import com.example.content.model.dto.SaveTeachplanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.service.ITeachPlanService;
import com.example.content.service.impl.TeachPlanServiceImp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *课程计划接口
 */
@Api(value = "课程计划接口",tags = "课程计划接口")
@RestController
@Slf4j
public class TeachPlanController {
    private ITeachPlanService teachPlanService;

    @Autowired
    public TeachPlanController(ITeachPlanService teachPlanService) {
        this.teachPlanService = teachPlanService;
    }

    @ApiOperation("获取课程大纲")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachPlanDto> getTreeNodes(@PathVariable("courseId")Long courseId){
        return teachPlanService.selectTeachPlanTree(courseId);
    }

    @ApiOperation("修改或添加教学大纲")
    @PostMapping("/teachplan")
    public List<TeachPlanDto> saveTeachPlan(@RequestBody SaveTeachplanDto dto){
        teachPlanService.saveTeachPlan(dto);
        return teachPlanService.selectTeachPlanTree(dto.getCourseId());
    }
}
