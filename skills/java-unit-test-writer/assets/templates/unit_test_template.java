package {{PACKAGE}};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class {{TEST_CLASS_NAME}} {

    private {{CLASS_NAME}} sut;

    @BeforeEach
    void setUp() {
        // Arrange
        sut = new {{CLASS_NAME}}();
    }

    @AfterEach
    void tearDown() {
        // Optional cleanup
    }

    @Test
    void methodName_whenCondition_thenExpectedResult() {
        // Arrange

        // Act

        // Assert
        assertEquals(1, 1);
    }

    @Test
    void methodName_whenInvalidInput_thenThrowsExpectedException() {
        // Arrange

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("replace with real call");
        });
    }
}

