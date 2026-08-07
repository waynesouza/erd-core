package com.erd.core;

import com.erd.core.dto.ItemDTO;
import com.erd.core.dto.LinkDataDTO;
import com.erd.core.dto.LocationDTO;
import com.erd.core.dto.LogoutDTO;
import com.erd.core.dto.NodeDataDTO;
import com.erd.core.dto.RefreshTokenMessageDTO;
import com.erd.core.dto.collaboration.EntityLockDTO;
import com.erd.core.dto.error.ErrorMessageDTO;
import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.request.CreateDiagramRequestDTO;
import com.erd.core.dto.request.DiagramDataRequestDTO;
import com.erd.core.dto.request.ExportDdlRequestDTO;
import com.erd.core.dto.request.ImportDdlRequestDTO;
import com.erd.core.dto.request.ProjectCreateRequestDTO;
import com.erd.core.dto.request.ProjectUpdateRequestDTO;
import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.dto.request.TeamMemberRequestDTO;
import com.erd.core.dto.request.UpdateTeamMemberRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.erd.core.dto.response.DiagramDataResponseDTO;
import com.erd.core.dto.response.ExportDdlResponseDTO;
import com.erd.core.dto.response.ProjectDetailsResponseDTO;
import com.erd.core.dto.response.ProjectResponseDTO;
import com.erd.core.dto.response.UserProjectDetailsResponseDTO;
import com.erd.core.dto.response.UserResponseDTO;
import com.erd.core.model.Project;
import com.erd.core.model.RefreshToken;
import com.erd.core.model.Team;
import com.erd.core.model.User;
import com.erd.core.model.mongo.Diagram;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseCookie;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for the project's data-carrier classes (DTOs and persistence models).
 * <p>
 * These 30 classes are written by hand (the project uses neither Lombok nor records), so they
 * contribute roughly 700 lines of constructors and accessors. Rather than duplicating the same
 * boilerplate assertions across 30 test classes, this test drives each class reflectively:
 * <ol>
 *     <li>every declared constructor is invoked with sample arguments;</li>
 *     <li>every readable property is round-tripped — written through its setter when one exists,
 *         or directly into the backing field when the class is read-only — and read back.</li>
 * </ol>
 * Behaviour that is more than a field access (for example {@code User.getAuthorities()}) has no
 * backing field and is therefore skipped here; it is covered by its own dedicated test class.
 */
class PojoContractTest {

    static Stream<Class<?>> pojoTypes() {
        return Stream.of(
                // com.erd.core.dto
                ItemDTO.class,
                LinkDataDTO.class,
                LocationDTO.class,
                LogoutDTO.class,
                NodeDataDTO.class,
                RefreshTokenMessageDTO.class,
                // com.erd.core.dto.collaboration / error
                EntityLockDTO.class,
                ErrorMessageDTO.class,
                // com.erd.core.dto.request
                AuthenticationRequestDTO.class,
                CreateDiagramRequestDTO.class,
                DiagramDataRequestDTO.class,
                ExportDdlRequestDTO.class,
                ImportDdlRequestDTO.class,
                ProjectCreateRequestDTO.class,
                ProjectUpdateRequestDTO.class,
                SignupRequestDTO.class,
                TeamMemberRequestDTO.class,
                UpdateTeamMemberRequestDTO.class,
                // com.erd.core.dto.response
                AuthenticationResponseDTO.class,
                DiagramDataResponseDTO.class,
                ExportDdlResponseDTO.class,
                ProjectDetailsResponseDTO.class,
                ProjectResponseDTO.class,
                UserProjectDetailsResponseDTO.class,
                UserResponseDTO.class,
                // com.erd.core.model
                Project.class,
                RefreshToken.class,
                Team.class,
                User.class,
                Diagram.class
        );
    }

    @ParameterizedTest
    @MethodSource("pojoTypes")
    void testEveryDeclaredConstructor_isInvokable(Class<?> type) throws Exception {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertNotNull(constructors);

        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);

