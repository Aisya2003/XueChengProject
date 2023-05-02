package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.base.constant.RedisConstant;
import com.example.system.mapper.DictionaryMapper;
import com.example.system.model.po.Dictionary;
import com.example.system.service.IDictionaryService;
import com.example.system.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * <p>
 * 数据字典 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class DictionaryServiceImpl extends ServiceImpl<DictionaryMapper, Dictionary> implements IDictionaryService {

    private final CacheUtil cacheUtil;

    @Autowired
    public DictionaryServiceImpl(CacheUtil cacheUtil) {
        this.cacheUtil = cacheUtil;
    }

    @Override
    public List<Dictionary> queryAll() {
        return cacheUtil.buildArrayCache(
                RedisConstant.DICTIONARY_QUERY_PREFIX,
                "",
                Dictionary.class,
                new Function<Object, List<Dictionary>>() {
                    @Override
                    public List<Dictionary> apply(Object o) {
                        return list();
                    }
                }, 365L, TimeUnit.DAYS
        );

    }

    @Override
    public Dictionary getByCode(String code) {
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dictionary::getCode, code);
        return this.getOne(queryWrapper);
    }
}
