package com.example.mainapp.coordinator;
import com.example.mainapp.model.Rate;
import com.example.mainapp.model.RateFields;
import com.example.mainapp.model.RateStatus;

/**
 * Interface defining callback methods for rate data and platform connections
 */
public interface CoordinatorCallBack {

    /**
     * Called when a connection to a platform is established
     * @param platformName Platform name
     * @param status Connection status
     */
    void onConnect(String platformName, boolean status);

    /**
     * Called when a connection to a platform is closed
     * @param platformName Platform name
     * @param status Disconnection status
     */
    void onDisConnect(String platformName, boolean status);

    /**
     * Called when requested rate data is first available
     * @param platformName Platform name
     * @param rateName Rate name
     * @param rate Rate data
     */
    void onRateAvailable(String platformName, String rateName, Rate rate);

    /**
     * Called when requested rate data is updated
     * @param platformName Platform name
     * @param rateName Rate name
     * @param rateFields Updated rate fields
     */
    void onRateUpdate(String platformName, String rateName, RateFields rateFields);

    /**
     * Called when the status of requested rate data changes
     * @param platformName Platform name
     * @param rateName Rate name
     * @param rateStatus Rate status
     */
    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);
}
