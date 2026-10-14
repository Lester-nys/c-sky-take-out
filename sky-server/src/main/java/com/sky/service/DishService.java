package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     * */
    public void saveWithFlavor(DishDTO dishDTO);  // 因为要同时操作两张表  一张 菜品  一张 菜品的风味表

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);
}
