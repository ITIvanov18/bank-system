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
 * Customer subtype за юридически лица.
 * Съдържа company identity чрез EIK и данни за официалния representative, като наследява общите online banking credentials от Customer.
 */

@Entity
@Table(
        name = "corporate_customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_corporate_customer_eik", columnNames = "eik")
        }
)
@DiscriminatorValue("CORPORATE")
public class CorporateCustomer extends Customer {

    @NotBlank
    @Size(max = 200)
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{9,13}$", message = "EIK must be 9 to 13 digits")
    @Column(name = "eik", nullable = false, length = 13)
    private String eik;

    @NotBlank
    @Size(max = 100)
    @Column(name = "representative_first_name", nullable = false, length = 100)
    private String representativeFirstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "representative_last_name", nullable = false, length = 100)
    private String representativeLastName;

    protected CorporateCustomer() {
        super(CustomerType.CORPORATE);
    }

    public CorporateCustomer(
            String companyName,
            String eik,
            String representativeFirstName,
            String representativeLastName
    ) {
        super(CustomerType.CORPORATE);
        this.companyName = companyName;
        this.eik = eik;
        this.representativeFirstName = representativeFirstName;
        this.representativeLastName = representativeLastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getEik() {
        return eik;
    }

    public String getRepresentativeFirstName() {
        return representativeFirstName;
    }

    public String getRepresentativeLastName() {
        return representativeLastName;
    }
}

