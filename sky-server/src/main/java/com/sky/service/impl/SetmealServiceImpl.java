package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
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
import java.util.Set;

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
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum,pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 修改套餐
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        // 1. 修改套餐表 执行update
        setmealMapper.update(setmeal);

        // 2. 删除套餐和菜品的关联关系 操作setmeal_dish表 执行delete
        Long setmealId = setmealDTO.getId();  // 获取套餐id 方便下面表的操作
        setmealDishMapper.deleteBySetmealId(setmealId);


        // 3. 重新插入套餐和菜品的关联关系  操作setmeal_dish表 执行insert
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();  // 获取套餐里面的dishes
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });    // 给这里的每一道菜设置所属的套餐id
        setmealDishMapper.insertBatch(setmealDishes);   // 这里就是再把刚刚设置好套餐id的 dishes 插入 setmeal_dish 表
    }

    /**
     * 起售停售套餐
     */
    public void startOrStop(Integer status, Long id) {
        // 起售套餐时，要检查套餐内是否有停售菜品，有要给提示“套餐内包含未启售菜品，无法启售”
        if(status == StatusConstant.ENABLE)
        {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0)
            {
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus())
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                });
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 通过id查询套餐
     */
    public SetmealVO getByIdWithDish(Long id) {
        // 根据id得到对应的套餐
        Setmeal setmeal = setmealMapper.getById(id);
        // 获取套餐里的dishes
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        // 设置VO 便于返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);  // 基本信息
        setmealVO.setSetmealDishes(setmealDishes);    // 套餐里各种菜的信息
        return setmealVO;
    }

}
