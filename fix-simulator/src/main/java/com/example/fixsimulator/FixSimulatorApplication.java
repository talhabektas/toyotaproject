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
 * Main Application Class for the FIX Protocol Platform Simulator
 */
@SpringBootApplication
@EnableScheduling
public class FixSimulatorApplication {

	private static final Logger logger = LogManager.getLogger(FixSimulatorApplication.class);

	public static void main(String[] args) {
		// Handle command line arguments
		CommandLineParser parser = new DefaultParser();
		Options options = getCommandLineOptions();

		try {
			CommandLine cmd = parser.parse(options, args);

			// Get configuration file paths
			String quickfixConfigFile = cmd.getOptionValue("config", "config/quickfix.cfg");
			String ratesConfigFile = cmd.getOptionValue("rates", "config/rates-config.json");

			// Check if files exist
			checkFileExists(quickfixConfigFile);
			checkFileExists(ratesConfigFile);

			// Set system properties
			System.setProperty("quickfix.config", quickfixConfigFile);
			System.setProperty("rates.config", ratesConfigFile);

			// Start the application
			SpringApplication.run(FixSimulatorApplication.class, args);
			logger.info("FIX Simulator started");

		} catch (ParseException e) {
			logger.error("Could not parse command line arguments", e);
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("fix-simulator", options);
			System.exit(1);
		} catch (Exception e) {
			logger.error("An error occurred while starting the application", e);
			System.exit(1);
		}
	}

	/**
	 * Defines command line options
	 * @return Command line options
	 */
	private static Options getCommandLineOptions() {
		Options options = new Options();

		Option configOption = Option.builder("c")
				.longOpt("config")
				.argName("file")
				.hasArg()
				.desc("Path to the QuickFIX configuration file (default: config/quickfix.cfg)")
				.build();

		Option ratesOption = Option.builder("r")
				.longOpt("rates")
				.argName("file")
				.hasArg()
				.desc("Path to the rates JSON file (default: config/rates-config.json)")
				.build();

		options.addOption(configOption);
		options.addOption(ratesOption);

		return options;
	}

	/**
	 * Checks if the file exists
	 * @param filePath The file path
	 */
	private static void checkFileExists(String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			logger.error("File not found: {}", filePath);
			throw new RuntimeException("Required file not found: " + filePath);
		}
	}
}