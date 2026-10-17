package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 插入口味数据
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 根据主键删除菜品数据
     * */
    @Delete("delete from dish_flavor where id = #{dishId}")
    void deleteById(Long dishId);

    /**
     * 根据id查询对应的风味数据
     * */
    @Select("select * from dish_flavor where dish_id = #{dish_id}")
    List<DishFlavor> getByDishId(Long id);
}
