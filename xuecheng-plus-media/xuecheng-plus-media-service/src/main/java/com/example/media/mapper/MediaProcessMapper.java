package com.example.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    /**
     * 根据分片参数和cpu的核心数，获取指定数量的待处理视频文件
     * @param shardTotal 分片总数
     * @param shardIndex 分片索引
     * @param coreCount 处理数量
     * @return 待处理视频文件
     */
    @Select("SELECT * FROM media_process WHERE id % #{shardTotal} = #{shardIndex} LIMIT #{coreCount}")
    public List<MediaProcess> selectListByShardInfo(@Param("shardTotal")int shardTotal,
                                                    @Param("shardIndex")int shardIndex,
                                                    @Param("coreCount")int coreCount);
}
