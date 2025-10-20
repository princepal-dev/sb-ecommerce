package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {
  @Autowired private AuthUtil authUtil;

  @Autowired private CartRepository cartRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private CartItemRepository cartItemRepository;

  @Autowired private ModelMapper modelMapper;

  @Override
  public CartDTO addProductToCart(Long productId, Integer quantity) {
    Cart cart = createCart();

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

    CartItem cartItem =
        cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

    if (cartItem != null)
      throw new APIException("Product " + product.getProductName() + " already exists in the Cart");

    if (product.getQuantity() == 0)
      throw new APIException(product.getProductName() + " is not available");

    if (product.getQuantity() < quantity)
      throw new APIException(
          "Please, make an order of the "
              + product.getProductName()
              + " less than or equal to the quantity "
              + product.getQuantity()
              + ".");

    CartItem newCartItem = new CartItem();

    newCartItem.setProduct(product);
    newCartItem.setCart(cart);
    newCartItem.setQuantity(quantity);
    newCartItem.setDiscount(product.getDiscount());
    newCartItem.setProductPrice(product.getPrice());
    cartItemRepository.save(newCartItem);

    product.setQuantity(product.getQuantity());
    cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
    cart.getCartItems().add(newCartItem);
    cartRepository.save(cart);

    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

    List<CartItem> cartItems = cart.getCartItems();
    Stream<ProductDTO> productStream =
        cartItems.stream()
            .map(
                item -> {
                  ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                  map.setQuantity(item.getQuantity());
                  return map;
                });

    cartDTO.setProducts(productStream.toList());

    return cartDTO;
  }

  private Cart createCart() {
    Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
    if (userCart != null) {
      return userCart;
    }
    Cart cart = new Cart();
    cart.setTotalPrice(0.0);
    cart.setUser(authUtil.loggedInUser());
    return cartRepository.save(cart);
  }

  @Override
  public List<CartDTO> getAllCarts() {
    List<Cart> carts = cartRepository.findAll();
    if (carts.isEmpty()) throw new APIException("No cart exists");

    return carts.stream()
        .map(
            cart -> {
              CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
              List<ProductDTO> products =
                  cart.getCartItems().stream()
                      .map(item -> modelMapper.map(item, ProductDTO.class))
                      .toList();
              cartDTO.setProducts(products);
              return cartDTO;
            })
        .toList();
  }

  @Override
  public CartDTO getCart(String emailId, Long cartId) {
    Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
    if (cart == null) throw new ResourceNotFoundException("Cart", "cartId", cartId);

    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
    cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));
    List<ProductDTO> products =
        cart.getCartItems().stream()
            .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class))
            .toList();
    cartDTO.setProducts(products);

    return cartDTO;
  }
}
