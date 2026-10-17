package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SetmealService {

    /**
     * 删除套餐
     */
    void deleteBatch(List<Long> ids);

    /**
     * 新增套餐
     * */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
}
