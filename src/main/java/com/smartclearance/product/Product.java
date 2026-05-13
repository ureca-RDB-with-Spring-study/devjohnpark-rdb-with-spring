package com.smartclearance.product;

public class Product {

    private final Long productId;
    private final String name;
    private final String category;
    private final String description;
    private final int price;
    private final int stockQuantity;

    public Product(Long productId, String name, String category, String description, int price, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
}
