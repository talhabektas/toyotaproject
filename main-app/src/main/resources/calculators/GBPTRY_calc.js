/**
 * GBPTRY kuru hesaplama fonksiyonu
 * @param {Object} dependencies - Bağımlı kurları içeren nesne
 * @return {Object} bid ve ask değerlerini içeren hesaplanmış kur
 */
function calculate(dependencies) {
    // PF1 ve PF2'den GBPUSD kurlarını al
    const pf1GbpUsd = dependencies["PF1_GBPUSD"];
    const pf2GbpUsd = dependencies["PF2_GBPUSD"];

    // PF1 ve PF2'den USDTRY kurlarını al
    const pf1UsdTry = dependencies["PF1_USDTRY"];
    const pf2UsdTry = dependencies["PF2_USDTRY"];

    // Platformlardan alınan kurların ortalamasını hesapla
    const gbpUsdBid = (pf1GbpUsd.bid + pf2GbpUsd.bid) / 2;
    const gbpUsdAsk = (pf1GbpUsd.ask + pf2GbpUsd.ask) / 2;
    const usdTryBid = (pf1UsdTry.bid + pf2UsdTry.bid) / 2;
    const usdTryAsk = (pf1UsdTry.ask + pf2UsdTry.ask) / 2;

    // GBPTRY bid ve ask değerlerini hesapla
    const bid = gbpUsdBid * usdTryBid;
    const ask = gbpUsdAsk * usdTryAsk;

    return { bid, ask };
}