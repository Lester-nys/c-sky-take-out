package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
       // 判断状态：起售中的套餐不能删除
       ids.forEach(id -> {
           Setmeal setmeal = setmealMapper.getById(id);
           if (StatusConstant.ENABLE == setmeal.getStatus())
               throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
       });

       // 删除套餐表中的数据 以及 套餐菜品关系表 的数据
       ids.forEach(setmealId -> {
           setmealMapper.deleteById(setmealId);
           setmealDishMapper.deleteBySetmealId(setmealId);
       });

    }

    /**
     * 新增套餐   同时需要保存套餐和菜品之间的关系
     * 涉及套餐表 和 菜品表 之间的关联
     * */
    @Transactional   // 涉及多张表的操作一定要用事务
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        // 向套餐表插入数据
        setmealMapper.insert(setmeal);

        // 获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();  // 有一堆套餐要处理
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        // 保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum,pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }
}
