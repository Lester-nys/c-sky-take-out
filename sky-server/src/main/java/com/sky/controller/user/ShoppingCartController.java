package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "购物车相关接口")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车,并为购物车添加商品
     * */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO)
    {
        log.info("添加购物车：{}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查看购物车
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list(){
        return Result.success(shoppingCartService.showShoppingCart());
    }

    /**
     * 清空购物车商品
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车商品")
    public Result<String> clean(){
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /**
     * 减少购物车中的商品数量
     */
    @PostMapping("/sub")
    @ApiOperation("减少购物车商品数量")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {

        // 记录请求参数，方便调试
        log.info("减少购物车商品：{}", shoppingCartDTO);

        // 调用Service处理减少数量的业务
        shoppingCartService.subShoppingCart(shoppingCartDTO);

        return Result.success();
    }
}
