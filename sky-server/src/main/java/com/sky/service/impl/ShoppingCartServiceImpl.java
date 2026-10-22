package com.sky.service.impl;


import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        // 获取当前登录用户ID
        Long userId = BaseContext.getCurrentId();

// 打印实际获得的用户ID
        log.info("添加购物车获取到的userId：{}", userId);

// 将用户ID设置到购物车对象
        shoppingCart.setUserId(userId);

// 再次确认对象内部的userId
        log.info("ShoppingCart中的userId：{}", shoppingCart.getUserId());

// 没有用户ID就不允许继续执行
        if (userId == null) {
            throw new IllegalStateException("添加购物车失败：用户ID为空");
        }


        // 判断当前商品是否在购物车中
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList != null && shoppingCartList.size() == 1)
        {
            // 如果已经存在就更新数量，数量加1
            shoppingCart = shoppingCartList.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);;
            shoppingCartMapper.updateNumberById(shoppingCart);

        }
        else
        {
            // 如果不存在，就插入数据，数量就是1

            // 判断当前要加入购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null)
            {
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }
            else
            {
                Setmeal setmeal= setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);

        }
    }

    /**
     * 查看购物车
     */
    public List<ShoppingCart> showShoppingCart() {
        return shoppingCartMapper.list(ShoppingCart.
                builder().
                userId(BaseContext.getCurrentId()).
                build());
    }

    /**
     * 清空购物车商品
     */
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    /**
     * 减少购物车中的商品数量
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {

        // 1. 获取当前登录用户ID
        Long userId = BaseContext.getCurrentId();

        if (userId == null) {
            throw new IllegalStateException("当前登录用户ID为空");
        }

        // 2. 创建购物车对象，作为查询条件
        ShoppingCart shoppingCart = new ShoppingCart();

        // 3. 将DTO中的商品信息复制到ShoppingCart
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 4. 设置当前登录用户ID
        shoppingCart.setUserId(userId);

        // 5. 查询当前用户对应的购物车商品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 6. 找到商品ID和口味都匹配的购物车记录
        ShoppingCart target = null;

        for (ShoppingCart item : list) {

            if (Objects.equals(item.getDishId(), shoppingCartDTO.getDishId())
                    && Objects.equals(item.getSetmealId(), shoppingCartDTO.getSetmealId())
                    && Objects.equals(item.getDishFlavor(), shoppingCartDTO.getDishFlavor())) {

                target = item;
                break;
            }
        }

        // 7. 如果购物车中不存在对应商品，直接返回
        if (target == null) {
            return;
        }

        // 8. 获取当前商品数量
        Integer number = target.getNumber();

        if (number != null && number > 1) {

            // 数量大于1，直接减1
            target.setNumber(number - 1);

            // 更新数据库中的商品数量
            shoppingCartMapper.updateNumberById(target);

        } else {

            // 数量为1时，删除购物车中的这条记录
            shoppingCartMapper.deleteByIdAndUserId(
                    target.getId(), userId);
        }
    }
}
