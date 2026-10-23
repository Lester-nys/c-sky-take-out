package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 条件查询
     * */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    @Insert("insert into shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor,number, amount, create_time) "
            + "values (#{name},#{image},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{createTime})")
    void insert(ShoppingCart shoppingCart);


    /**
     * 根据用户id清空对应的购物车
     * */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 删除当前用户购物车中的指定商品
     */
    @Delete("delete from shopping_cart where id = #{id} and user_id = #{userId}")
    void deleteByIdAndUserId(@Param("id") Long id,
                             @Param("userId") Long userId);

    /**
     * 批量插入购物车数据
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
