/*
 * Copyright 2026-present ZhouXY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zhouxy.jdbc.test.util;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.util.AssertTools.*;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.util.AssertTools;

@SuppressWarnings({"null", "DataFlowIssue", "ConstantValue"})
class AssertToolsTest {

    // #region - Argument

    @Test
    void testCheckArgument_true() {
        checkArgument(true);
    }

    @Test
    void testCheckArgument_true_withMessage() {
        final String IGNORE_ME = "IGNORE_ME"; // NOSONAR
        checkArgument(true, IGNORE_ME);
    }

    @Test
    void testCheckArgument_true_withNullMessage() {
        final String IGNORE_ME = null; // NOSONAR
        checkArgument(true, IGNORE_ME);
    }

    @Test
    void testCheckArgument_true_withMessageSupplier() {
        checkArgument(true, () -> "Error message: " + LocalDate.now());
    }

    @Test
    void testCheckArgument_true_withNullMessageSupplier() {
        final Supplier<String> IGNORE_ME = null; // NOSONAR
        checkArgument(true, IGNORE_ME);
    }

    @Test
    void testCheckArgument_true_withMessageFormat() {
        LocalDate today = LocalDate.now();
        checkArgument(true, "String format: %s", today);
    }

    @Test
    void testCheckArgument_true_withNullMessageFormat() {
        checkArgument(true, null, LocalDate.now());
    }

    @Test
    void testCheckArgument_false() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgument(false));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgument_false_withMessage() {
        final String message = "testCheckArgument_false_withMessage";

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgument(false, message));

        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgument_false_withNullMessage() {
        final String message = null;

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgument(false, message));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgument_false_withMessageSupplier() {
        final LocalDate today = LocalDate.now();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgument(false, () -> "Error message: " + today));

        assertEquals("Error message: " + today, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgument_false_withNullMessageSupplier() {
        Supplier<String> messageSupplier = null;
        assertThrows(NullPointerException.class,
                () -> checkArgument(false, messageSupplier));
    }

    @Test
    void testCheckArgument_false_withMessageFormat() {
        LocalDate today = LocalDate.now();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgument(false, "String format: %s", today));
        assertEquals(String.format("String format: %s", today), e.getMessage());
    }

    @Test
    void testCheckArgument_false_withNullMessageFormat() {
        LocalDate today = LocalDate.now();
        assertThrows(NullPointerException.class,
                () -> checkArgument(false, null, today));
    }

    // #endregion - Argument

    // #region - ArgumentNotNull

    @Test
    void testCheckArgumentNotNull_notNull() {
        final Object object = new Object();
        assertEquals(object, checkArgumentNotNull(object));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withMessage() {
        final Object object = new Object();
        final String IGNORE_ME = "IGNORE_ME"; // NOSONAR
        assertEquals(object, checkArgumentNotNull(object, IGNORE_ME));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withNullMessage() {
        final Object object = new Object();
        final String IGNORE_ME = null; // NOSONAR
        assertEquals(object, checkArgumentNotNull(object, IGNORE_ME));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withMessageSupplier() {
        final Object object = new Object();
        assertEquals(object, checkArgumentNotNull(object, () -> "Error message: " + LocalDate.now()));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withNullMessageSupplier() {
        final Object object = new Object();
        final Supplier<String> IGNORE_ME = null; // NOSONAR
        assertEquals(object, checkArgumentNotNull(object, IGNORE_ME));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withMessageFormat() {
        final Object object = new Object();
        LocalDate today = LocalDate.now();
        assertEquals(object, checkArgumentNotNull(object, "String format: %s", today));
    }

    @Test
    void testCheckArgumentNotNull_notNull_withNullMessageFormat() {
        final Object object = new Object();
        assertEquals(object, checkArgumentNotNull(object, null, LocalDate.now()));
    }

    @Test
    void testCheckArgumentNotNull_null() {
        final Object object = null;
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgumentNotNull(object));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgumentNotNull_null_withMessage() {
        final Object object = null;
        final String message = "testCheckArgumentNotNull_null_withMessage";

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgumentNotNull(object, message));

        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgumentNotNull_null_withNullMessage() {
        final Object object = null;
        final String message = null;

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgumentNotNull(object, message));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgumentNotNull_null_withMessageSupplier() {
        final Object object = null;
        final LocalDate today = LocalDate.now();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgumentNotNull(object, () -> "Error message: " + today));

        assertEquals("Error message: " + today, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckArgumentNotNull_null_withNullMessageSupplier() {
        final Object object = null;
        Supplier<String> messageSupplier = null;
        assertThrows(NullPointerException.class,
                () -> checkArgumentNotNull(object, messageSupplier));
    }

    @Test
    void testCheckArgumentNotNull_null_withMessageFormat() {
        final Object object = null;
        LocalDate today = LocalDate.now();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> checkArgumentNotNull(object, "String format: %s", today));
        assertEquals(String.format("String format: %s", today), e.getMessage());
    }

    @Test
    void testCheckArgumentNotNull_null_withNullMessageFormat() {
        final Object object = null;
        LocalDate today = LocalDate.now();
        assertThrows(NullPointerException.class,
                () -> checkArgumentNotNull(object, null, today));
    }

    // #endregion - ArgumentNotNull

    // #region - State

    @Test
    void testCheckState_true() {
        checkState(true);
    }

    @Test
    void testCheckState_true_withMessage() {
        final String IGNORE_ME = "IGNORE_ME"; // NOSONAR
        checkState(true, IGNORE_ME);
    }

    @Test
    void testCheckState_true_withNullMessage() {
        final String IGNORE_ME = null; // NOSONAR
        checkState(true, IGNORE_ME);
    }

    @Test
    void testCheckState_true_withMessageSupplier() {
        checkState(true, () -> "Error message: " + LocalDate.now());
    }

    @Test
    void testCheckState_true_withNullMessageSupplier() {
        final Supplier<String> IGNORE_ME = null; // NOSONAR
        checkState(true, IGNORE_ME);
    }

    @Test
    void testCheckState_true_withMessageFormat() {
        LocalDate today = LocalDate.now();
        checkState(true, "String format: %s", today);
    }

    @Test
    void testCheckState_true_withNullMessageFormat() {
        checkState(true, null, LocalDate.now());
    }

    @Test
    void testCheckState_false() {
        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> checkState(false));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckState_false_withMessage() {
        final String message = "testCheckState_false_withMessage";

        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> checkState(false, message));

        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckState_false_withNullMessage() {
        final String message = null;

        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> checkState(false, message));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckState_false_withMessageSupplier() {
        final LocalDate today = LocalDate.now();
        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> checkState(false, () -> "Error message: " + today));

        assertEquals("Error message: " + today, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckState_false_withNullMessageSupplier() {
        Supplier<String> messageSupplier = null;
        assertThrows(NullPointerException.class,
                () -> checkState(false, messageSupplier));
    }

    @Test
    void testCheckState_false_withMessageFormat() {
        LocalDate today = LocalDate.now();
        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> checkState(false, "String format: %s", today));
        assertEquals(String.format("String format: %s", today), e.getMessage());
    }

    @Test
    void testCheckState_false_withNullMessageFormat() {
        LocalDate today = LocalDate.now();
        assertThrows(NullPointerException.class,
                () -> checkState(false, null, today));
    }

    // #endregion - State

    // #region - NotNull

    @Test
    void testCheckNotNull_notNull() {
        final Object object = new Object();
        checkNotNull(object);
    }

    @Test
    void testCheckNotNull_notNull_withMessage() {
        final Object object = new Object();
        final String IGNORE_ME = "IGNORE_ME"; // NOSONAR
        checkNotNull(object, IGNORE_ME);
    }

    @Test
    void testCheckNotNull_notNull_withNullMessage() {
        final Object object = new Object();
        final String IGNORE_ME = null; // NOSONAR
        checkNotNull(object, IGNORE_ME);
    }

    @Test
    void testCheckNotNull_notNull_withMessageSupplier() {
        final Object object = new Object();
        checkNotNull(object, () -> "Error message: " + LocalDate.now());
    }

    @Test
    void testCheckNotNull_notNull_withNullMessageSupplier() {
        final Object object = new Object();
        final Supplier<String> IGNORE_ME = null; // NOSONAR
        checkNotNull(object, IGNORE_ME);
    }

    @Test
    void testCheckNotNull_notNull_withMessageFormat() {
        final Object object = new Object();
        LocalDate today = LocalDate.now();
        checkNotNull(object, "String format: %s", today);
    }

    @Test
    void testCheckNotNull_notNull_withNullMessageFormat() {
        final Object object = new Object();
        checkNotNull(object, null, LocalDate.now());
    }

    @Test
    void testCheckNotNull_null() {
        final Object object = null;
        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> checkNotNull(object));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckNotNull_null_withMessage() {
        final Object object = null;
        final String message = "testCheckNotNull_null_withMessage";

        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> checkNotNull(object, message));

        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckNotNull_null_withNullMessage() {
        final Object object = null;
        final String message = null;

        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> checkNotNull(object, message));

        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckNotNull_null_withMessageSupplier() {
        final Object object = null;
        final LocalDate today = LocalDate.now();
        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> checkNotNull(object, () -> "Error message: " + today));

        assertEquals("Error message: " + today, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCheckNotNull_null_withNullMessageSupplier() {
        final Object object = null;
        Supplier<String> messageSupplier = null;
        assertThrows(NullPointerException.class,
                () -> checkNotNull(object, messageSupplier));
    }

    @Test
    void testCheckNotNull_null_withMessageFormat() {
        final Object object = null;
        LocalDate today = LocalDate.now();
        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> checkNotNull(object, "String format: %s", today));
        assertEquals(String.format("String format: %s", today), e.getMessage());
    }

    @Test
    void testCheckNotNull_null_withNullMessageFormat() {
        final Object object = null;
        LocalDate today = LocalDate.now();
        assertThrows(NullPointerException.class,
                () -> checkNotNull(object, null, today));
    }

    // #endregion - NotNull

    // #region - Condition

    static final class MyException extends RuntimeException {}

    @Test
    void testCheckCondition() {

        checkCondition(true, MyException::new);

        final MyException me = new MyException();
        MyException e = assertThrows(MyException.class, () -> checkCondition(false, () -> me));
        assertEquals(me, e);
    }

    // #endregion - Condition

    // ================================
    // #region - invoke constructor
    // ================================

    @Test
    void test_constructor_isNotAccessible_ThrowsIllegalStateException() {
        Constructor<?>[] constructors = AssertTools.class.getDeclaredConstructors();
        Arrays.stream(constructors)
                .forEach(constructor -> {
                    assertFalse(constructor.isAccessible());
                    constructor.setAccessible(true);
                    Throwable cause = assertThrows(Exception.class, constructor::newInstance)
                            .getCause();
                    assertInstanceOf(IllegalStateException.class, cause);
                    assertEquals("Utility class", cause.getMessage());
                });
    }

    // ================================
    // #endregion - invoke constructor
    // ================================
}
