package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.model.EventConditions;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@UtilityClass
public class ConditionUtil {
    private final String[] OPERATORS = {">=", "<=", "==", "!=", ">", "<"};

    public boolean test(@NotNull EventConditions conditions) {
        LocalDateTime now = LocalDateTime.now(TimeParseUtil.zone());

        if (conditions.from() != null && now.isBefore(conditions.from())) return false;
        if (conditions.to() != null && now.isAfter(conditions.to())) return false;

        for (String condition : conditions.placeholders()) {
            if (!test(condition)) return false;
        }

        return true;
    }

    public boolean test(@NotNull String condition) {
        for (String operator : OPERATORS) {
            int index = condition.indexOf(operator);
            if (index < 0) continue;

            String left = PlaceholderUtil.apply(null, condition.substring(0, index).trim());
            String right = PlaceholderUtil.apply(null, condition.substring(index + operator.length()).trim());

            return compare(left, right, operator);
        }

        LoggerUtils.warn("Invalid condition (missing operator): {}", condition);
        return false;
    }

    private boolean compare(@NotNull String left, @NotNull String right, @NotNull String operator) {
        Double leftNumber = number(left);
        Double rightNumber = number(right);

        if (leftNumber != null && rightNumber != null) {
            int result = Double.compare(leftNumber, rightNumber);

            return switch (operator) {
                case ">=" -> result >= 0;
                case "<=" -> result <= 0;
                case "==" -> result == 0;
                case "!=" -> result != 0;
                case ">" -> result > 0;
                default -> result < 0;
            };
        }

        return switch (operator) {
            case "==" -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            default -> {
                LoggerUtils.warn("Cannot compare non numeric values: '{}' {} '{}'", left, operator, right);
                yield false;
            }
        };
    }

    private Double number(@NotNull String raw) {
        try {
            return Double.parseDouble(raw.replaceAll("§.", "").replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}