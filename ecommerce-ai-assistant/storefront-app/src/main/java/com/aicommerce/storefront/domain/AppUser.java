package com.aicommerce.storefront.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTier loyaltyTier;

    private String city;
    private String pincode;

    protected AppUser() {}

    public AppUser(String username, String displayName, LoyaltyTier loyaltyTier, String city, String pincode) {
        this.username = username;
        this.displayName = displayName;
        this.loyaltyTier = loyaltyTier;
        this.city = city;
        this.pincode = pincode;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public LoyaltyTier getLoyaltyTier() { return loyaltyTier; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public boolean isVip() { return loyaltyTier == LoyaltyTier.VIP; }
}
