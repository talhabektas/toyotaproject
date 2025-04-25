
function calculate(dependencies) {
    // PF1 ve PF2'den EURUSD kurlarını al
    const pf1EurUsd = dependencies["PF1_EURUSD"];
    const pf2EurUsd = dependencies["PF2_EURUSD"];

    // PF1 ve PF2'den USDTRY kurlarını al
    const pf1UsdTry = dependencies["PF1_USDTRY"];
    const pf2UsdTry = dependencies["PF2_USDTRY"];

    // Platformlardan alınan kurların ortalamasını hesapla
    const eurUsdBid = (pf1EurUsd.bid + pf2EurUsd.bid) / 2;
    const eurUsdAsk = (pf1EurUsd.ask + pf2EurUsd.ask) / 2;
    const usdTryBid = (pf1UsdTry.bid + pf2UsdTry.bid) / 2;
    const usdTryAsk = (pf1UsdTry.ask + pf2UsdTry.ask) / 2;

    // EURTRY bid ve ask değerlerini hesapla
    const bid = eurUsdBid * usdTryBid;
    const ask = eurUsdAsk * usdTryAsk;

    return { bid, ask };
}