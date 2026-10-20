package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     * */
    public void saveWithFlavor(DishDTO dishDTO);  // 因为要同时操作两张表  一张 菜品  一张 菜品的风味表

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询对应菜品及其分类
     * */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 根据id修改对应的菜品信息
     * */
    void update(DishDTO dishDTO);

    /**
     * 根据分类id查询菜品
     * */
    List<Dish> list(Long categoryId);

    /**
     * 条件查询菜品和口味
     */
    List<DishVO> listWithFlavor(Dish dish);


    /*
    * 设置菜品的起售和停售
    * */
    void startOrStop(Integer status, Long id);
}
