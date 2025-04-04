package com.example.mainapp.calculator.impl;

import com.example.mainapp.calculator.RateCalculator;
import com.example.mainapp.model.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java dilinde kur hesaplama uygulaması
 */
public class JavaRateCalculator implements RateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(JavaRateCalculator.class);

    // Derlenmiş sınıfları önbellekle
    private final Map<String, Class<?>> compiledClasses = new ConcurrentHashMap<>();

    // Kur formüllerini sakla
    private final Map<String, String> formulaMap = new ConcurrentHashMap<>();

    @Override
    public Rate calculate(String targetRateName, Map<String, Rate> dependencyRates) {
        // Formülü kontrol et
        String formula = formulaMap.get(targetRateName);
        if (formula == null) {
            logger.error("No formula registered for rate: {}", targetRateName);
            return null;
        }

        // Bağlam haritasını oluştur
        Map<String, Object> context = new HashMap<>();
        context.put("targetRateName", targetRateName);
        context.put("formula", formula);
        context.put("dependencies", dependencyRates);

        // Formülü çalıştır
        Map<String, Object> result = executeFormula(context);
        if (result == null) {
            return null;
        }

        // Rate nesnesini oluştur
        Double bid = (Double) result.get("bid");
        Double ask = (Double) result.get("ask");

        if (bid == null || ask == null) {
            logger.error("Formula execution did not return valid bid/ask values");
            return null;
        }

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName(targetRateName);
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        return calculatedRate;
    }

    @Override
    public boolean registerFormula(String rateName, String formula, String formulaType) {
        if (!"java".equals(formulaType)) {
            logger.error("Unsupported formula type: {}", formulaType);
            return false;
        }

        // Formülü kaydet
        formulaMap.put(rateName, formula);

        // Önceden derlenmiş sınıfı varsa kaldır
        compiledClasses.remove(rateName);

        return true;
    }

    @Override
    public boolean unregisterFormula(String rateName) {
        // Formülü kaldır
        boolean removed = formulaMap.remove(rateName) != null;

        // Derlenmiş sınıfı da kaldır
        compiledClasses.remove(rateName);

        return removed;
    }

    /**
     * Java formülünü çalıştırır
     * @param context Hesaplama bağlamı
     * @return Bid ve ask içeren sonuç haritası
     */
    public Map<String, Object> executeFormula(Map<String, Object> context) {
        try {
            String targetRateName = (String) context.get("targetRateName");
            String formula = (String) context.get("formula");

            @SuppressWarnings("unchecked")
            Map<String, Rate> dependencies = (Map<String, Rate>) context.get("dependencies");

            // Sınıf zaten derlenmiş mi kontrol et
            Class<?> calculatorClass = compiledClasses.get(targetRateName);

            if (calculatorClass == null) {
                // Sınıfı derle
                calculatorClass = compileCalculator(targetRateName, formula);
                if (calculatorClass == null) {
                    logger.error("Failed to compile calculator for {}", targetRateName);
                    return null;
                }

                // Derlenmiş sınıfı önbelleğe al
                compiledClasses.put(targetRateName, calculatorClass);
            }

            // Hesaplayıcı örneği oluştur
            Object calculator = calculatorClass.getDeclaredConstructor().newInstance();

            // calculate metodunu çağır
            Method calculateMethod = calculatorClass.getMethod("calculate", Map.class);
            Object result = calculateMethod.invoke(calculator, dependencies);

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            } else {
                logger.error("Java calculation did not return expected format");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error executing Java formula", e);
            return null;
        }
    }

    /**
     * Java hesaplayıcı sınıfını derler
     * @param targetRateName Hedef kur adı
     * @param formula Formül kodu
     * @return Derlenmiş sınıf veya derleme başarısız olursa null
     */
    private Class<?> compileCalculator(String targetRateName, String formula) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            logger.error("Java compiler not available");
            return null;
        }

        // Sınıf adı oluştur
        String className = "Calculator_" + targetRateName.replace('-', '_').replace('.', '_');

        // Derleme için geçici dizin oluştur
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("javacalc_" + UUID.randomUUID().toString());
        } catch (IOException e) {
            logger.error("Failed to create temp directory for compilation", e);
            return null;
        }

        // Tam sınıf kaynağı oluştur
        String packageName = "com.example.mainapp.calculator.generated";
        String fullClassName = packageName + "." + className;

        String sourceCode = "package " + packageName + ";\n\n" +
                "import com.example.mainapp.model.Rate;\n" +
                "import java.util.Map;\n" +
                "import java.util.HashMap;\n\n" +
                "public class " + className + " {\n" +
                "    public Map<String, Object> calculate(Map<String, Rate> dependencies) {\n" +
                formula + "\n" +
                "    }\n" +
                "}\n";

        // Kaynak kodunu dosyaya yaz
        File sourceFile;
        try {
            Path packagePath = tempDir.resolve(Paths.get("com", "example", "mainapp", "calculator", "generated"));
            Files.createDirectories(packagePath);
            sourceFile = packagePath.resolve(className + ".java").toFile();
            Files.write(sourceFile.toPath(), sourceCode.getBytes());
        } catch (IOException e) {
            logger.error("Failed to write source file", e);
            return null;
        }

        // Sınıfı derle
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));

        String[] compileOptions = new String[]{"-d", tempDir.toAbsolutePath().toString()};
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, Arrays.asList(compileOptions), null, compilationUnits);

        boolean success = task.call();

        if (!success) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                logger.error("Compilation error: {}", diagnostic.getMessage(null));
            }
            return null;
        }

        try {
            fileManager.close();
        } catch (IOException e) {
            logger.warn("Failed to close file manager", e);
        }

        // Derlenmiş sınıfı yükle
        try {
            // Geçici dizin için sınıf yükleyici oluştur
            URLClassLoader classLoader = new URLClassLoader(
                    new java.net.URL[]{tempDir.toUri().toURL()},
                    this.getClass().getClassLoader()
            );

            // Sınıfı yükle
            Class<?> loadedClass = classLoader.loadClass(fullClassName);
            return loadedClass;

        } catch (Exception e) {
            logger.error("Failed to load compiled class", e);
            return null;
        }
    }

    /**
     * Derlenmiş sınıfları yüklemek için özel sınıf yükleyici
     */
    private static class URLClassLoader extends java.net.URLClassLoader {
        public URLClassLoader(java.net.URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
}