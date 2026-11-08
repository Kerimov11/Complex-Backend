package az.arena.icearena.controller;

import az.arena.icearena.dto.BookingPaymentRequest;
import az.arena.icearena.dto.PaymentDto;
import az.arena.icearena.entity.PaymentStatus;
import az.arena.icearena.mapper.PaymentMapper;
import az.arena.icearena.service.PaymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    /** Normal istifadəçi üçün ödəniş sessiyası yaradır (PENDING). */
    @PostMapping("/checkout")
    public ResponseEntity<PaymentDto> checkout(@Valid @RequestBody BookingPaymentRequest request,
                                               Principal principal) {
        return ResponseEntity.ok(paymentMapper.toDto(
                paymentService.createBookingPayment(principal.getName(), request)
        ));
    }

    /** Demo gateway: istifadəçi öz PENDING ödənişini SUCCESS/FAILED edə bilər. */
    @PostMapping("/{transactionId}/pay")
    public ResponseEntity<PaymentDto> payOwnPayment(@PathVariable String transactionId,
                                                    Principal principal) {
        return ResponseEntity.ok(paymentMapper.toDto(
                paymentService.processOwnPayment(
                        transactionId,
                        principal.getName(),
                        PaymentStatus.SUCCESS
                )
        ));
    }

    @PostMapping("/{transactionId}/fail")
    public ResponseEntity<PaymentDto> failOwnPayment(@PathVariable String transactionId,
                                                     Principal principal) {
        return ResponseEntity.ok(paymentMapper.toDto(
                paymentService.processOwnPayment(
                        transactionId,
                        principal.getName(),
                        PaymentStatus.FAILED
                )
        ));
    }

    /** Admin bütün ödənişləri statusa görə görə bilər. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDto>> listPayments(
            @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(
                paymentService.getPayments(status).stream()
                        .map(paymentMapper::toDto)
                        .toList()
        );
    }

    /** Admin ödənişi təsdiqləyir və ya uğursuz edir. */
    @PostMapping("/process/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDto> processPayment(@PathVariable String transactionId,
                                                     @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentMapper.toDto(
                paymentService.processPayment(transactionId, status)
        ));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentDto> getPaymentByTransactionId(@PathVariable String transactionId,
                                                                Principal principal) {
        return ResponseEntity.ok(paymentMapper.toDto(
                paymentService.getPaymentForUser(transactionId, principal.getName())
        ));
    }
}
