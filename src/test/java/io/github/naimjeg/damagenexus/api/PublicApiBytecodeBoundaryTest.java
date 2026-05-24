package io.github.naimjeg.damagenexus.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicApiBytecodeBoundaryTest {
    private static final List<String> FORBIDDEN = List.of(
            "io.github.naimjeg.damagenexus.core.",
            "io.github.naimjeg.damagenexus.internal.",
            "io.github.naimjeg.damagenexus.registry.",
            "io.github.naimjeg.damagenexus.diagnostics.",
            "io.github.naimjeg.damagenexus.debug.",
            "io.github.naimjeg.damagenexus.test."
    );

    @Test
    void everyPublicApiBytecodeSignatureHidesImplementationPackages() throws Exception {
        Path root = Path.of("build/classes/java/main/io/github/naimjeg/damagenexus/api");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                String relative = root.relativize(file).toString()
                        .replace('\\', '.').replace('/', '.');
                String name = "io.github.naimjeg.damagenexus.api."
                        + relative.substring(0, relative.length() - 6);
                Class<?> type = Class.forName(name, false,
                        PublicApiBytecodeBoundaryTest.class.getClassLoader());
                if (!visible(type.getModifiers())) continue;
                for (Field field : type.getDeclaredFields()) {
                    if (visible(field.getModifiers())) check(
                            type + " field " + field.getName(),
                            field.getGenericType(), violations);
                }
                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    if (!visible(constructor.getModifiers())) continue;
                    for (Type value : constructor.getGenericParameterTypes())
                        check(type + " constructor", value, violations);
                    for (Type value : constructor.getGenericExceptionTypes())
                        check(type + " constructor throws", value, violations);
                }
                for (Method method : type.getDeclaredMethods()) {
                    if (!visible(method.getModifiers())) continue;
                    check(type + "#" + method.getName() + " return",
                            method.getGenericReturnType(), violations);
                    for (Type value : method.getGenericParameterTypes())
                        check(type + "#" + method.getName() + " parameter", value, violations);
                    for (Type value : method.getGenericExceptionTypes())
                        check(type + "#" + method.getName() + " throws", value, violations);
                }
            }
        }
        assertFalse(!violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static void check(String owner, Type type, List<String> violations) {
        String signature = type.getTypeName();
        for (String forbidden : FORBIDDEN) {
            if (signature.contains(forbidden)) {
                violations.add(owner + " exposes " + signature);
            }
        }
    }
}
