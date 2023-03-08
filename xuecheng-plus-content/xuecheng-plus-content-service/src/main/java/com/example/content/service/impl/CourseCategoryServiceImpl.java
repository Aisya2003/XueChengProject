package com.example.content.service.impl;

import com.example.content.mapper.CourseCategoryMapper;
import com.example.content.model.dto.CourseCategoryDto;
import com.example.content.service.ICourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 返回课程分类Service
 */
@Service
@Slf4j
public class CourseCategoryServiceImpl implements ICourseCategoryService {
    private CourseCategoryMapper mapper;

    @Autowired
    public CourseCategoryServiceImpl(CourseCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<CourseCategoryDto> queryTreeNodes(String id) {
        //根据传入的id获取该节点下面的所有子节点
        List<CourseCategoryDto> courseCategoryDtos = mapper.selectTreeNodes(id);
        //需要返回包括了根节点下面的直接下属节点
        //定义一个list作为最终返回数据
        List<CourseCategoryDto> courseCategoryDtoList = new ArrayList<>();
        //定义一个map用来方便寻找父节点，每一个父节点只存在一个在map中
        Map<String,CourseCategoryDto> parentNodes = new HashMap<>();
        //遍历每一个节点
        courseCategoryDtos.stream().forEach(
                item ->{
                    parentNodes.put(item.getId(),item);
                    //如果传入的id与子节点的parentid相同，则说明这个子节点是直接下属
                    //添加到返回列表中
                    if (id.equals(item.getParentid())){
                        courseCategoryDtoList.add(item);
                    }

                    //接下来向直接下属节点添加下属节点
                    //根据父节点id找到父节点对象，父节点就是曾经存储在map中的item对象
                    CourseCategoryDto parentNode = parentNodes.get(item.getParentid());
                    //判断父节点是否为空
                    if (parentNode != null){
                        //获取父节点的childrenTreeNodes
                        List<CourseCategoryDto> childrenTreeNodes = parentNode.getChildrenTreeNodes();
                        //判断childTreeNodes是否为第一次创建
                        if (childrenTreeNodes == null) {
                            //第一次添加则新建一个childTreeNodes
                            parentNode.setChildrenTreeNodes(new ArrayList<CourseCategoryDto>());
                        }
                        //不是第一次添加则正常添加
                        parentNode.getChildrenTreeNodes().add(item);

                    }
                }
        );
        return courseCategoryDtoList;
    }
}
