package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    // 是否启用模拟支付，默认关闭
    @Value("${sky.payment.mock-enabled:false}")
    private boolean mockPaymentEnabled;

    // 获取当前 Spring 运行环境
    @Autowired
    private Environment environment;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
//    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
//    }

    /**
     * 模拟订单支付成功
     *
     * 仅允许在 dev 开发环境执行。
     * 不调用微信支付，不发生真实资金交易。
     *
     * @param ordersPaymentDTO 支付请求参数，包含订单号
     * @return 模拟支付结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {

        // 1. 确保模拟支付只在开发环境启用
        if (!mockPaymentEnabled ||
                !environment.acceptsProfiles(Profiles.of("dev"))) {
            throw new OrderBusinessException("模拟支付仅允许在开发环境使用");
        }

        // 2. 获取当前登录用户ID
        Long userId = BaseContext.getCurrentId();

        // 3. 获取前端传入的订单号
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        if (orderNumber == null || orderNumber.isEmpty()) {
            throw new OrderBusinessException("订单号不能为空");
        }

        // 4. 查询当前用户的订单
        // 同时校验订单归属，不能支付其他用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(
                orderNumber, userId
        );

        // 5. 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }

        // 6. 校验是否已经支付
        if (Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            throw new OrderBusinessException("该订单已支付");
        }

        // 7. 只能支付待付款状态的订单
        if (!Objects.equals(ordersDB.getStatus(), Orders.PENDING_PAYMENT)
                || !Objects.equals(ordersDB.getPayStatus(), Orders.UN_PAID)) {
            throw new OrderBusinessException("当前订单状态不允许支付");
        }

        // 8. 构造订单更新对象，只修改需要更新的字段
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED) // 待接单
                .payStatus(Orders.PAID)         // 已支付
                .checkoutTime(LocalDateTime.now())
                .build();

        // 9. 更新数据库
        orderMapper.update(orders);

        log.info("【模拟支付成功】订单号：{}，用户ID：{}",
                orderNumber, userId);

        // 10. 告诉前端本次是模拟支付
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setMockPaid(true);

        return vo;
    }


    /**
     * 支付成功，修改订单状态
     */
    public void paySuccess(String outTradeNo) {

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //////////////////////////////////////////////
        Map map = new HashMap();
        map.put("type", 1);//消息类型，1表示来单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);

        //通过WebSocket实现来单提醒，向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
        ///////////////////////////////////////////////////


    }

    /**
     * 用户端订单分页查询
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        // 封装DTO
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 封装VO List便于返回
        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id   --  历史订单不只有一个

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();        // 每个订单都一个单独的VO
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);    // 这里的id就是order的id

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO 并 返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 再来一单
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 设置分页规则 查询第几页 每页多少条订单记录
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 开始执行查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }
    /**
    * 一个订单VO = 订单基本信息（Orders） + 订单菜品明细（转成字符串OrderDishes）
    */
    /**
     * 组装订单数据
     * */
    /**
     * 将分页查询出来的 Orders 转换为 OrderVO
     * @param page 分页查询结果
     * @return 包含菜品信息的 OrderVO 集合
     */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {

        // 1. 创建一个空集合，用于保存最终的 OrderVO
        List<OrderVO> orderVOList = new ArrayList<>();

        // 2. 从分页对象中获取当前页的订单列表
        List<Orders> ordersList = page.getResult();

        // 3. 判断订单集合是否为空
        // 非空才继续处理，避免无意义的遍历
        if (!CollectionUtils.isEmpty(ordersList)) {

            // 4. 遍历当前页中的每一个订单
            for (Orders orders : ordersList) {

                // 5. 创建一个新的 OrderVO 对象
                OrderVO orderVO = new OrderVO();

                // 6. 复制 Orders 与 OrderVO 中同名且兼容的属性
                BeanUtils.copyProperties(orders, orderVO);

                // 7. 查询当前订单的菜品信息，并拼接成字符串
                String orderDishes = getOrderDishesStr(orders);

                // 8. 将菜品信息设置到 OrderVO 中
                orderVO.setOrderDishes(orderDishes);

                // 9. 将组装好的 OrderVO 添加到结果集合
                orderVOList.add(orderVO);
            }
        }

        // 10. 返回所有组装完成的 OrderVO
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * 组装订单里的菜品
     */
    private String getOrderDishesStr(Orders orders) {

        /*  一个订单对应多条订单明细
            OrderDetail(name="宫保鸡丁", number=2)
            OrderDetail(name="米饭", number=3)
        */
        // 根据订单ID查询订单菜品明细
        List<OrderDetail> orderDetailList =
                orderDetailMapper.getByOrderId(orders.getId());

        // 将每条订单明细转换成字符串
        /*
            stream()：把集合转换成一个 Stream 流，方便进行批量数据处理。
            map() 是映射转换，不是你之前学过的 HashMap
            将流中的每个 OrderDetail 对象转换为一个 String
            x -> { ... } 是 Lambda 表达式，其中 x 代表流中当前处理的一条 OrderDetail 数据。
            collect(Collectors.toList())

            collect()：把转换后的结果收集为一个新的 List 集合。
        */
        List<String> orderDishList = orderDetailList.stream()
                .map(x -> {
                    String orderDish =
                            x.getName() + "*" + x.getNumber() + ";";

                    return orderDish;
                })
                .collect(Collectors.toList());

        // 将所有字符串拼接在一起
        /*
            String.join() 用于将多个字符串拼接在一起。
            它的基本格式：
            String.join(分隔符, 字符串集合);
        */
        return String.join("", orderDishList);
    }



    /**
     * 商家拒单
     *
     * 只有待接单状态的订单可以拒单。
     * 如果订单已经支付，则执行开发环境模拟退款。
     *
     * @param ordersRejectionDTO 包含订单ID和拒单原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {

        // 1. 根据订单ID查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 2. 校验订单是否存在，以及是否处于待接单状态
        if (ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3. 创建订单对象，用于更新数据库
        Orders orders = new Orders();

        // 设置需要修改的字段
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);                 // 已取消
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        // 4. 模拟退款
        // 如果原订单已经支付，则将新对象的 payStatus 设置为 REFUND
        // 如果原订单未支付，则不修改 payStatus
        mockRefundIfPaid(ordersDB, orders);

        // 5. 更新数据库
        orderMapper.update(orders);
    }


    /**
     * 商家取消订单
     *
     * 业务规则：
     * 1. 根据订单ID查询订单
     * 2. 校验订单是否存在
     * 3. 已完成、已取消的订单不允许再次取消
     * 4. 如果订单已支付，则执行模拟退款
     * 5. 修改订单状态为已取消
     * 6. 保存商家输入的取消原因以及取消时间
     *
     * @param ordersCancelDTO 包含订单ID和取消原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrdersCancelDTO ordersCancelDTO) {

        // 1. 获取需要取消的订单ID
        Long orderId = ordersCancelDTO.getId();

        // 2. 根据订单ID查询数据库中的原始订单
        Orders ordersDB = orderMapper.getById(orderId);

        // 3. 判断订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(
                    MessageConstant.ORDER_NOT_FOUND
            );
        }

        // 4. 校验订单状态
        // 已取消、已完成的订单不允许重复取消
        Integer status = ordersDB.getStatus();

        if (status == null
                || Objects.equals(status, Orders.CANCELLED)
                || Objects.equals(status, Orders.COMPLETED)) {

            throw new OrderBusinessException(
                    MessageConstant.ORDER_STATUS_ERROR
            );
        }

        // 5. 创建新的Orders对象
        // 只设置需要更新的字段
        Orders orders = new Orders();

        // 设置订单ID
        orders.setId(ordersDB.getId());

        // 6. 将订单状态修改为已取消
        orders.setStatus(Orders.CANCELLED);

        // 7. 设置取消原因
        // 原因来自商家端提交的OrdersCancelDTO
        orders.setCancelReason(
                ordersCancelDTO.getCancelReason()
        );

        // 8. 设置取消时间
        orders.setCancelTime(LocalDateTime.now());

        // 9. 模拟退款
        // 内部会判断订单是否已支付
        // 已支付：payStatus设置为REFUND
        // 未支付：不修改payStatus
        mockRefundIfPaid(ordersDB, orders);

        // 10. 更新数据库
        orderMapper.update(orders);

        // 11. 打印日志
        log.info("商家取消订单成功，订单ID：{}，订单号：{}",
                orderId, ordersDB.getNumber());
    }

    /**
     * 派送订单
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为 3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为 4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态, 状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    /**
     * 模拟退款
     *
     * 功能：
     * 1. 判断订单是否已经支付
     * 2. 检查当前是否为开发环境
     * 3. 模拟退款成功，修改支付状态
     *
     * 注意：
     * 这里只修改传入的Orders对象，
     * 真正更新数据库需要调用orderMapper.update(orders)
     *
     * @param ordersDB 数据库中查询出来的原始订单
     * @param orders   准备更新的订单对象
     */
    private void mockRefundIfPaid(Orders ordersDB, Orders orders) {

        // 1. 判断订单是否已经支付
        // 如果未支付，就不需要退款，直接结束方法
        if (!Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            return;
        }

        // 2. 确保只有开发环境可以使用模拟退款
        if (!mockPaymentEnabled ||
                !environment.acceptsProfiles(Profiles.of("dev"))) {

            throw new OrderBusinessException("真实退款尚未配置");
        }

        // 3. 模拟退款成功
        // 不调用微信支付接口，只打印日志
        log.info("【模拟退款成功】订单号：{}", ordersDB.getNumber());

        // 4. 设置支付状态为已退款
        // Orders.REFUND = 2
        orders.setPayStatus(Orders.REFUND);
    }


    /**
     * 用户取消订单
     *
     * 业务规则：
     * 1. 订单必须存在
     * 2. 只能取消当前登录用户自己的订单
     * 3. 只有待付款、待接单状态可以直接取消
     * 4. 待付款订单直接取消，无需退款
     * 5. 已支付的待接单订单执行模拟退款
     * 6. 将订单状态修改为已取消，并记录取消原因和时间
     * @param id 需要取消的订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) {

        // 1. 根据订单ID查询数据库中的原始订单
        Orders ordersDB = orderMapper.getById(id);

        // 2. 判断订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(
                    MessageConstant.ORDER_NOT_FOUND
            );
        }

        // 3. 校验订单是否属于当前登录用户
        Long userId = BaseContext.getCurrentId();

        if (!Objects.equals(ordersDB.getUserId(), userId)) {
            throw new OrderBusinessException("无权取消该订单");
        }

        // 4. 校验订单状态
        // 1：待付款
        // 2：待接单
        // 3：已接单
        // 4：派送中
        // 5：已完成
        // 6：已取消

        Integer status = ordersDB.getStatus();

        // 只有状态1或2允许用户直接取消
        if (!Objects.equals(status, Orders.PENDING_PAYMENT)
                && !Objects.equals(status, Orders.TO_BE_CONFIRMED)) {

            throw new OrderBusinessException(
                    MessageConstant.ORDER_STATUS_ERROR
            );
        }

        // 5. 创建新的Orders对象，用于修改订单信息
        // 不直接修改ordersDB，便于区分原始订单和更新数据
        Orders orders = new Orders();

        // 设置需要修改的订单ID
        orders.setId(ordersDB.getId());

        // 6. 判断是否需要退款
        // 待接单状态可能已经支付，需要模拟退款
        if (Objects.equals(status, Orders.TO_BE_CONFIRMED)) {

            // 如果已支付，设置支付状态为已退款
            // 如果未支付，则不修改支付状态
            mockRefundIfPaid(ordersDB, orders);
        }

        // 7. 将订单状态修改为已取消
        orders.setStatus(Orders.CANCELLED);

        // 8. 设置取消原因
        orders.setCancelReason("用户取消");

        // 9. 记录取消时间
        orders.setCancelTime(LocalDateTime.now());

        // 10. 更新数据库
        // 更新status、pay_status（如果发生模拟退款）、
        // cancel_reason、cancel_time等字段
        orderMapper.update(orders);

        // 11. 打印日志
        log.info("用户取消订单成功，订单ID：{}，订单号：{}",
                id, ordersDB.getNumber());
    }
}
