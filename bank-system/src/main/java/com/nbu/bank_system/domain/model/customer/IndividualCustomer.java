package com.nbu.bank_system.domain.model.customer;

import com.nbu.bank_system.domain.enums.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Customer subtype за физически лица.
 * Съхранява имена и уникален personal identification number (ЕГН), като наследява общите online banking credentials от Customer.
 */

@Entity
@Table(
        name = "individual_customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_individual_customer_egn", columnNames = "egn")
        }
)
@DiscriminatorValue("INDIVIDUAL")
public class IndividualCustomer extends Customer {

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "EGN must be exactly 10 digits")
    @Column(name = "egn", nullable = false, length = 10)
    private String egn;

    protected IndividualCustomer() {
        super(CustomerType.INDIVIDUAL);
    }

    public IndividualCustomer(String firstName, String lastName, String egn) {
        super(CustomerType.INDIVIDUAL);
        this.firstName = firstName;
        this.lastName = lastName;
        this.egn = egn;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEgn() {
        return egn;
    }
}

