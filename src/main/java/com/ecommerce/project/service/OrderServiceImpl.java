package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
  @Autowired private CartRepository cartRepository;
  @Autowired private AddressRepository addressRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private CartService cartService;
  @Autowired private ModelMapper modelMapper;

  @Override
  @Transactional
  public OrderDTO placeOrder(
      String emailId,
      Long addressId,
      String paymentMethod,
      String pgName,
      String pgPaymentId,
      String pgStatus,
      String pgResponseMessage) {
    Cart cart = cartRepository.findCartByEmail(emailId);

    if (cart == null) throw new ResourceNotFoundException("Cart", "email", emailId);

    Address address =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

    // Create a new order with the payment info
    Order order = new Order();
    order.setEmail(emailId);
    order.setOrderDate(LocalDate.now());
    order.setTotalAmount(cart.getTotalPrice());
    order.setOrderStatus("Order Accepted !");
    order.setAddress(address);

    Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgName, pgResponseMessage);
    payment.setOrder(order);
    payment = paymentRepository.save(payment);
    order.setPayment(payment);

    Order savedOrder = orderRepository.save(order);

    // Get items from the cart into the order items
    List<CartItem> cartItems = cart.getCartItems();
    if (cartItems.isEmpty()) throw new APIException("Cart is empty!");

    List<OrderItem> orderItems = new ArrayList<>();
    for (CartItem item : cartItems) {
      OrderItem orderItem = new OrderItem();
      orderItem.setProduct(item.getProduct());
      orderItem.setQuantity(item.getQuantity());
      orderItem.setDiscount(item.getDiscount());
      orderItem.setOrderedProductPrice(item.getProductPrice());
      orderItem.setOrder(savedOrder);
      orderItems.add(orderItem);
    }

    orderItems = orderItemRepository.saveAll(orderItems);

    // Update the product stock
    List<CartItem> itemsCopy = new ArrayList<>(cart.getCartItems());

    for (CartItem item : itemsCopy) {
      int quantity = item.getQuantity();
      Product product = item.getProduct();
      product.setQuantity(product.getQuantity() - quantity);
      productRepository.save(product);

      // Remove product from cart safely
      cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
    }

    // Send back the order summary
    OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
    orderItems.forEach(
        item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));
    orderDTO.setAddressId(addressId);
    return orderDTO;
  }
}