            Object[] arguments = new Object[constructor.getParameterCount()];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = sampleValue(parameterTypes[i]);
            }

            Object instance = constructor.newInstance(arguments);

            assertNotNull(instance, type.getSimpleName() + " constructor with "
                    + constructor.getParameterCount() + " argument(s) returned null");
        }
    }

    @ParameterizedTest
    @MethodSource("pojoTypes")
    void testEveryProperty_roundTripsThroughItsAccessors(Class<?> type) throws Exception {
        Object instance = newInstance(type);
        int propertiesChecked = 0;

        for (Method getter : readableProperties(type)) {
            String property = propertyName(getter);
            Field field = declaredFieldOrNull(type, property);
            if (field == null) {
                // Derived/behavioural accessor with no backing field - covered elsewhere.
                continue;
            }

            Object expected = sampleValue(getter.getReturnType());
            Method setter = setterOrNull(type, property, getter.getReturnType());

            if (setter != null) {
                setter.invoke(instance, expected);
            } else {
                field.setAccessible(true);
                field.set(instance, expected);
            }

            assertEquals(expected, getter.invoke(instance),
                    type.getSimpleName() + "." + getter.getName() + "() did not return the value written to '"
                            + property + "'");
            propertiesChecked++;
        }

        assertTrue(propertiesChecked > 0,
                type.getSimpleName() + " exposes no readable property - is it listed in the wrong test?");
    }

    // ---------------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------------

    private static List<Method> readableProperties(Class<?> type) {
        List<Method> getters = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() == void.class) {
                continue;
            }
            String name = method.getName();
            boolean isGetter = (name.startsWith("get") && name.length() > 3)
                    || (name.startsWith("is") && name.length() > 2);
            if (isGetter) {
                getters.add(method);
            }
        }
        // Deterministic order keeps failures reproducible.
        getters.sort(Comparator.comparing(Method::getName));
        return getters;
    }

    private static String propertyName(Method getter) {
        String name = getter.getName();
        String withoutPrefix = name.startsWith("get") ? name.substring(3) : name.substring(2);
        return Character.toLowerCase(withoutPrefix.charAt(0)) + withoutPrefix.substring(1);
    }

    private static Field declaredFieldOrNull(Class<?> type, String property) {
        try {
            Field field = type.getDeclaredField(property);
            return Modifier.isStatic(field.getModifiers()) ? null : field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Method setterOrNull(Class<?> type, String property, Class<?> valueType) {
        String setterName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            return type.getMethod(setterName, valueType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object newInstance(Class<?> type) throws Exception {
        try {
            Constructor<?> noArgs = type.getDeclaredConstructor();
            noArgs.setAccessible(true);
            return noArgs.newInstance();
        } catch (NoSuchMethodException e) {
            Constructor<?> widest = null;
            for (Constructor<?> candidate : type.getDeclaredConstructors()) {
                if (widest == null || candidate.getParameterCount() > widest.getParameterCount()) {
                    widest = candidate;
                }
            }
            assertNotNull(widest, type.getSimpleName() + " has no usable constructor");

            Object[] arguments = new Object[widest.getParameterCount()];
            Class<?>[] parameterTypes = widest.getParameterTypes();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = sampleValue(parameterTypes[i]);
            }
            widest.setAccessible(true);
            return widest.newInstance(arguments);
        }
    }

    /**
     * Produces a distinct, non-null sample for any type used by the project's data carriers.
     * Unknown types fall back to their no-argument constructor, and to {@code null} when they
     * do not have one.
     */
    private static Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "sample-" + UUID.randomUUID();
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.TRUE;
        }
        if (type == Integer.class || type == int.class) {
            return 42;
        }
        if (type == Long.class || type == long.class) {
            return 42L;
        }
        if (type == Double.class || type == double.class) {
            return 42.0d;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        }
        if (type == Instant.class) {
            return Instant.parse("2026-01-01T12:00:00Z");
        }
        if (type == ResponseCookie.class) {
            return ResponseCookie.from("sample-cookie", "sample-value").build();
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (type == List.class || type == Iterable.class || type == java.util.Collection.class) {
            return new ArrayList<>();
        }
        if (type.isPrimitive() || type.isArray() || type.isInterface()) {
            return null;
        }
        try {
            Constructor<?> noArgs = type.getDeclaredConstructor();
            noArgs.setAccessible(true);
            return noArgs.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

}
