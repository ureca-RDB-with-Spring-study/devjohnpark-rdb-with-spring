package com.smartclearance.product;

public class Product {

    private final Long productId;
    private final String name;
    private final int price;
    private final int stockQuantity;

    public Product(Long productId, String name, int price, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
}
