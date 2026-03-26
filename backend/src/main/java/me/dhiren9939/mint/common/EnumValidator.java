package me.dhiren9939.mint.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class EnumValidator implements ConstraintValidator<EnumValue, String> {
    private List<String> acceptedValues;

    @Override
    public void initialize(EnumValue annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants()).map(Enum::name).toList();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        boolean isValid = acceptedValues.contains(value);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(String
                            .format("Invalid Value. Accpeted values are: %s.", acceptedValues.toString()))
                    .addConstraintViolation();
        }

        return isValid;
    }
}
