package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long paymentId;

  @OneToOne(
      mappedBy = "payment",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Order order;

  @NotBlank
  @Size(min = 4, message = "Payment method must contain at-least 4 characters")
  private String paymentMethod;

  private String pgName;
  private String pgStatus;
  private String pgPaymentId;
  private String pgResponseMessage;

  public Payment(
      String paymentMethod,
      String pgPaymentId,
      String pgStatus,
      String pgName,
      String pgResponseMessage) {
    this.paymentMethod = paymentMethod;
    this.pgPaymentId = pgPaymentId;
    this.pgStatus = pgStatus;
    this.pgName = pgName;
    this.pgResponseMessage = pgResponseMessage;
  }
}
