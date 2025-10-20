package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.io.IOException;

@Service
public class ProductServiceImpl implements ProductService {
  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private ModelMapper modelMapper;

  @Autowired private FileService fileService;

  @Value("${project.image}")
  private String path;

  @Autowired private CartRepository cartRepository;
    @Autowired
    private CartService cartService;

    @Override
  public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

    boolean isProductNotPresent = true;
    List<Product> products = category.getProducts();

    for (Product value : products) {
      if (value.getProductName().equals(productDTO.getProductName())) {
        isProductNotPresent = false;
        break;
      }
    }

    if (isProductNotPresent) {
      Product product = modelMapper.map(productDTO, Product.class);

      product.setImage("default.png");
      product.setCategory(category);
      double specialPrice =
          product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
      product.setSpecialPrice(specialPrice);
      Product savedProduct = productRepository.save(product);
      return modelMapper.map(savedProduct, ProductDTO.class);
    } else {
      throw new APIException("Product already exists!");
    }
  }

  @Override
  public ProductResponse getAllProducts(
      Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Product> productsPage = productRepository.findAll(pageDetails);
    List<Product> products = productsPage.getContent();

    List<ProductDTO> productDTOS =
        products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

    ProductResponse productResponse = new ProductResponse();
    productResponse.setContent(productDTOS);
    productResponse.setPageNumber(productsPage.getNumber());
    productResponse.setPageSize(productsPage.getSize());
    productResponse.setTotalElements(productsPage.getTotalElements());
    productResponse.setTotalPages(productsPage.getTotalPages());
    productResponse.setLastPage(productsPage.isLast());
    return productResponse;
  }

  @Override
  public ProductResponse searchByCategory(
      Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, Long categoryId) {
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Product> productsPage =
        productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);
    List<Product> products = productsPage.getContent();

    List<ProductDTO> productDTOS =
        products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

    ProductResponse productResponse = new ProductResponse();
    productResponse.setContent(productDTOS);
    productResponse.setPageNumber(productsPage.getNumber());
    productResponse.setPageSize(productsPage.getSize());
    productResponse.setTotalElements(productsPage.getTotalElements());
    productResponse.setTotalPages(productsPage.getTotalPages());
    productResponse.setLastPage(productsPage.isLast());
    return productResponse;
  }

  @Override
  public ProductResponse searchProductByKeyword(
      Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Product> productsPage =
        productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);
    List<Product> products = productsPage.getContent();

    if (products.isEmpty()) throw new APIException("No products are found!");

    List<ProductDTO> productDTOS =
        products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

    ProductResponse productResponse = new ProductResponse();
    productResponse.setContent(productDTOS);
    productResponse.setPageNumber(productsPage.getNumber());
    productResponse.setPageSize(productsPage.getSize());
    productResponse.setTotalElements(productsPage.getTotalElements());
    productResponse.setTotalPages(productsPage.getTotalPages());
    productResponse.setLastPage(productsPage.isLast());
    return productResponse;
  }

  @Override
  public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
    Product productFromDB =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

    Product product = modelMapper.map(productDTO, Product.class);

    productFromDB.setPrice(product.getPrice());
    productFromDB.setDiscount(product.getDiscount());
    productFromDB.setQuantity(product.getQuantity());
    productFromDB.setProductName(product.getProductName());
    productFromDB.setDescription(product.getDescription());
    productFromDB.setSpecialPrice(product.getSpecialPrice());

    Product savedProduct = productRepository.save(productFromDB);

    List<Cart> carts = cartRepository.findCartsByProductId(productId);

    List<CartDTO> cartDTOS =
        carts.stream()
            .map(
                cart -> {
                  CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                  List<ProductDTO> products =
                      cart.getCartItems().stream()
                          .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class))
                          .toList();

                  cartDTO.setProducts(products);
                  return cartDTO;
                })
            .toList();

    cartDTOS.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

    return modelMapper.map(savedProduct, ProductDTO.class);
  }

  @Override
  public ProductDTO deleteProduct(Long productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

      List<Cart> carts = cartRepository.findCartsByProductId(productId);

      carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

      productRepository.delete(product);
    return modelMapper.map(product, ProductDTO.class);
  }

  @Override
  public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
    Product productFromDB =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

    String fileName = fileService.uploadImage(path, image);

    productFromDB.setImage(fileName);

    Product updatedProduct = productRepository.save(productFromDB);

    return modelMapper.map(updatedProduct, ProductDTO.class);
  }
}
