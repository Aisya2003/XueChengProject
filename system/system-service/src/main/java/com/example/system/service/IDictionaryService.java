package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.model.po.Dictionary;

import java.util.List;

public interface IDictionaryService extends IService<Dictionary> {

    /**
     * 查询所有数据字典内容
     *
     * @return 数据字典信息列表
     */
    List<Dictionary> queryAll();

    /**
     * 根据code查询数据字典
     *
     * @param code -- String 数据字典Code
     * @return 数据字典内容
     */
    Dictionary getByCode(String code);
}
