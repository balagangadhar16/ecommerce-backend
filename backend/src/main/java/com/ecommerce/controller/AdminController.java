package com.ecommerce.controller;

import com.ecommerce.dto.AdminProductRequest;
import com.ecommerce.dto.AdminUserUpdateRequest;
import com.ecommerce.dto.BusinessAnalyticsResponse;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ----- Products -----

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addProduct(request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        adminService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Users -----

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserDetails(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.getUserDetails(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Integer id,
                                                   @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    // ----- Business analytics -----

    @GetMapping("/analytics/day")
    public ResponseEntity<BusinessAnalyticsResponse> getDayBusiness(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(adminService.getDayBusiness(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/analytics/month")
    public ResponseEntity<BusinessAnalyticsResponse> getMonthBusiness(
            @RequestParam(required = false) YearMonth month) {
        return ResponseEntity.ok(adminService.getMonthBusiness(month != null ? month : YearMonth.now()));
    }

    @GetMapping("/analytics/year")
    public ResponseEntity<BusinessAnalyticsResponse> getYearBusiness(
            @RequestParam(required = false) Year year) {
        return ResponseEntity.ok(adminService.getYearBusiness(year != null ? year : Year.now()));
    }

    @GetMapping("/analytics/overall")
    public ResponseEntity<BusinessAnalyticsResponse> getOverallBusiness() {
        return ResponseEntity.ok(adminService.getOverallBusiness());
    }
}
