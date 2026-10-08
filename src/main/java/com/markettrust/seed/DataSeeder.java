package com.markettrust.seed;

import com.markettrust.category.entity.Category;
import com.markettrust.category.repository.CategoryRepository;
import com.markettrust.product.entity.LocationType;
import com.markettrust.product.entity.Product;
import com.markettrust.product.entity.ProductCondition;
import com.markettrust.product.entity.ProductImage;
import com.markettrust.product.entity.ProductStatus;
import com.markettrust.product.repository.ProductImageRepository;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.seller.entity.KycIdType;
import com.markettrust.seller.entity.KycStatus;
import com.markettrust.seller.entity.SellerKyc;
import com.markettrust.seller.entity.SellerLevel;
import com.markettrust.seller.entity.SellerProfile;
import com.markettrust.seller.repository.SellerKycRepository;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.user.entity.Role;
import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.entity.UserStatus;
import com.markettrust.user.repository.RoleRepository;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.wallet.entity.Wallet;
import com.markettrust.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SellerKycRepository sellerKycRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedCategories();
        seedUsersAndListings();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> categories = List.of(
                createCat("Electronics", "Phones, laptops, tablets, gadgets and accessories", 1),
                createCat("Vehicles", "Cars, bikes, scooters and other vehicles", 2),
                createCat("Furniture", "Home and office furniture, decor", 3),
                createCat("Fashion", "Clothing, footwear, accessories and jewellery", 4),
                createCat("Books", "Books, magazines, comics and educational material", 5),
                createCat("Sports", "Sports equipment, fitness gear and outdoor accessories", 6),
                createCat("Home Appliances", "Refrigerators, washing machines, ACs and more", 7),
                createCat("Musical Instruments", "Guitars, keyboards, drums and studio equipment", 8)
            );
            categoryRepository.saveAll(categories);
            log.info("Seeded default categories");
        }
    }

    private Category createCat(String name, String desc, int order) {
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(desc);
        cat.setIsActive(true);
        cat.setSortOrder(order);
        return cat;
    }

    private void seedUsersAndListings() {
        if (userRepository.existsByEmail("admin@markettrust.com")) {
            return;
        }

        Role buyerRole = roleRepository.findByName(RoleName.BUYER).orElseThrow();
        Role sellerRole = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();

        // 1. Admin User
        User admin = User.builder()
                .name("System Administrator")
                .email("admin@markettrust.com")
                .phone("9999999999")
                .password(passwordEncoder.encode("Admin@123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .roles(Set.of(adminRole, sellerRole, buyerRole))
                .build();
        admin = userRepository.save(admin);
        createWallet(admin.getId(), 1000000L);

        // 2. Seller User
        User seller = User.builder()
                .name("TechHub Electronics")
                .email("seller1@demo.com")
                .phone("9876543210")
                .password(passwordEncoder.encode("Seller@123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .roles(Set.of(sellerRole, buyerRole))
                .build();
        seller = userRepository.save(seller);
        createWallet(seller.getId(), 15000L);

        // Create Seller Profile
        SellerProfile sp = new SellerProfile();
        sp.setUserId(seller.getId());
        sp.setDisplayName("TechHub Official");
        sp.setBio("Verified premium seller for refurbished tech and gadgets in Bengaluru.");
        sp.setCity("Bengaluru");
        sp.setState("Karnataka");
        sp.setLatitude(12.9716);
        sp.setLongitude(77.5946);
        sp.setKycStatus(KycStatus.VERIFIED);
        sp.setIsVerified(true);
        sp.setTrustScore(92.5);
        sp.setSellerLevel(SellerLevel.GOLD);
        sp.setAverageRating(4.9);
        sp.setReviewCount(48);
        sp.setCompletedOrders(124);
        sp.setTotalSales(BigDecimal.valueOf(350000L));
        sp.setResponseRate(98.0);
        sp.setCancellationRate(1.2);
        sellerProfileRepository.save(sp);

        SellerKyc kyc = new SellerKyc();
        kyc.setSellerId(seller.getId());
        kyc.setIdType(KycIdType.AADHAAR);
        kyc.setIdNumber("1234-5678-9012");
        kyc.setCity("Bengaluru");
        kyc.setState("Karnataka");
        kyc.setPincode("560001");
        kyc.setDeclarationAccepted(true);
        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setSubmittedAt(LocalDateTime.now().minusDays(30));
        kyc.setReviewedAt(LocalDateTime.now().minusDays(29));
        sellerKycRepository.save(kyc);

        // 3. Buyer User
        User buyer = User.builder()
                .name("Alex Johnson")
                .email("buyer1@demo.com")
                .phone("9123456789")
                .password(passwordEncoder.encode("Buyer@123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .roles(Set.of(buyerRole))
                .build();
        buyer = userRepository.save(buyer);
        createWallet(buyer.getId(), 25000L);

        // 4. Sample Products
        Category electronics = categoryRepository.findByName("Electronics").orElse(null);
        Category vehicles = categoryRepository.findByName("Vehicles").orElse(null);
        Category furniture = categoryRepository.findByName("Furniture").orElse(null);
        Category fashion = categoryRepository.findByName("Fashion").orElse(null);

        if (electronics != null) {
            createProduct(seller.getId(), electronics.getId(), "MacBook Pro 16-inch M2 Max (32GB RAM, 1TB SSD)",
                    "Mint condition MacBook Pro 16-inch with M2 Max chip. Battery capacity 96%. Includes original MagSafe charger and leather sleeve.",
                    145000L, ProductCondition.LIKE_NEW, "Apple", "Bengaluru", "Karnataka", 12.9716, 77.5946,
                    "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80");

            createProduct(seller.getId(), electronics.getId(), "Sony WH-1000XM5 Wireless Noise Canceling Headphones",
                    "Brand new unopened Sony WH-1000XM5 ANC headphones in Silver. Full international warranty.",
                    22000L, ProductCondition.NEW, "Sony", "Bengaluru", "Karnataka", 12.9352, 77.6245,
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80");

            createProduct(seller.getId(), electronics.getId(), "iPhone 14 Pro 256GB Deep Purple",
                    "Gently used for 6 months. Comes with box, original cable, and Spigen Armor case. Screen protector applied from day one.",
                    72000L, ProductCondition.GOOD, "Apple", "Bengaluru", "Karnataka", 12.9716, 77.5946,
                    "https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=800&q=80");
        }

        if (vehicles != null) {
            createProduct(seller.getId(), vehicles.getId(), "Trek Marlin 7 Mountain Bike (Medium Frame)",
                    "Upgraded Trek Marlin 7 Hardtail MTB with Shimano Deore 1x10 drivetrain and RockShox suspension fork. Ready to ride.",
                    38000L, ProductCondition.GOOD, "Trek", "Bengaluru", "Karnataka", 12.9250, 77.5890,
                    "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80");
        }

        if (furniture != null) {
            createProduct(seller.getId(), furniture.getId(), "Herman Miller Aeron Ergonomic Office Chair (Size B)",
                    "Authentic Herman Miller Aeron chair with fully adjustable posturefit SL support. Fully functional, supreme comfort.",
                    48000L, ProductCondition.LIKE_NEW, "Herman Miller", "Bengaluru", "Karnataka", 12.9716, 77.5946,
                    "https://images.unsplash.com/photo-1580481072645-022f9a6d1270?auto=format&fit=crop&w=800&q=80");
        }

        if (fashion != null) {
            createProduct(seller.getId(), fashion.getId(), "Seiko Prospex Automatic Diver Watch (SRPD25)",
                    "Seiko Prospex 'Monster' Blue Dial Automatic Diver watch with stainless steel bracelet and additional rubber strap.",
                    19500L, ProductCondition.GOOD, "Seiko", "Bengaluru", "Karnataka", 12.9716, 77.5946,
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80");
        }

        log.info("Successfully seeded demo users, seller profile, and products.");
    }

    private void createWallet(Long userId, Long initialBalance) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAvailableCredits(BigDecimal.valueOf(initialBalance));
        wallet.setHeldCredits(BigDecimal.ZERO);
        wallet.setTotalEarned(BigDecimal.valueOf(initialBalance));
        wallet.setTotalSpent(BigDecimal.ZERO);
        wallet.setIsFrozen(false);
        walletRepository.save(wallet);
    }

    private void createProduct(Long sellerId, Long categoryId, String title, String desc,
                               Long price, ProductCondition condition, String brand,
                               String city, String state, double lat, double lng, String imageUrl) {
        Product p = new Product();
        p.setSellerId(sellerId);
        p.setCategoryId(categoryId);
        p.setTitle(title);
        p.setDescription(desc);
        p.setPriceInCredits(price);
        p.setCondition(condition);
        p.setBrand(brand);
        p.setStatus(ProductStatus.ACTIVE);
        p.setQuantity(1);
        p.setCity(city);
        p.setState(state);
        p.setLatitude(lat);
        p.setLongitude(lng);
        p.setLocationType(LocationType.EXACT);
        p.setViews(142);
        p.setUniqueViews(98);
        p.setWishlistCount(12);
        p.setInquiryCount(5);
        p.setIsFeatured(true);
        p.setExpiresAt(LocalDateTime.now().plusDays(60));
        p = productRepository.save(p);

        ProductImage img = new ProductImage();
        img.setProductId(p.getId());
        img.setUrl(imageUrl);
        img.setIsPrimary(true);
        img.setSortOrder(0);
        productImageRepository.save(img);
    }
}
