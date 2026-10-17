package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     * 涉及两张表 需要同时进行修改 涉及事务的一致性
     * */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO)
    {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 向菜品表插入一条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        if(flavors != null && flavors.size() > 0)
        {
            // 给每个菜的每个口味设置对应的菜的id
            flavors.forEach(dishFlavor -> {dishFlavor.setDishId(dishId);});
        }
        dishFlavorMapper.insertBatch(flavors);    // 每个菜有多种不同口味  需要批量插入

    }

    /**
     * 实现菜品分页查询
     * */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 实现菜品批量删除
     * */
    @Transactional  // 这是事务，因为涉及到 dish 和 dish_flavor 等多张表的同时操作
    public void deleteBatch(List<Long> ids) {
        // 1. 判断当前菜品能否一一删除：起售中的菜品不能删除
        for(Long id : ids)
        {
            Dish dish = dishMapper.getById(id);
            // 处于起售状态不可删除
            if(dish.getStatus() == StatusConstant.ENABLE)
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 2. 判断当前菜品能否一一删除：被套餐关联的菜品不能删除
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);

        if(setmealIds != null && setmealIds.size() > 0)
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);

        // 经过前两个判断排除风险之后就能执行删除操作
        for(Long id : ids)
        {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteById(id);
        }
    }

}
