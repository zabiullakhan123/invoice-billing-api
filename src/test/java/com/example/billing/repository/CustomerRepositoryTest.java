package com.example.billing.repository;

import com.example.billing.dto.CustomerSearchCriteria;
import com.example.billing.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against a real MySQL in Docker, so the queries are tested on the database you deploy. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CustomerRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    CustomerRepository repository;

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                customer("Acme Traders", "billing@acmetraders.in", true),
                customer("Acme Foods", "accounts@acmefoods.in", true),
                customer("Blue Ocean Pvt Ltd", "pay@blueocean.in", true),
                customer("Acme Legacy", "old@acmelegacy.in", false)));
    }

    @Test
    void searchIsCaseInsensitiveFiltersActiveAndPages() {
        var criteria = new CustomerSearchCriteria("ACME", true);

        Page<Customer> page = repository.findAll(
                CustomerSpecifications.matching(criteria), PageRequest.of(0, 1, Sort.by("name")));

        assertThat(page.getTotalElements()).isEqualTo(2);   // Acme Legacy excluded (inactive)
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Customer::getName).containsExactly("Acme Foods");
    }

    @Test
    void searchMatchesEmailToo() {
        Page<Customer> page = repository.findAll(
                CustomerSpecifications.matching(new CustomerSearchCriteria("blueocean", null)),
                PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Customer::getName).containsExactly("Blue Ocean Pvt Ltd");
    }

    @Test
    void wildcardCharactersAreTreatedLiterally() {
        Page<Customer> page = repository.findAll(
                CustomerSpecifications.matching(new CustomerSearchCriteria("%", null)),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void emptyCriteriaReturnsEverything() {
        assertThat(repository.findAll(CustomerSpecifications.matching(null), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(4);
    }

    private static Customer customer(String name, String email, boolean active) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setActive(active);
        return c;
    }
}
