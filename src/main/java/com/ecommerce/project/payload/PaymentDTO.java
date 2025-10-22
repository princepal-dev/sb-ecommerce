package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long paymentId;
    private String pgName;
    private String pgStatus;
    private String pgPaymentId;
    private String paymentMethod;
    private String pgResponseMessage;
}
