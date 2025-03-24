package com.example.mainapp.services.impl;

import com.example.mainapp.coordinator.Coordinator;
import com.example.mainapp.services.RateCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class DefaultRateCalculationService implements RateCalculationService {

    private final Coordinator coordinator;

    @Autowired
    public DefaultRateCalculationService(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public boolean subscribeRate(String rateName) {
        // Coordinator'ün döndürdüğü Set<String> değerini boolean'a dönüştürme
        Set<String> platforms = coordinator.subscribeRate(rateName);
        return !platforms.isEmpty(); // En az bir platformda abone olunabilirse başarılı sayılır
    }

    @Override
    public boolean subscribeRate(String platformName, String rateName) {
        return coordinator.subscribeRate(platformName, rateName);
    }

    @Override
    public boolean unsubscribeRate(String rateName) {
        // Coordinator'ün döndürdüğü Set<String> değerini boolean'a dönüştürme
        Set<String> platforms = coordinator.unsubscribeRate(rateName);
        return !platforms.isEmpty(); // En az bir platformda abonelik iptal edilebilirse başarılı sayılır
    }

    @Override
    public boolean unsubscribeRate(String platformName, String rateName) {
        return coordinator.unsubscribeRate(platformName, rateName);
    }

    @Override
    public Set<String> getAllRateNames() {
        return coordinator.getAllRateNames();
    }

    @Override
    public Set<String> getAllPlatformNames() {
        return coordinator.getAllPlatformNames();
    }

    @Override
    public void start() {
        coordinator.start();
    }

    @Override
    public void stop() {
        coordinator.stop();
    }
}