package com.watchstore.config;

import com.watchstore.model.*;
import com.watchstore.repository.CategoryRepository;
import com.watchstore.repository.ProductRepository;
import com.watchstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Seeds an admin account + a starter catalog on first run, so you have
// something to demo immediately. Only runs if the tables are empty.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCatalog();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@watchstore.com")) return;
        User admin = User.builder()
                .fullName("Store Admin")
                .email("admin@watchstore.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        System.out.println(">> Seeded admin account: admin@watchstore.com / admin123");
    }

    private void seedCatalog() {
        if (productRepository.count() > 0) return;

        Category dive = categoryRepository.save(Category.builder().name("Dive Watch").description("Water-resistant sport watches").build());
        Category dress = categoryRepository.save(Category.builder().name("Dress Watch").description("Formal, minimalist watches").build());
        Category chrono = categoryRepository.save(Category.builder().name("Chronograph").description("Stopwatch-function watches").build());
        Category smart = categoryRepository.save(Category.builder().name("Smartwatch").description("Connected, digital watches").build());

        productRepository.save(Product.builder()
                .name("Sealine Diver 200").brand("Northbourne")
                .description("200m water resistant automatic dive watch with unidirectional bezel.")
                .price(new BigDecimal("18500")).stockQuantity(12)
                .imageUrl("https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=600")
                .category(dive).build());

        productRepository.save(Product.builder()
                .name("Aria Slimline").brand("Marchetti")
                .description("Ultra-thin dress watch, leather strap, sapphire crystal.")
                .price(new BigDecimal("24900")).stockQuantity(8)
                .imageUrl("https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=600")
                .category(dress).build());

        productRepository.save(Product.builder()
                .name("Rally Chrono GT").brand("Corsaro")
                .description("Racing-inspired chronograph with tachymeter bezel.")
                .price(new BigDecimal("21200")).stockQuantity(10)
                .imageUrl("https://images.unsplash.com/photo-1533139502658-0198f920d8e8?w=600")
                .category(chrono).build());

        productRepository.save(Product.builder()
                .name("Pulse Connect 2")	.brand("Veyra")
                .description("AMOLED smartwatch, heart-rate + GPS, 7-day battery.")
                .price(new BigDecimal("15900")).stockQuantity(20)
                .imageUrl("https://images.unsplash.com/photo-1544117519-31a4b719223d?w=600")
                .category(smart).build());

        productRepository.save(Product.builder()
                .name("Fieldmaster Classic").brand("Northbourne")
                .description("Rugged everyday field watch with luminous hands.")
                .price(new BigDecimal("9800")).stockQuantity(15)
                .imageUrl("https://images.unsplash.com/photo-1508057198894-247b23fe5ade?w=600")
                .category(dive).build());

        productRepository.save(Product.builder()
                .name("Heritage Moonphase").brand("Marchetti")
                .description("Classic moonphase complication, stainless case.")
                .price(new BigDecimal("32500")).stockQuantity(5)
                .imageUrl("https://images.unsplash.com/photo-1547996160-81dfa63595aa?w=600")
                .category(dress).build());

        System.out.println(">> Seeded product catalog");
    }
}
