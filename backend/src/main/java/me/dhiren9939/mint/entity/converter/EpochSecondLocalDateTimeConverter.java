package me.dhiren9939.mint.entity.converter;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class EpochSecondLocalDateTimeConverter implements AttributeConverter<LocalDateTime> {

    @Override
    public AttributeValue transformFrom(LocalDateTime input) {
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(input);
        return AttributeValue.fromN(String.valueOf(input.toEpochSecond(offset)));
    }

    @Override
    public LocalDateTime transformTo(AttributeValue input) {
        Instant instant = Instant.ofEpochSecond(Long.parseLong(input.n()));
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Override
    public EnhancedType<LocalDateTime> type() {
        return EnhancedType.of(LocalDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }
}
