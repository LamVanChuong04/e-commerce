package com.project.shopapp.services.orders;

import com.project.shopapp.dtos.CartItemDTO;
import com.project.shopapp.dtos.OrderDTO;
import com.project.shopapp.dtos.OrderDetailDTO;
import com.project.shopapp.dtos.OrderWithDetailsDTO;
import com.project.shopapp.exceptions.DataNotFoundException;
import com.project.shopapp.models.*;
import com.project.shopapp.repositories.*;
import com.project.shopapp.responses.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService{
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderDetailRepository orderDetailRepository;

    private final ModelMapper modelMapper;

    @Override
@Transactional
public Order createOrder(OrderDTO orderDTO) throws Exception {
    User user = userRepository
            .findById(orderDTO.getUserId())
            .orElseThrow(() -> new DataNotFoundException("Cannot find user with id: " + orderDTO.getUserId()));

    modelMapper.typeMap(OrderDTO.class, Order.class)
            .addMappings(mapper -> mapper.skip(Order::setId));

    Order order = new Order();
    modelMapper.map(orderDTO, order);
    order.setUser(user);
    order.setOrderDate(LocalDateTime.now());
    order.setStatus(OrderStatus.PENDING);

    LocalDate shippingDate = orderDTO.getShippingDate() == null
            ? LocalDate.now() : orderDTO.getShippingDate();
    if (shippingDate.isBefore(LocalDate.now())) {
        throw new DataNotFoundException("Date must be at least today!");
    }
    order.setShippingDate(shippingDate);
    order.setActive(true);

    // coupon
    String couponCode = orderDTO.getCouponCode();
    if (couponCode != null && !couponCode.isEmpty()) {
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon is not active");
        }
        order.setCoupon(coupon);
    } else {
        order.setCoupon(null);
    }

    // 🔹 Lưu order trước để có ID
    orderRepository.save(order);

    List<OrderDetail> orderDetails = new ArrayList<>();
    float totalMoney = 0f;

    for (CartItemDTO cartItemDTO : orderDTO.getCartItems()) {
        Long productId = cartItemDTO.getProductId();
        int quantity = cartItemDTO.getQuantity();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException("Product not found with id: " + productId));

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrder(order);
        orderDetail.setProduct(product);
        orderDetail.setNumberOfProducts(quantity);
        orderDetail.setPrice(product.getPrice());

        // 🔹 Tính totalMoney cho từng OrderDetail (float)
        float itemTotal = product.getPrice() * quantity;
        orderDetail.setTotalMoney(itemTotal);

        totalMoney += itemTotal;

        orderDetails.add(orderDetail);
    }

    // 🔹 Update lại totalMoney cho Order
    order.setTotalMoney(totalMoney);
    orderRepository.save(order);

    // 🔹 Lưu danh sách OrderDetail sau khi Order đã có ID
    orderDetailRepository.saveAll(orderDetails);

    return order;
}

    @Transactional
    public Order updateOrderWithDetails(OrderWithDetailsDTO orderWithDetailsDTO) {
        modelMapper.typeMap(OrderWithDetailsDTO.class, Order.class)
                .addMappings(mapper -> mapper.skip(Order::setId));
        Order order = new Order();
        modelMapper.map(orderWithDetailsDTO, order);
        Order savedOrder = orderRepository.save(order);

        // Set the order for each order detail
        for (OrderDetailDTO orderDetailDTO : orderWithDetailsDTO.getOrderDetailDTOS()) {
            //orderDetail.setOrder(OrderDetail);
        }

        // Save or update the order details
        List<OrderDetail> savedOrderDetails = orderDetailRepository.saveAll(order.getOrderDetails());

        // Set the updated order details for the order
        savedOrder.setOrderDetails(savedOrderDetails);

        return savedOrder;
    }
    @Override
    public Order getOrderById(Long orderId) {
        // Tìm theo ID
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            // Nếu không tìm thấy theo ID, tìm theo vnpTxnRef
            order = orderRepository.findByVnpTxnRef(orderId.toString()).orElse(null);
        }
        return order;
    }

    @Override
    @Transactional
    public Order updateOrder(Long id, OrderDTO orderDTO)
            throws DataNotFoundException {
        Order order = getOrderById(id);
        User existingUser = userRepository.findById(
                orderDTO.getUserId()).orElseThrow(() ->
                new DataNotFoundException("Cannot find user with id: " + id));
        /*
        modelMapper.typeMap(OrderDTO.class, Order.class)
                .addMappings(mapper -> mapper.skip(Order::setId));
        modelMapper.map(orderDTO, order);
         */
        // Setting user
        if (orderDTO.getUserId() != null) {
            User user = new User();
            user.setId(orderDTO.getUserId());
            order.setUser(user);
        }

        if (orderDTO.getFullName() != null && !orderDTO.getFullName().trim().isEmpty()) {
            order.setFullName(orderDTO.getFullName().trim());
        }

        if (orderDTO.getEmail() != null && !orderDTO.getEmail().trim().isEmpty()) {
            order.setEmail(orderDTO.getEmail().trim());
        }

        if (orderDTO.getPhoneNumber() != null && !orderDTO.getPhoneNumber().trim().isEmpty()) {
            order.setPhoneNumber(orderDTO.getPhoneNumber().trim());
        }

        if (orderDTO.getStatus() != null && !orderDTO.getStatus().trim().isEmpty()) {
            order.setStatus(orderDTO.getStatus().trim());
        }

        if (orderDTO.getAddress() != null && !orderDTO.getAddress().trim().isEmpty()) {
            order.setAddress(orderDTO.getAddress().trim());
        }

        if (orderDTO.getNote() != null && !orderDTO.getNote().trim().isEmpty()) {
            order.setNote(orderDTO.getNote().trim());
        }

        if (orderDTO.getTotalMoney() != null) {
            order.setTotalMoney(orderDTO.getTotalMoney());
        }

        if (orderDTO.getShippingMethod() != null && !orderDTO.getShippingMethod().trim().isEmpty()) {
            order.setShippingMethod(orderDTO.getShippingMethod().trim());
        }

        if (orderDTO.getShippingAddress() != null && !orderDTO.getShippingAddress().trim().isEmpty()) {
            order.setShippingAddress(orderDTO.getShippingAddress().trim());
        }

        if (orderDTO.getShippingDate() != null) {
            order.setShippingDate(orderDTO.getShippingDate());
        }

        if (orderDTO.getPaymentMethod() != null && !orderDTO.getPaymentMethod().trim().isEmpty()) {
            order.setPaymentMethod(orderDTO.getPaymentMethod().trim());
        }

        order.setUser(existingUser);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = getOrderById(orderId);
        //no hard-delete, => please soft-delete
        if(order != null) {
            order.setActive(false);
            orderRepository.save(order);
        }
    }
    @Override
    public List<OrderResponse> findByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(order -> OrderResponse.fromOrder(order)).toList();
    }

    @Override
    public Page<Order> getOrdersByKeyword(String keyword, Pageable pageable) {
        return orderRepository.findByKeyword(keyword, pageable);
    }
    @Override
    @Transactional
    public Order updateOrderStatus(Long id, String status) throws DataNotFoundException, IllegalArgumentException {
        // Tìm đơn hàng theo ID
        Order order = getOrderById(id); // Sẽ tìm theo ID trước, sau đó tìm theo vnpTxnRef

        // Kiểm tra trạng thái hợp lệ
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        // Kiểm tra xem trạng thái có nằm trong danh sách hợp lệ không
        if (!OrderStatus.VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        // Kiểm tra logic chuyển đổi trạng thái
        String currentStatus = order.getStatus();
        if (currentStatus.equals(OrderStatus.DELIVERED) && !status.equals(OrderStatus.CANCELLED)) {
            throw new IllegalArgumentException("Cannot change status from DELIVERED to " + status);
        }

        if (currentStatus.equals(OrderStatus.CANCELLED)) {
            throw new IllegalArgumentException("Cannot change status of a CANCELLED order");
        }

        if (status.equals(OrderStatus.CANCELLED)) {
            // Kiểm tra xem đơn hàng có thể bị hủy không
            if (!currentStatus.equals(OrderStatus.PENDING)) {
                throw new IllegalArgumentException("Order can only be cancelled from PENDING status");
            }
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(status);

        // Lưu đơn hàng đã cập nhật
        return orderRepository.save(order);
    }
}
