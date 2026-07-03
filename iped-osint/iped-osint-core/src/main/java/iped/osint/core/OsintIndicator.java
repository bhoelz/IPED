package iped.osint.core;

import iped.osint.spi.OsintIndicatorType;

public record OsintIndicator(OsintIndicatorType type, String value) {
}
