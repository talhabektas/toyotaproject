package com.example.fixsimulator;

import com.example.fixsimulator.config.FixSimulatorConfig;
import com.example.fixsimulator.service.FixServerService;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

/**
 * FIX Protocol Platform Simülatörü Ana Uygulama Sınıfı
 */
@SpringBootApplication
@EnableScheduling
public class FixSimulatorApplication {

	private static final Logger logger = LogManager.getLogger(FixSimulatorApplication.class);

	public static void main(String[] args) {
		// Komut satırı argümanlarını işle
		CommandLineParser parser = new DefaultParser();
		Options options = getCommandLineOptions();

		try {
			CommandLine cmd = parser.parse(options, args);

			// Yapılandırma dosyası yollarını al
			String quickfixConfigFile = cmd.getOptionValue("config", "config/quickfix.cfg");
			String ratesConfigFile = cmd.getOptionValue("rates", "config/rates-config.json");

			// Dosyaların varlığını kontrol et
			checkFileExists(quickfixConfigFile);
			checkFileExists(ratesConfigFile);

			// Sistem özelliklerini ayarla
			System.setProperty("quickfix.config", quickfixConfigFile);
			System.setProperty("rates.config", ratesConfigFile);

			// Uygulamayı başlat
			SpringApplication.run(FixSimulatorApplication.class, args);
			logger.info("FIX Simulator başlatıldı");

		} catch (ParseException e) {
			logger.error("Komut satırı argümanları ayrıştırılamadı", e);
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("fix-simulator", options);
			System.exit(1);
		} catch (Exception e) {
			logger.error("Uygulama başlatılırken hata oluştu", e);
			System.exit(1);
		}
	}

	/**
	 * Komut satırı seçeneklerini tanımlar
	 * @return Komut satırı seçenekleri
	 */
	private static Options getCommandLineOptions() {
		Options options = new Options();

		Option configOption = Option.builder("c")
				.longOpt("config")
				.argName("file")
				.hasArg()
				.desc("QuickFIX yapılandırma dosyasının yolu (varsayılan: config/quickfix.cfg)")
				.build();

		Option ratesOption = Option.builder("r")
				.longOpt("rates")
				.argName("file")
				.hasArg()
				.desc("Kurlar JSON dosyasının yolu (varsayılan: config/rates-config.json)")
				.build();

		options.addOption(configOption);
		options.addOption(ratesOption);

		return options;
	}

	/**
	 * Dosyanın var olup olmadığını kontrol eder
	 * @param filePath Dosya yolu
	 */
	private static void checkFileExists(String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			logger.error("Dosya bulunamadı: {}", filePath);
			throw new RuntimeException("Gerekli dosya bulunamadı: " + filePath);
		}
	}
}