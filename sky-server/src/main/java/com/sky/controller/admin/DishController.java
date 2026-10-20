package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品");
        dishService.saveWithFlavor(dishDTO);
        // 菜品新增说明数据库更新 那么缓存也需要更新
        // 那么我们采用直接删除的方法而不是更新，避免并发问题导致数据不一致
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页管理
     * */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO)
    {
        log.info("菜品分页查询");
        PageResult  pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }


    /**
     * 菜品批量删除
     * */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    // 这里的@RequestParam 注解必须要添加 因为简单类型的参数可以通过默认规则绑定，但是像list这一类复杂的，就必须要显示指定
    public Result delete(@RequestParam List<Long> ids)
    {
        log.info("菜品批量删除");
        dishService.deleteBatch(ids);
        cleanCache("dish_*");  // 删除一批缓存
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id)
    {
        log.info("根据id查询菜品");
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品实现
     * */
    @PutMapping
    @ApiOperation("修改菜品实现")
    public Result update(@RequestBody DishDTO dishDTO)
    {
        log.info("修改菜品：{}",dishDTO);
        dishService.update(dishDTO);

        // 将所有菜品的缓存数据都清理掉，所有以dish_开头的key
        cleanCache("dish_*");
        return Result.success();
    }
    /**
     * 根据分类id查询套餐
     * */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId)
    {
        log.info("根据分类id查询菜品");
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品起售停售
     * */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id)
    {
        dishService.startOrStop(status, id);
        cleanCache("dish_*");
        return Result.success();
    }
    /**
     * 清理缓存数据
     */
    private void cleanCache(String pattern)
    {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }


}
