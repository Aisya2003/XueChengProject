package com.example.system.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheUtil {
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public CacheUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //构建缓存工具
    public <R, ID> R buildCache(
            String keyPrefix,
            ID id,
            Class<R> classType,
            Function<ID, R> dbQuery,
            Long cacheTime,
            TimeUnit timeUnit
    ) {
        //查询是否存在换粗
        String key = keyPrefix + id;
        String cacheJson = stringRedisTemplate.opsForValue().get(key);
        //存在则返回
        if (StringUtils.isNotEmpty(cacheJson)) return JSON.parseObject(cacheJson, classType);

        //不存在则构建缓存
        R result = dbQuery.apply(id);
        //数据库不存在信息时保存一个空值防止穿透
        if (Objects.isNull(result)) stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            //存在则构建缓存
        else stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(result), cacheTime, timeUnit);


        return result;
    }

    public void removeCache(String key) {
        stringRedisTemplate.delete(key);
    }

    public <R, ID> List<R> buildArrayCache(
            String keyPrefix,
            ID id,
            Class<R> classType,
            Function<ID, List<R>> dbQuery,
            Long cacheTime,
            TimeUnit timeUnit
    ) {
        //查询是否存在换粗
        String key = keyPrefix + id;
        String cacheJson = stringRedisTemplate.opsForValue().get(key);
        //存在则返回
        if (StringUtils.isNotEmpty(cacheJson)) return JSON.parseArray(cacheJson, classType);

        //不存在则构建缓存
        List<R> result = dbQuery.apply(id);
        //数据库不存在信息时保存一个空值防止穿透
        if (Objects.isNull(result)) stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            //存在则构建缓存
        else stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(result), cacheTime, timeUnit);


        return result;
    }

}
