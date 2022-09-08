package com.nphase.service;

import com.nphase.entity.Product;
import com.nphase.entity.ShoppingCart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShoppingCartService {

    private final BigDecimal discount;
    private final BigDecimal discountCoefficient;
    private final Integer maxDiscountProductQuantity;

    public ShoppingCartService(BigDecimal discount, Integer maxDiscountProductQuantity) {
        this.discount = discount;
        this.maxDiscountProductQuantity = maxDiscountProductQuantity;
        this.discountCoefficient = BigDecimal.valueOf(100)
                .subtract(discount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    //task 1,2
    public BigDecimal calculateTotalPrice(ShoppingCart shoppingCart) {
        return shoppingCart.getProducts()
                .stream()
                .map(this::calculateBulkPriceWithDiscount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    //task 3
    public BigDecimal calculateTotalPriceByCategory(ShoppingCart shoppingCart) {
        Map<String, List<Product>> productsInCategories = shoppingCart.getProducts().stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.toList()));

        BigDecimal discountByCategory = calculateDiscountByCategory(productsInCategories);

        return shoppingCart.getProducts()
                .stream()
                .map(this::calculateBulkPriceWithDiscount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
                .subtract(discountByCategory);
    }

    private BigDecimal calculateDiscountByCategory(Map<String, List<Product>> productsInCategories) {
        return productsInCategories.values().stream()
                .map(products -> new Category(
                        products.stream()
                                .map(product -> product.getPricePerUnit()
                                        .multiply(BigDecimal.valueOf(product.getQuantity())))
                                .reduce(BigDecimal::add)
                                .orElse(BigDecimal.ZERO),
                        products.stream()
                                .map(Product::getQuantity)
                                .reduce(Integer::sum)
                                .orElse(0)
                ))
                .filter(category -> category.getProductQuantity() >= maxDiscountProductQuantity)
                .map(Category::getProductPrice)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
                .divide(discount, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBulkPriceWithDiscount(Product product) {
        int quantity = product.getQuantity();
        BigDecimal pricePerUnit = product.getPricePerUnit();
        BigDecimal totalPrice = pricePerUnit.multiply(BigDecimal.valueOf(quantity));

        return quantity >= maxDiscountProductQuantity ? totalPrice.multiply(discountCoefficient)
                .setScale(0, RoundingMode.UP) : totalPrice;
    }

    static class Category {
        private final BigDecimal productPrice;
        private final Integer productQuantity;

        public Category(BigDecimal productPrice, Integer productQuantity) {
            this.productPrice = productPrice;
            this.productQuantity = productQuantity;
        }

        public BigDecimal getProductPrice() {
            return productPrice;
        }

        public Integer getProductQuantity() {
            return productQuantity;
        }
    }
}
