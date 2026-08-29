package com.assignment.booking.validation;

import com.assignment.booking.dto.request.ReservationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, ReservationRequest> {

    @Override
    public boolean isValid(ReservationRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }
        return request.getStartTime().isBefore(request.getEndTime());
    }
}
