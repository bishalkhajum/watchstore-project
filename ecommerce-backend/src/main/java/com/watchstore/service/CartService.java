package com.watchstore.service;

import com.watchstore.dto.CartItemRequest;
import com.watchstore.dto.CartItemResponse;
import com.watchstore.exception.ApiException;
import com.watchstore.model.CartItem;
import com.watchstore.model.Product;
import com.watchstore.model.User;
import com.watchstore.repository.CartItemRepository;
import com.watchstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(User user) {
        return cartItemRepository.findByUser(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<CartItemResponse> addOrUpdate(User user, CartItemRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ApiException("This product is no longer available", HttpStatus.BAD_REQUEST);
        }
        if (req.getQuantity() > product.getStockQuantity()) {
            throw new ApiException("Only " + product.getStockQuantity() + " left in stock", HttpStatus.BAD_REQUEST);
        }

        CartItem item = cartItemRepository.findByUserAndProductId(user, product.getId())
                .orElse(CartItem.builder().user(user).product(product).quantity(0).build());
        item.setQuantity(req.getQuantity()); // set (not add) - frontend sends desired total quantity
        cartItemRepository.save(item);

        return getCart(user);
    }

    @Transactional
    public List<CartItemResponse> removeItem(User user, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ApiException("Cart item not found", HttpStatus.NOT_FOUND));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ApiException("Not your cart item", HttpStatus.FORBIDDEN);
        }
        cartItemRepository.delete(item);
        return getCart(user);
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }

    private CartItemResponse toResponse(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(item.getProduct().getStockQuantity())
                .build();
    }
}
