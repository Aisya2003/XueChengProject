package com.example.ucenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ucenter.model.po.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\txc_menu \n" +
            "WHERE\n" +
            "\tid IN (\n" +
            "\tSELECT\n" +
            "\t\tmenu_id \n" +
            "\tFROM\n" +
            "\t\txc_permission \n" +
            "WHERE\n" +
            "\trole_id IN ( SELECT role_id FROM xc_user_role WHERE user_id = 52 ))")
    List<Menu> getAuthoritiesByUserId(@Param("userId") String userId);
}
